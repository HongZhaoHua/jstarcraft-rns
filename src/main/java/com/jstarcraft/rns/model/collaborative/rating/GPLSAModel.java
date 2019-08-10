package com.jstarcraft.rns.model.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.MathUtility;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.table.SparseTable;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.Float2FloatKeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.ProbabilisticGraphicalModel;
import com.jstarcraft.rns.utility.GaussianUtility;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;

/**
 * 
 * GPLSA推荐器
 * 
 * <pre>
 * Collaborative Filtering via Gaussian Probabilistic Latent Semantic Analysis
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class GPLSAModel extends ProbabilisticGraphicalModel {

    /*
     * {user, item, {topic z, probability}}
     */
    protected SparseTable<float[]> probabilityTensor;
    /*
     * Conditional Probability: P(z|u)
     */
    protected DenseMatrix userTopicProbabilities;
    /*
     * Conditional Probability: P(v|y,z)
     */
    protected DenseMatrix itemMus, itemSigmas;
    /*
     * regularize ratings
     */
    protected DenseVector userMus, userSigmas;
    /*
     * smoothing weight
     */
    protected float smoothWeight;
    /*
     * tempered EM parameter beta, suggested by Wu Bin
     */
    protected float beta;
    /*
     * small value for initialization
     */
    protected static float smallValue = MathUtility.EPSILON;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // Initialize users' conditional probabilities
        userTopicProbabilities = DenseMatrix.valueOf(userSize, factorSize);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            DenseVector probabilityVector = userTopicProbabilities.getRowVector(userIndex);
            probabilityVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                scalar.setValue(RandomUtility.randomInteger(factorSize) + 1);
            });
            probabilityVector.scaleValues(1F / probabilityVector.getSum(false));
        }

        Float2FloatKeyValue keyValue = scoreMatrix.getVariance();
        float mean = keyValue.getKey();
        float variance = keyValue.getValue() / scoreMatrix.getElementSize();

        userMus = DenseVector.valueOf(userSize);
        userSigmas = DenseVector.valueOf(userSize);
        smoothWeight = configuration.getInteger("recommender.recommender.smoothWeight");
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            int size = userVector.getElementSize();
            if (size < 1) {
                continue;
            }
            float mu = (userVector.getSum(false) + smoothWeight * mean) / (size + smoothWeight);
            userMus.setValue(userIndex, mu);
            float sigma = userVector.getVariance(mu);
            sigma += smoothWeight * variance;
            sigma = (float) Math.sqrt(sigma / (size + smoothWeight));
            userSigmas.setValue(userIndex, sigma);
        }

        // Initialize Q
        // TODO 重构
        probabilityTensor = new SparseTable<>(true, userSize, itemSize, new Int2ObjectRBTreeMap<>());

        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float score = term.getValue();
            score = (score - userMus.getValue(userIndex)) / userSigmas.getValue(userIndex);
            term.setValue(score);
            probabilityTensor.setValue(userIndex, itemIndex, new float[factorSize]);
        }

        itemMus = DenseMatrix.valueOf(itemSize, factorSize);
        itemSigmas = DenseMatrix.valueOf(itemSize, factorSize);
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
            int size = itemVector.getElementSize();
            if (size < 1) {
                continue;
            }
            float mu = itemVector.getSum(false) / size;
            float sigma = itemVector.getVariance(mu);
            sigma = (float) Math.sqrt(sigma / size);
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                itemMus.setValue(itemIndex, topicIndex, mu + smallValue * RandomUtility.randomFloat(1F));
                itemSigmas.setValue(itemIndex, topicIndex, sigma + smallValue * RandomUtility.randomFloat(1F));
            }
        }
    }

    @Override
    protected void eStep() {
        // variational inference to compute Q
        float[] numerators = new float[factorSize];
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float score = term.getValue();
            float denominator = 0F;
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                float pdf = GaussianUtility.probabilityDensity(score, itemMus.getValue(itemIndex, topicIndex), itemSigmas.getValue(itemIndex, topicIndex));
                float value = (float) Math.pow(userTopicProbabilities.getValue(userIndex, topicIndex) * pdf, beta); // Tempered
                // EM
                numerators[topicIndex] = value;
                denominator += value;
            }
            float[] probabilities = probabilityTensor.getValue(userIndex, itemIndex);
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                float probability = (denominator > 0 ? numerators[topicIndex] / denominator : 0);
                probabilities[topicIndex] = probability;
            }
        }
    }

    @Override
    protected void mStep() {
        float[] numerators = new float[factorSize];
        // theta_u,z
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            if (userVector.getElementSize() < 1) {
                continue;
            }
            float denominator = 0F;
            for (VectorScalar term : userVector) {
                int itemIndex = term.getIndex();
                float[] probabilities = probabilityTensor.getValue(userIndex, itemIndex);
                for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                    numerators[topicIndex] = probabilities[topicIndex];
                    denominator += numerators[topicIndex];
                }
            }
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                userTopicProbabilities.setValue(userIndex, topicIndex, numerators[topicIndex] / denominator);
            }
        }

        // topicItemMu, topicItemSigma
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
            if (itemVector.getElementSize() < 1) {
                continue;
            }
            float numerator = 0F, denominator = 0F;
            for (VectorScalar term : itemVector) {
                int userIndex = term.getIndex();
                float score = term.getValue();
                float[] probabilities = probabilityTensor.getValue(userIndex, itemIndex);
                for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                    float probability = probabilities[topicIndex];
                    numerator += score * probability;
                    denominator += probability;
                }
            }
            float mu = denominator > 0F ? numerator / denominator : 0F;
            numerator = 0F;
            for (VectorScalar term : itemVector) {
                int userIndex = term.getIndex();
                float score = term.getValue();
                float[] probabilities = probabilityTensor.getValue(userIndex, itemIndex);
                for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                    double probability = probabilities[topicIndex];
                    numerator += Math.pow(score - mu, 2) * probability;
                }
            }
            float sigma = (float) (denominator > 0F ? Math.sqrt(numerator / denominator) : 0F);
            for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
                itemMus.setValue(itemIndex, topicIndex, mu);
                itemSigmas.setValue(itemIndex, topicIndex, sigma);
            }
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float sum = 0F;
        for (int topicIndex = 0; topicIndex < factorSize; topicIndex++) {
            sum += userTopicProbabilities.getValue(userIndex, topicIndex) * itemMus.getValue(itemIndex, topicIndex);
        }
        instance.setQuantityMark(userMus.getValue(userIndex) + userSigmas.getValue(userIndex) * sum);
    }

}
