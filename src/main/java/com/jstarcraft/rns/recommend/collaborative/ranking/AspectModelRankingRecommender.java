package com.jstarcraft.rns.recommend.collaborative.ranking;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.ProbabilisticGraphicalRecommender;

/**
 * 
 * Aspect Model推荐器
 * 
 * <pre>
 * Latent class models for collaborative filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class AspectModelRankingRecommender extends ProbabilisticGraphicalRecommender {

    /**
     * Conditional distribution: P(u|z)
     */
    private DenseMatrix userProbabilities, userSums;

    /**
     * Conditional distribution: P(i|z)
     */
    private DenseMatrix itemProbabilities, itemSums;

    /**
     * topic distribution: P(z)
     */
    private DenseVector topicProbabilities, topicSums;

    private DenseVector probabilities;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        // Initialize topic distribution
        // TODO 考虑重构
        topicProbabilities = DenseVector.valueOf(numberOfFactors);
        topicSums = DenseVector.valueOf(numberOfFactors);
        topicProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomInteger(numberOfFactors) + 1);
        });
        topicProbabilities.scaleValues(1F / topicProbabilities.getSum(false));

        userProbabilities = DenseMatrix.valueOf(numberOfFactors, userSize);
        userSums = DenseMatrix.valueOf(numberOfFactors, userSize);
        for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
            DenseVector probabilityVector = userProbabilities.getRowVector(topicIndex);
            probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                float value = scalar.getValue();
                scalar.setValue(RandomUtility.randomInteger(userSize) + 1);
            });
            probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
        }

        itemProbabilities = DenseMatrix.valueOf(numberOfFactors, itemSize);
        itemSums = DenseMatrix.valueOf(numberOfFactors, itemSize);
        for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
            DenseVector probabilityVector = itemProbabilities.getRowVector(topicIndex);
            probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(RandomUtility.randomInteger(itemSize) + 1);
            });
            probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
        }

        probabilities = DenseVector.valueOf(numberOfFactors);
    }

    /*
     *
     */
    @Override
    protected void eStep() {
        topicSums.setValues(0F);
        userSums.setValues(0F);
        itemSums.setValues(0F);
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            probabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = userProbabilities.getValue(index, userIndex) * itemProbabilities.getValue(index, itemIndex) * topicProbabilities.getValue(index);
                scalar.setValue(value);
            });
            probabilities.scaleValues(1F / probabilities.getSum(false));
            for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
                float value = probabilities.getValue(topicIndex) * term.getValue();
                topicSums.shiftValue(topicIndex, value);
                userSums.shiftValue(topicIndex, userIndex, value);
                itemSums.shiftValue(topicIndex, itemIndex, value);
            }
        }
    }

    @Override
    protected void mStep() {
        float scale = 1F / topicSums.getSum(false);
        for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
            topicProbabilities.setValue(topicIndex, topicSums.getValue(topicIndex) * scale);
            float userSum = userProbabilities.getColumnVector(topicIndex).getSum(false);
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                userProbabilities.setValue(topicIndex, userIndex, userSums.getValue(topicIndex, userIndex) / userSum);
            }
            float itemSum = itemProbabilities.getColumnVector(topicIndex).getSum(false);
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                itemProbabilities.setValue(topicIndex, itemIndex, itemSums.getValue(topicIndex, itemIndex) / itemSum);
            }
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = 0F;
        for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
            value += userProbabilities.getValue(topicIndex, userIndex) * itemProbabilities.getValue(topicIndex, itemIndex) * topicProbabilities.getValue(topicIndex);
        }
        instance.setQuantityMark(value);
    }

}
