package com.jstarcraft.rns.recommend.benchmark.rating;

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
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.ProbabilisticGraphicalRecommender;

/**
 * 
 * Item Cluster推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "userDimension", "itemDimension", "itemTopicProbabilities", "numberOfFactors", "scoreIndexes", "topicScoreMatrix" })
public class ItemClusterRecommender extends ProbabilisticGraphicalRecommender {

    /** 物品的每评分次数 */
    private DenseMatrix itemScoreMatrix; // Nur
    /** 物品的总评分次数 */
    private DenseVector itemScoreVector; // Nu

    /** 主题的每评分概率 */
    private DenseMatrix topicScoreMatrix; // Pkr
    /** 主题的总评分概率 */
    private DenseVector topicScoreVector; // Pi

    /** 物品主题概率映射 */
    private DenseMatrix itemTopicProbabilities; // Gamma_(u,k)

    @Override
    protected boolean isConverged(int iter) {
        // TODO 需要重构
        float loss = 0F;
        for (int i = 0; i < numberOfItems; i++) {
            for (int k = 0; k < numberOfFactors; k++) {
                float rik = itemTopicProbabilities.getValue(i, k);
                float pi_k = topicScoreVector.getValue(k);

                float sum_nl = 0F;
                for (int scoreIndex = 0; scoreIndex < numberOfScores; scoreIndex++) {
                    float nir = itemScoreMatrix.getValue(i, scoreIndex);
                    float pkr = topicScoreMatrix.getValue(k, scoreIndex);

                    sum_nl += nir * Math.log(pkr);
                }

                loss += rik * (Math.log(pi_k) + sum_nl);
            }
        }
        float deltaLoss = (float) (loss - currentLoss);
        if (iter > 1 && (deltaLoss > 0 || Float.isNaN(deltaLoss))) {
            return true;
        }
        currentLoss = loss;
        return false;
    }

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        topicScoreMatrix = DenseMatrix.valueOf(numberOfFactors, numberOfScores);
        for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
            DenseVector probabilityVector = topicScoreMatrix.getRowVector(topicIndex);
            probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                float value = scalar.getValue();
                scalar.setValue(RandomUtility.randomInteger(numberOfScores) + 1);
            });
            probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
        }
        topicScoreVector = DenseVector.valueOf(numberOfFactors);
        topicScoreVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomInteger(numberOfFactors) + 1);
        });
        topicScoreVector.scaleValues(1F / topicScoreVector.getSum(false));
        // TODO
        topicScoreMatrix.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue((float) Math.log(scalar.getValue()));
        });
        topicScoreVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue((float) Math.log(scalar.getValue()));
        });

        itemScoreMatrix = DenseMatrix.valueOf(numberOfItems, numberOfScores);
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            SparseVector scoreVector = scoreMatrix.getColumnVector(itemIndex);
            for (VectorScalar term : scoreVector) {
                float score = term.getValue();
                int scoreIndex = scoreIndexes.get(score);
                itemScoreMatrix.shiftValue(itemIndex, scoreIndex, 1);
            }
        }
        itemScoreVector = DenseVector.valueOf(numberOfItems);
        itemScoreVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(scoreMatrix.getColumnVector(scalar.getIndex()).getElementSize());
        });
        currentLoss = Float.MIN_VALUE;

        itemTopicProbabilities = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
    }

    @Override
    protected void eStep() {
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            DenseVector probabilityVector = itemTopicProbabilities.getRowVector(itemIndex);
            SparseVector scoreVector = scoreMatrix.getColumnVector(itemIndex);
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
            for (int scoreIndex = 0; scoreIndex < numberOfScores; scoreIndex++) {
                float numerator = 0F, denorminator = 0F;
                for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
                    float probability = (float) FastMath.exp(itemTopicProbabilities.getValue(itemIndex, index));
                    numerator += probability * itemScoreMatrix.getValue(itemIndex, scoreIndex);
                    denorminator += probability * itemScoreVector.getValue(itemIndex);
                }
                float probability = (numerator / denorminator);
                topicScoreMatrix.setValue(index, scoreIndex, probability);
            }
            float sumProbability = 0F;
            for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
                float probability = (float) FastMath.exp(itemTopicProbabilities.getValue(itemIndex, index));
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
        for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
            float topicProbability = itemTopicProbabilities.getValue(itemIndex, topicIndex); // probability
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
