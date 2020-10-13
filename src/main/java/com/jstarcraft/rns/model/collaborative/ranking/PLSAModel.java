package com.jstarcraft.rns.model.collaborative.ranking;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.table.SparseTable;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.ProbabilisticGraphicalModel;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;

/**
 * 
 * PLSA推荐器
 * 
 * <pre>
 * Latent semantic models for collaborative filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class PLSAModel extends ProbabilisticGraphicalModel {

    /**
     * {user, item, {topic z, probability}}
     */
    private SparseTable<DenseVector> probabilityTensor;

    /**
     * Conditional Probability: P(z|u)
     */
    private DenseMatrix userTopicProbabilities, userTopicSums;

    /**
     * Conditional Probability: P(i|z)
     */
    private DenseMatrix topicItemProbabilities, topicItemSums;

    /**
     * topic probability sum value
     */
    private DenseVector topicProbabilities;

    /**
     * entry[u]: number of tokens rated by user u.
     */
    private DenseVector userScoreTimes;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        // TODO 此处代码可以消除(使用常量Marker代替或者使用binarize.threshold)
        for (MatrixScalar term : scoreMatrix) {
            term.setValue(1F);
        }

        userTopicSums = DenseMatrix.valueOf(userSize, factorSize);
        topicItemSums = DenseMatrix.valueOf(factorSize, itemSize);
        topicProbabilities = DenseVector.valueOf(factorSize);

        userTopicProbabilities = DenseMatrix.valueOf(userSize, factorSize);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            DenseVector probabilityVector = userTopicProbabilities.getRowVector(userIndex);
            probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(RandomUtility.randomInteger(userSize) + 1);
            });
            probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
        }

        topicItemProbabilities = DenseMatrix.valueOf(factorSize, itemSize);
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            DenseVector probabilityVector = topicItemProbabilities.getRowVector(topicIndex);
            probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(RandomUtility.randomInteger(itemSize) + 1);
            });
            probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
        }

        // initialize Q

        // initialize Q
        probabilityTensor = new SparseTable<>(true, userSize, itemSize, new Int2ObjectRBTreeMap<>());
        userScoreTimes = DenseVector.valueOf(userSize);
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            probabilityTensor.setValue(userIndex, itemIndex, DenseVector.valueOf(factorSize));
            userScoreTimes.shiftValue(userIndex, term.getValue());
        }
    }

    @Override
    protected void eStep() {
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            DenseVector probabilities = probabilityTensor.getValue(userIndex, itemIndex);
            probabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = userTopicProbabilities.getValue(userIndex, index) * topicItemProbabilities.getValue(index, itemIndex);
                scalar.setValue(value);
            });
            probabilities.scaleValues(1F / probabilities.getSum(false));
        }
    }

    @Override
    protected void mStep() {
        userTopicSums.setValues(0F);
        topicItemSums.setValues(0F);
        topicProbabilities.setValues(0F);
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float numerator = term.getValue();
            DenseVector probabilities = probabilityTensor.getValue(userIndex, itemIndex);
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                float value = probabilities.getValue(topicIndex) * numerator;
                userTopicSums.shiftValue(userIndex, topicIndex, value);
                topicItemSums.shiftValue(topicIndex, itemIndex, value);
                topicProbabilities.shiftValue(topicIndex, value);
            }
        }

        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            float denominator = userScoreTimes.getValue(userIndex);
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                float value = denominator > 0F ? userTopicSums.getValue(userIndex, topicIndex) / denominator : 0F;
                userTopicProbabilities.setValue(userIndex, topicIndex, value);
            }
        }

        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            float probability = topicProbabilities.getValue(topicIndex);
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                float value = probability > 0F ? topicItemSums.getValue(topicIndex, itemIndex) / probability : 0F;
                topicItemProbabilities.setValue(topicIndex, itemIndex, value);
            }
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseVector userVector = userTopicProbabilities.getRowVector(userIndex);
        DenseVector itemVector = topicItemProbabilities.getColumnVector(itemIndex);
        instance.setQuantityMark(scalar.dotProduct(userVector, itemVector).getValue());
    }

}
