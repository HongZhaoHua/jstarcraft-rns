package com.jstarcraft.rns.recommend.collaborative.rating;

import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.ProbabilisticGraphicalRecommender;
import com.jstarcraft.rns.utility.SampleUtility;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntRBTreeMap;

/**
 * 
 * LDCC推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class LDCCRecommender extends ProbabilisticGraphicalRecommender {

    // TODO 重构为稀疏矩阵?
    private Int2IntRBTreeMap userTopics, itemTopics; // Zu, Zv

    private DenseMatrix userTopicTimes, itemTopicTimes; // Nui, Nvj
    private DenseVector userRateTimes, itemRateTimes; // Nv

    private DenseMatrix topicTimes;
    private DenseMatrix topicProbabilities;

    private DenseVector userProbabilities;
    private DenseVector itemProbabilities;

    private int[][][] rateTopicTimes;

    private int numberOfUserTopics, numberOfItemTopics;

    private float userAlpha, itemAlpha, ratingBeta;

    private DenseMatrix userTopicProbabilities, itemTopicProbabilities;
    private DenseMatrix userTopicSums, itemTopicSums;
    private float[][][] rateTopicProbabilities, rateTopicSums;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        numberOfStatistics = 0;

        numberOfUserTopics = configuration.getInteger("recommender.pgm.number.users", 10);
        numberOfItemTopics = configuration.getInteger("recommender.pgm.number.items", 10);

        userAlpha = configuration.getFloat("recommender.pgm.user.alpha", 1F / numberOfUserTopics);
        itemAlpha = configuration.getFloat("recommender.pgm.item.alpha", 1F / numberOfItemTopics);
        ratingBeta = configuration.getFloat("recommender.pgm.rating.beta", 1F / numberOfActions);

        userTopicTimes = DenseMatrix.valueOf(userSize, numberOfUserTopics);
        itemTopicTimes = DenseMatrix.valueOf(itemSize, numberOfItemTopics);
        userRateTimes = DenseVector.valueOf(userSize);
        itemRateTimes = DenseVector.valueOf(itemSize);

        rateTopicTimes = new int[numberOfUserTopics][numberOfItemTopics][numberOfActions];
        topicTimes = DenseMatrix.valueOf(numberOfUserTopics, numberOfItemTopics);
        topicProbabilities = DenseMatrix.valueOf(numberOfUserTopics, numberOfItemTopics);
        userProbabilities = DenseVector.valueOf(numberOfUserTopics);
        itemProbabilities = DenseVector.valueOf(numberOfItemTopics);

        userTopics = new Int2IntRBTreeMap();
        itemTopics = new Int2IntRBTreeMap();

        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float rate = term.getValue();
            int rateIndex = scoreIndexes.get(rate);

            int userTopic = RandomUtility.randomInteger(numberOfUserTopics);
            int itemTopic = RandomUtility.randomInteger(numberOfItemTopics);

            userTopicTimes.shiftValue(userIndex, userTopic, 1);
            userRateTimes.shiftValue(userIndex, 1);

            itemTopicTimes.shiftValue(itemIndex, itemTopic, 1);
            itemRateTimes.shiftValue(itemIndex, 1);

            rateTopicTimes[userTopic][itemTopic][rateIndex]++;
            topicTimes.shiftValue(userTopic, itemTopic, 1);

            userTopics.put(userIndex * itemSize + itemIndex, userTopic);
            itemTopics.put(userIndex * itemSize + itemIndex, itemTopic);
        }

        // parameters
        userTopicSums = DenseMatrix.valueOf(userSize, numberOfUserTopics);
        itemTopicSums = DenseMatrix.valueOf(itemSize, numberOfItemTopics);
        rateTopicProbabilities = new float[numberOfUserTopics][numberOfItemTopics][numberOfActions];
        rateTopicSums = new float[numberOfUserTopics][numberOfItemTopics][numberOfActions];
    }

    @Override
    protected void eStep() {
        // 缓存概率
        float random = 0F;

        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float rate = term.getValue();
            // TODO 此处可以重构
            int rateIndex = scoreIndexes.get(rate);
            // TODO 此处可以重构
            // user and item's factors
            int userTopic = userTopics.get(userIndex * itemSize + itemIndex);
            int itemTopic = itemTopics.get(userIndex * itemSize + itemIndex);

            // remove this observation
            userTopicTimes.shiftValue(userIndex, userTopic, -1);
            userRateTimes.shiftValue(userIndex, -1);

            itemTopicTimes.shiftValue(itemIndex, itemTopic, -1);
            itemRateTimes.shiftValue(itemIndex, -1);

            rateTopicTimes[userTopic][itemTopic][rateIndex]--;
            topicTimes.shiftValue(userTopic, itemTopic, -1);

            int topicIndex = userTopic;
            // TODO
            // 此处topicProbabilities似乎可以与userProbabilities和itemProbabilities整合.
            // Compute P(i, j)
            // 归一化
            topicProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int row = scalar.getRow();
                int column = scalar.getColumn();
                // Compute Pmn
                float v1 = (userTopicTimes.getValue(userIndex, row) + userAlpha) / (userRateTimes.getValue(userIndex) + numberOfUserTopics * userAlpha);
                float v2 = (userTopicTimes.getValue(topicIndex, column) + itemAlpha) / (itemRateTimes.getValue(itemIndex) + numberOfItemTopics * itemAlpha);
                float v3 = (rateTopicTimes[row][column][rateIndex] + ratingBeta) / (topicTimes.getValue(row, column) + numberOfActions * ratingBeta);
                float value = v1 * v2 * v3;
                scalar.setValue(value);
            });
            // Re-sample user factor
            // 计算概率
            DefaultScalar sum = DefaultScalar.getInstance();
            sum.setValue(0F);
            userProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = topicProbabilities.getRowVector(index).getSum(false);
                sum.shiftValue(value);
                scalar.setValue(sum.getValue());
            });
            userTopic = SampleUtility.binarySearch(userProbabilities, 0, userProbabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));
            sum.setValue(0F);
            itemProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                int index = scalar.getIndex();
                float value = topicProbabilities.getColumnVector(index).getSum(false);
                sum.shiftValue(value);
                scalar.setValue(sum.getValue());
            });
            itemTopic = SampleUtility.binarySearch(itemProbabilities, 0, itemProbabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));

            // Add statistics
            userTopicTimes.shiftValue(userIndex, userTopic, 1);
            userRateTimes.shiftValue(userIndex, 1);

            itemTopicTimes.shiftValue(itemIndex, itemTopic, 1);
            itemRateTimes.shiftValue(itemIndex, 1);

            rateTopicTimes[userTopic][itemTopic][rateIndex]++;
            topicTimes.shiftValue(userTopic, itemTopic, 1);

            userTopics.put(userIndex * itemSize + itemIndex, userTopic);
            itemTopics.put(userIndex * itemSize + itemIndex, itemTopic);
        }
    }

    @Override
    protected void mStep() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void readoutParams() {
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            for (int topicIndex = 0; topicIndex < numberOfUserTopics; topicIndex++) {
                userTopicSums.shiftValue(userIndex, topicIndex, (userTopicTimes.getValue(userIndex, topicIndex) + userAlpha) / (userRateTimes.getValue(userIndex) + numberOfUserTopics * userAlpha));
            }
        }

        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            for (int topicIndex = 0; topicIndex < numberOfItemTopics; topicIndex++) {
                itemTopicSums.shiftValue(itemIndex, topicIndex, (itemTopicTimes.getValue(itemIndex, topicIndex) + itemAlpha) / (itemRateTimes.getValue(itemIndex) + numberOfItemTopics * itemAlpha));
            }
        }

        for (int userTopic = 0; userTopic < numberOfUserTopics; userTopic++) {
            for (int itemTopic = 0; itemTopic < numberOfItemTopics; itemTopic++) {
                for (int rateIndex = 0; rateIndex < numberOfActions; rateIndex++) {
                    rateTopicSums[userTopic][itemTopic][rateIndex] += (rateTopicTimes[userTopic][itemTopic][rateIndex] + ratingBeta) / (topicTimes.getValue(userTopic, itemTopic) + numberOfActions * ratingBeta);
                }
            }
        }
        numberOfStatistics++;
    }

    /**
     * estimate the model parameters
     */
    @Override
    protected void estimateParams() {
        float scale = 1F / numberOfStatistics;
        // TODO
        // 此处可以重构(整合userTopicProbabilities/userTopicSums和itemTopicProbabilities/itemTopicSums)
        userTopicProbabilities = DenseMatrix.copyOf(userTopicSums);
        userTopicProbabilities.scaleValues(scale);
        itemTopicProbabilities = DenseMatrix.copyOf(itemTopicSums);
        itemTopicProbabilities.scaleValues(scale);

        // TODO 此处可以重构(整合rateTopicProbabilities/rateTopicSums)
        for (int userTopic = 0; userTopic < numberOfUserTopics; userTopic++) {
            for (int itemTopic = 0; itemTopic < numberOfItemTopics; itemTopic++) {
                for (int rateIndex = 0; rateIndex < numberOfActions; rateIndex++) {
                    rateTopicProbabilities[userTopic][itemTopic][rateIndex] = rateTopicSums[userTopic][itemTopic][rateIndex] / numberOfStatistics;
                }
            }
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        float value = 0F;
        for (Entry<Float, Integer> term : scoreIndexes.entrySet()) {
            float rate = term.getKey();
            int rateIndex = term.getValue();
            float probability = 0F; // P(r|u,v)=\sum_{i,j} P(r|i,j)P(i|u)P(j|v)
            for (int userTopic = 0; userTopic < numberOfUserTopics; userTopic++) {
                for (int itemTopic = 0; itemTopic < numberOfItemTopics; itemTopic++) {
                    probability += rateTopicProbabilities[userTopic][itemTopic][rateIndex] * userTopicProbabilities.getValue(userIndex, userTopic) * itemTopicProbabilities.getValue(itemIndex, itemTopic);
                }
            }
            value += rate * probability;
        }
        instance.setQuantityMark(value);
    }

    @Override
    protected boolean isConverged(int iter) {
        // Get the parameters
        estimateParams();
        // Compute the perplexity
        float sum = 0F;
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float rate = term.getValue();
            sum += perplexity(userIndex, itemIndex, rate);
        }
        float perplexity = (float) Math.exp(sum / numberOfActions);
        float delta = perplexity - currentLoss;
        if (numberOfStatistics > 1 && delta > 0) {
            return true;
        }
        currentLoss = perplexity;
        return false;
    }

    private double perplexity(int user, int item, double rate) {
        int rateIndex = (int) (rate / minimumOfScore - 1);
        // Compute P(r | u, v)
        double probability = 0;
        for (int userTopic = 0; userTopic < numberOfUserTopics; userTopic++) {
            for (int itemTopic = 0; itemTopic < numberOfItemTopics; itemTopic++) {
                probability += rateTopicProbabilities[userTopic][itemTopic][rateIndex] * userTopicProbabilities.getValue(user, userTopic) * itemTopicProbabilities.getValue(item, itemTopic);
            }
        }
        return -Math.log(probability);
    }

}
