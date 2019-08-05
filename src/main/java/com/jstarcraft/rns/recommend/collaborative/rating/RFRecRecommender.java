package com.jstarcraft.rns.recommend.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;

import it.unimi.dsi.fastutil.floats.Float2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatRBTreeSet;
import it.unimi.dsi.fastutil.floats.FloatSet;

/**
 * 
 * RF Rec推荐器
 * 
 * <pre>
 * RF-Rec: Fast and Accurate Computation of Recommendations based on Rating Frequencies
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class RFRecRecommender extends MatrixFactorizationRecommender {
    /**
     * The average ratings of users
     */
    private DenseVector userMeans;

    /**
     * The average ratings of items
     */
    private DenseVector itemMeans;

    /** 分数索引 (TODO 考虑取消或迁移.本质为连续特征离散化) */
    protected Float2IntLinkedOpenHashMap scoreIndexes;

    /**
     * The number of ratings per rating value per user
     */
    private DenseMatrix userScoreFrequencies;

    /**
     * The number of ratings per rating value per item
     */
    private DenseMatrix itemScoreFrequencies;

    /**
     * User weights learned by the gradient solver
     */
    private DenseVector userWeights;

    /**
     * Item weights learned by the gradient solver.
     */
    private DenseVector itemWeights;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // Calculate the average ratings
        userMeans = DenseVector.valueOf(userSize);
        itemMeans = DenseVector.valueOf(itemSize);
        userWeights = DenseVector.valueOf(userSize);
        itemWeights = DenseVector.valueOf(itemSize);

        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            userMeans.setValue(userIndex, userVector.getSum(false) / userVector.getElementSize());
            userWeights.setValue(userIndex, 0.6F + RandomUtility.randomFloat(0.01F));
        }
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
            itemMeans.setValue(itemIndex, itemVector.getSum(false) / itemVector.getElementSize());
            itemWeights.setValue(itemIndex, 0.4F + RandomUtility.randomFloat(0.01F));
        }

        // TODO 此处会与scoreIndexes一起重构,本质为连续特征离散化.
        FloatSet scores = new FloatRBTreeSet();
        for (MatrixScalar term : scoreMatrix) {
            scores.add(term.getValue());
        }
        scores.remove(0F);
        scoreIndexes = new Float2IntLinkedOpenHashMap();
        int index = 0;
        for (float score : scores) {
            scoreIndexes.put(score, index++);
        }

        // Calculate the frequencies.
        // Users,items
        userScoreFrequencies = DenseMatrix.valueOf(userSize, actionSize);
        itemScoreFrequencies = DenseMatrix.valueOf(itemSize, actionSize);
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            int scoreIndex = scoreIndexes.get(term.getValue());
            userScoreFrequencies.shiftValue(userIndex, scoreIndex, 1F);
            itemScoreFrequencies.shiftValue(itemIndex, scoreIndex, 1F);
        }
    }

    @Override
    protected void doPractice() {
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                float error = term.getValue() - predict(userIndex, itemIndex);

                // Gradient-Step on user weights.
                float userWeight = userWeights.getValue(userIndex) + learnRatio * (error - userRegularization * userWeights.getValue(userIndex));
                userWeights.setValue(userIndex, userWeight);

                // Gradient-Step on item weights.
                float itemWeight = itemWeights.getValue(itemIndex) + learnRatio * (error - itemRegularization * itemWeights.getValue(itemIndex));
                itemWeights.setValue(itemIndex, itemWeight);
            }
        }
    }

    /**
     * Returns 1 if the rating is similar to the rounded average value
     *
     * @param mean the average
     * @param rate the rating
     * @return 1 when the values are equal
     */
    private int isMean(double mean, int rate) {
        return Math.round(mean) == rate ? 1 : 0;
    }

    @Override
    protected float predict(int userIndex, int itemIndex) {
        float value = meanScore;
        float userSum = userScoreFrequencies.getRowVector(userIndex).getSum(false);
        float itemSum = itemScoreFrequencies.getRowVector(itemIndex).getSum(false);
        float userMean = userMeans.getValue(userIndex);
        float itemMean = itemMeans.getValue(itemIndex);

        if (userSum > 0F && itemSum > 0F && userMean > 0F && itemMean > 0F) {
            float numeratorUser = 0F;
            float denominatorUser = 0F;
            float numeratorItem = 0F;
            float denominatorItem = 0F;
            float frequency = 0F;
            // Go through all the possible rating values
            for (int scoreIndex = 0, scoreSize = scoreIndexes.size(); scoreIndex < scoreSize; scoreIndex++) {
                // user component
                frequency = userScoreFrequencies.getValue(userIndex, scoreIndex);
                frequency = frequency + 1 + isMean(userMean, scoreIndex);
                numeratorUser += frequency * scoreIndex;
                denominatorUser += frequency;

                // item component
                frequency = itemScoreFrequencies.getValue(itemIndex, scoreIndex);
                frequency = frequency + 1 + isMean(itemMean, scoreIndex);
                numeratorItem += frequency * scoreIndex;
                denominatorItem += frequency;
            }

            float userWeight = userWeights.getValue(userIndex);
            float itemWeight = itemWeights.getValue(itemIndex);
            value = userWeight * numeratorUser / denominatorUser + itemWeight * numeratorItem / denominatorItem;
        } else {
            // if the user or item weren't known in the training phase...
            if (userSum == 0F || userMean == 0F) {
                if (itemMean != 0F) {
                    return itemMean;
                } else {
                    return meanScore;
                }
            }
            if (itemSum == 0F || itemMean == 0F) {
                if (userMean != 0F) {
                    return userMean;
                } else {
                    // Some heuristic -> a bit above the average rating
                    return meanScore;
                }
            }
        }
        return value;
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(predict(userIndex, itemIndex));
    }

}
