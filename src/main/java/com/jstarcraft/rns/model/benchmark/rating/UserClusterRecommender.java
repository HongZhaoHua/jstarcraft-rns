package com.jstarcraft.rns.model.benchmark.rating;

import java.util.Map.Entry;

import org.apache.commons.math3.util.FastMath;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.ProbabilisticGraphicalRecommender;

/**
 * 
 * User Cluster推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "userDimension", "itemDimension", "userTopicProbabilities", "numberOfFactors", "scoreIndexes", "topicScoreMatrix" })
public class UserClusterRecommender extends ProbabilisticGraphicalRecommender {

    /** 用户的每评分次数 */
    private DenseMatrix userScoreMatrix; // Nur
    /** 用户的总评分次数 */
    private DenseVector userScoreVector; // Nu

    /** 主题的每评分概率 */
    private DenseMatrix topicScoreMatrix; // Pkr
    /** 主题的总评分概率 */
    private DenseVector topicScoreVector; // Pi

    /** 用户主题概率映射 */
    private DenseMatrix userTopicProbabilities; // Gamma_(u,k)

    @Override
    protected boolean isConverged(int iter) {
        // TODO 需要重构
        float loss = 0F;

        for (int u = 0; u < userSize; u++) {
            for (int k = 0; k < factorSize; k++) {
                float ruk = userTopicProbabilities.getValue(u, k);
                float pi_k = topicScoreVector.getValue(k);

                float sum_nl = 0F;
                for (int r = 0; r < scoreIndexes.size(); r++) {
                    float nur = userScoreMatrix.getValue(u, r);
                    float pkr = topicScoreMatrix.getValue(k, r);

                    sum_nl += nur * Math.log(pkr);
                }

                loss += ruk * (Math.log(pi_k) + sum_nl);
            }
        }

        float deltaLoss = (float) (loss - currentError);

        if (iter > 1 && (deltaLoss > 0 || Float.isNaN(deltaLoss))) {
            return true;
        }

        currentError = loss;
        return false;
    }

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        topicScoreMatrix = DenseMatrix.valueOf(factorSize, scoreSize);
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            DenseVector probabilityVector = topicScoreMatrix.getRowVector(topicIndex);
            probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(RandomUtility.randomInteger(scoreSize) + 1);
            });
            probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
        }
        topicScoreVector = DenseVector.valueOf(factorSize);
        topicScoreVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomInteger(factorSize) + 1);
        });
        topicScoreVector.scaleValues(1F / topicScoreVector.getSum(false));
        // TODO
        topicScoreMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue((float) Math.log(scalar.getValue()));
        });
        topicScoreVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue((float) Math.log(scalar.getValue()));
        });

        userScoreMatrix = DenseMatrix.valueOf(userSize, scoreSize);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseVector scoreVector = scoreMatrix.getRowVector(userIndex);
            for (VectorScalar term : scoreVector) {
                float score = term.getValue();
                int scoreIndex = scoreIndexes.get(score);
                userScoreMatrix.shiftValue(userIndex, scoreIndex, 1);
            }
        }
        userScoreVector = DenseVector.valueOf(userSize);
        userScoreVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(scoreMatrix.getRowVector(scalar.getIndex()).getElementSize());
        });
        currentError = Float.MIN_VALUE;

        userTopicProbabilities = DenseMatrix.valueOf(userSize, factorSize);
    }

    @Override
    protected void eStep() {
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            DenseVector probabilityVector = userTopicProbabilities.getRowVector(userIndex);
            SparseVector scoreVector = scoreMatrix.getRowVector(userIndex);
            if (scoreVector.getElementSize() == 0) {
                probabilityVector.copyVector(topicScoreVector);
            } else {
                probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float topicProbability = topicScoreVector.getValue(index);
                    for (VectorScalar term : scoreVector) {
                        int scoreIndex = scoreIndexes.get(term.getValue());
                        float scoreProbability = topicScoreMatrix.getValue(index, scoreIndex);
                        topicProbability = topicProbability + scoreProbability;
                    }
                    scalar.setValue(topicProbability);
                });
                probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
            }
        }
    }

    @Override
    protected void mStep() {
        topicScoreVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
                float numerator = 0F, denorminator = 0F;
                for (int userIndex = 0; userIndex < userSize; userIndex++) {
                    float probability = (float) FastMath.exp(userTopicProbabilities.getValue(userIndex, index));
                    numerator += probability * userScoreMatrix.getValue(userIndex, scoreIndex);
                    denorminator += probability * userScoreVector.getValue(userIndex);
                }
                float probability = (numerator / denorminator);
                topicScoreMatrix.setValue(index, scoreIndex, probability);
            }
            float sumProbability = 0F;
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                float probability = (float) FastMath.exp(userTopicProbabilities.getValue(userIndex, index));
                sumProbability += probability;
            }
            scalar.setValue(sumProbability);
        });
        topicScoreVector.scaleValues(1F / topicScoreVector.getSum(false));
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = 0F;
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            float topicProbability = userTopicProbabilities.getValue(userIndex, topicIndex);
            float topicValue = 0F;
            for (Entry<Float, Integer> entry : scoreIndexes.entrySet()) {
                float score = entry.getKey();
                float probability = topicScoreMatrix.getValue(topicIndex, entry.getValue());
                topicValue += score * probability;
            }
            value += topicProbability * topicValue;
        }
        instance.setQuantityMark(value);
    }

}
