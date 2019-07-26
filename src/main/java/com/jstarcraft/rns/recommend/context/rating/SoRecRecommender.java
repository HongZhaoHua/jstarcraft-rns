package com.jstarcraft.rns.recommend.context.rating;

import java.util.ArrayList;
import java.util.List;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.SocialRecommender;
import com.jstarcraft.rns.utility.LogisticUtility;

/**
 * 
 * SoRec推荐器
 * 
 * <pre>
 * SoRec: Social recommendation using probabilistic matrix factorization
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class SoRecRecommender extends SocialRecommender {
    /**
     * adaptive learn rate
     */
    private DenseMatrix socialFactors;

    private float regRate, regSocial;

    private List<Integer> inDegrees, outDegrees;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userFactors = DenseMatrix.valueOf(userSize, numberOfFactors);
        userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemFactors = DenseMatrix.valueOf(itemSize, numberOfFactors);
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        socialFactors = DenseMatrix.valueOf(userSize, numberOfFactors);
        socialFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });

        regRate = configuration.getFloat("recommender.rate.social.regularization", 0.01F);
        regSocial = configuration.getFloat("recommender.user.social.regularization", 0.01F);

        inDegrees = new ArrayList<>(userSize);
        outDegrees = new ArrayList<>(userSize);

        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            int in = socialMatrix.getColumnScope(userIndex);
            int out = socialMatrix.getRowScope(userIndex);
            inDegrees.add(in);
            outDegrees.add(out);
        }
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
            totalLoss = 0F;
            DenseMatrix userDeltas = DenseMatrix.valueOf(userSize, numberOfFactors);
            DenseMatrix itemDeltas = DenseMatrix.valueOf(itemSize, numberOfFactors);
            DenseMatrix socialDeltas = DenseMatrix.valueOf(userSize, numberOfFactors);

            // ratings
            for (MatrixScalar term : scoreMatrix) {
                int userIdx = term.getRow();
                int itemIdx = term.getColumn();
                float score = term.getValue();
                float predict = super.predict(userIdx, itemIdx);
                float error = LogisticUtility.getValue(predict) - (score - minimumOfScore) / (maximumOfScore - minimumOfScore);
                totalLoss += error * error;
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float userFactor = userFactors.getValue(userIdx, factorIndex);
                    float itemFactor = itemFactors.getValue(itemIdx, factorIndex);
                    userDeltas.shiftValue(userIdx, factorIndex, LogisticUtility.getGradient(predict) * error * itemFactor + userRegularization * userFactor);
                    itemDeltas.shiftValue(itemIdx, factorIndex, LogisticUtility.getGradient(predict) * error * userFactor + itemRegularization * itemFactor);
                    totalLoss += userRegularization * userFactor * userFactor + itemRegularization * itemFactor * itemFactor;
                }
            }

            // friends
            // TODO 此处是对称矩阵,是否有方法减少计算?
            for (MatrixScalar term : socialMatrix) {
                int userIndex = term.getRow();
                int socialIndex = term.getColumn();
                float socialRate = term.getValue();
                // tuv ~ cik in the original paper
                if (socialRate == 0F) {
                    continue;
                }
                float socialPredict = scalar.dotProduct(userFactors.getRowVector(userIndex), socialFactors.getRowVector(socialIndex)).getValue();
                float socialInDegree = inDegrees.get(socialIndex); // ~ d-(k)
                float userOutDegree = outDegrees.get(userIndex); // ~ d+(i)
                float weight = (float) Math.sqrt(socialInDegree / (userOutDegree + socialInDegree));
                float socialError = LogisticUtility.getValue(socialPredict) - weight * socialRate;
                totalLoss += regRate * socialError * socialError;

                socialPredict = LogisticUtility.getGradient(socialPredict);
                for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float socialFactor = socialFactors.getValue(socialIndex, factorIndex);
                    userDeltas.shiftValue(userIndex, factorIndex, regRate * socialPredict * socialError * socialFactor);
                    socialDeltas.shiftValue(socialIndex, factorIndex, regRate * socialPredict * socialError * userFactor + regSocial * socialFactor);
                    totalLoss += regSocial * socialFactor * socialFactor;
                }
            }

            userFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + userDeltas.getValue(row, column) * -learnRate);
            });
            itemFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + itemDeltas.getValue(row, column) * -learnRate);
            });
            socialFactors.iterateElement(MathCalculator.PARALLEL, (element) -> {
                int row = element.getRow();
                int column = element.getColumn();
                float value = element.getValue();
                element.setValue(value + socialDeltas.getValue(row, column) * -learnRate);
            });

            totalLoss *= 0.5F;
            if (isConverged(iterationStep) && isConverged) {
                break;
            }
            isLearned(iterationStep);
            currentLoss = totalLoss;
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float predict = super.predict(userIndex, itemIndex);
        predict = denormalize(LogisticUtility.getValue(predict));
        instance.setQuantityMark(predict);
    }

}
