package com.jstarcraft.rns.model.collaborative;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.MathCell;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.table.SparseTable;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.ProbabilisticGraphicalModel;
import com.jstarcraft.rns.utility.SampleUtility;

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;

/**
 * 
 * BH Free推荐器
 * 
 * <pre>
 * Balancing Prediction and Recommendation Accuracy: Hierarchical Latent Factors for Preference Data
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class BHFreeModel extends ProbabilisticGraphicalModel {

    private static class TopicTerm {

        private int userTopic;

        private int itemTopic;

        private int scoreIndex;

        private TopicTerm(int userTopic, int itemTopic, int scoreIndex) {
            this.userTopic = userTopic;
            this.itemTopic = itemTopic;
            this.scoreIndex = scoreIndex;
        }

        void update(int userTopic, int itemTopic) {
            this.userTopic = userTopic;
            this.itemTopic = itemTopic;
        }

        public int getUserTopic() {
            return userTopic;
        }

        public int getItemTopic() {
            return itemTopic;
        }

        public int getScoreIndex() {
            return scoreIndex;
        }

    }

    private SparseTable<TopicTerm> topicMatrix;

    private float initGamma, initSigma, initAlpha, initBeta;

    /**
     * number of user communities
     */
    protected int userTopicSize; // K

    /**
     * number of item categories
     */
    protected int itemTopicSize; // L

    /**
     * evaluation of the user u which have been assigned to the user topic k
     */
    private DenseMatrix user2TopicNumbers;

    /**
     * observations for the user
     */
    private DenseVector userNumbers;

    /**
     * observations associated with community k
     */
    private DenseVector userTopicNumbers;

    /**
     * number of user communities * number of topics
     */
    private DenseMatrix userTopic2ItemTopicNumbers; // Nkl

    /**
     * number of user communities * number of topics * number of ratings
     */
    private int[][][] userTopic2ItemTopicScoreNumbers, userTopic2ItemTopicItemNumbers; // Nklr,
    // Nkli;

    // parameters
    protected DenseMatrix user2TopicProbabilities, userTopic2ItemTopicProbabilities;
    protected DenseMatrix user2TopicSums, userTopic2ItemTopicSums;
    protected double[][][] userTopic2ItemTopicScoreProbabilities, userTopic2ItemTopicItemProbabilities;
    protected double[][][] userTopic2ItemTopicScoreSums, userTopic2ItemTopicItemSums;

    private DenseMatrix topicProbabilities;
    private DenseVector userProbabilities;
    private DenseVector itemProbabilities;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userTopicSize = configuration.getInteger("recommender.bhfree.user.topic.number", 10);
        itemTopicSize = configuration.getInteger("recommender.bhfree.item.topic.number", 10);
        initAlpha = configuration.getFloat("recommender.bhfree.alpha", 1.0f / userTopicSize);
        initBeta = configuration.getFloat("recommender.bhfree.beta", 1.0f / itemTopicSize);
        initGamma = configuration.getFloat("recommender.bhfree.gamma", 1.0f / scoreSize);
        initSigma = configuration.getFloat("recommender.sigma", 1.0f / itemSize);
        scoreSize = scoreIndexes.size();

        // TODO 考虑重构(整合为UserTopic对象)
        user2TopicNumbers = DenseMatrix.valueOf(userSize, userTopicSize);
        userNumbers = DenseVector.valueOf(userSize);

        userTopic2ItemTopicNumbers = DenseMatrix.valueOf(userTopicSize, itemTopicSize);
        userTopicNumbers = DenseVector.valueOf(userTopicSize);

        userTopic2ItemTopicScoreNumbers = new int[userTopicSize][itemTopicSize][scoreSize];
        userTopic2ItemTopicItemNumbers = new int[userTopicSize][itemTopicSize][itemSize];

        topicMatrix = new SparseTable<>(true, userSize, itemSize, new Int2ObjectRBTreeMap<>());

        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            float score = term.getValue();
            int scoreIndex = scoreIndexes.get(score);
            int userTopic = RandomUtility.randomInteger(userTopicSize); // user's
            // topic
            // k
            int itemTopic = RandomUtility.randomInteger(itemTopicSize); // item's
            // topic
            // l

            user2TopicNumbers.shiftValue(userIndex, userTopic, 1F);
            userNumbers.shiftValue(userIndex, 1F);
            userTopic2ItemTopicNumbers.shiftValue(userTopic, itemTopic, 1F);
            userTopicNumbers.shiftValue(userTopic, 1F);
            userTopic2ItemTopicScoreNumbers[userTopic][itemTopic][scoreIndex]++;
            userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex]++;
            TopicTerm topic = new TopicTerm(userTopic, itemTopic, scoreIndex);
            topicMatrix.setValue(userIndex, itemIndex, topic);
        }

        // parameters
        // TODO 考虑重构为一个对象
        user2TopicSums = DenseMatrix.valueOf(userSize, userTopicSize);
        userTopic2ItemTopicSums = DenseMatrix.valueOf(userTopicSize, itemTopicSize);
        userTopic2ItemTopicScoreSums = new double[userTopicSize][itemTopicSize][scoreSize];
        userTopic2ItemTopicScoreProbabilities = new double[userTopicSize][itemTopicSize][scoreSize];
        userTopic2ItemTopicItemSums = new double[userTopicSize][itemTopicSize][itemSize];
        userTopic2ItemTopicItemProbabilities = new double[userTopicSize][itemTopicSize][itemSize];

        topicProbabilities = DenseMatrix.valueOf(userTopicSize, itemTopicSize);
        userProbabilities = DenseVector.valueOf(userTopicSize);
        itemProbabilities = DenseVector.valueOf(itemTopicSize);
    }

    @Override
    protected void eStep() {
        for (MathCell<TopicTerm> term : topicMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            TopicTerm topicTerm = term.getValue();
            int scoreIndex = topicTerm.getScoreIndex();
            int userTopic = topicTerm.getUserTopic();
            int itemTopic = topicTerm.getItemTopic();

            user2TopicNumbers.shiftValue(userIndex, userTopic, -1F);
            userNumbers.shiftValue(userIndex, -1F);
            userTopic2ItemTopicNumbers.shiftValue(userTopic, itemTopic, -1F);
            userTopicNumbers.shiftValue(userTopic, -1F);
            userTopic2ItemTopicScoreNumbers[userTopic][itemTopic][scoreIndex]--;
            userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex]--;

            // normalization
            int userTopicIndex = userTopic;
            int itemTopicIndex = itemTopic;
            topicProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                float value = (user2TopicNumbers.getValue(userIndex, userTopicIndex) + initAlpha) / (userNumbers.getValue(userIndex) + userTopicSize * initAlpha);
                value *= (userTopic2ItemTopicNumbers.getValue(userTopicIndex, itemTopicIndex) + initBeta) / (userTopicNumbers.getValue(userTopicIndex) + itemTopicSize * initBeta);
                value *= (userTopic2ItemTopicScoreNumbers[userTopicIndex][itemTopicIndex][scoreIndex] + initGamma) / (userTopic2ItemTopicNumbers.getValue(userTopicIndex, itemTopicIndex) + scoreSize * initGamma);
                value *= (userTopic2ItemTopicItemNumbers[userTopicIndex][itemTopicIndex][itemIndex] + initSigma) / (userTopic2ItemTopicNumbers.getValue(userTopicIndex, itemTopicIndex) + itemSize * initSigma);
                scalar.setValue(value);
            });

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

            topicTerm.update(userTopic, itemTopic);
            // add statistic
            user2TopicNumbers.shiftValue(userIndex, userTopic, 1F);
            userNumbers.shiftValue(userIndex, 1F);
            userTopic2ItemTopicNumbers.shiftValue(userTopic, itemTopic, 1F);
            userTopicNumbers.shiftValue(userTopic, 1F);
            userTopic2ItemTopicScoreNumbers[userTopic][itemTopic][scoreIndex]++;
            userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex]++;
        }

    }

    @Override
    protected void mStep() {

    }

    @Override
    protected void readoutParameters() {
        for (int userTopic = 0; userTopic < userTopicSize; userTopic++) {
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                user2TopicSums.shiftValue(userIndex, userTopic, (user2TopicNumbers.getValue(userIndex, userTopic) + initAlpha) / (userNumbers.getValue(userIndex) + userTopicSize * initAlpha));
            }
            for (int itemTopic = 0; itemTopic < itemTopicSize; itemTopic++) {
                userTopic2ItemTopicSums.shiftValue(userTopic, itemTopic, (userTopic2ItemTopicNumbers.getValue(userTopic, itemTopic) + initBeta) / (userTopicNumbers.getValue(userTopic) + itemTopicSize * initBeta));
                for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
                    userTopic2ItemTopicScoreSums[userTopic][itemTopic][scoreIndex] += (userTopic2ItemTopicScoreNumbers[userTopic][itemTopic][scoreIndex] + initGamma) / (userTopic2ItemTopicNumbers.getValue(userTopic, itemTopic) + scoreSize * initGamma);
                }
                for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                    userTopic2ItemTopicItemSums[userTopic][itemTopic][itemIndex] += (userTopic2ItemTopicItemNumbers[userTopic][itemTopic][itemIndex] + initSigma) / (userTopic2ItemTopicNumbers.getValue(userTopic, itemTopic) + itemSize * initSigma);
                }
            }
        }
        numberOfStatistics++;
    }

    @Override
    protected void estimateParameters() {
        float scale = 1F / numberOfStatistics;
        user2TopicProbabilities = DenseMatrix.copyOf(user2TopicSums);
        user2TopicProbabilities.scaleValues(scale);
        userTopic2ItemTopicProbabilities = DenseMatrix.copyOf(userTopic2ItemTopicSums);
        userTopic2ItemTopicProbabilities.scaleValues(scale);
        for (int userTopic = 0; userTopic < userTopicSize; userTopic++) {
            for (int itemTopic = 0; itemTopic < itemTopicSize; itemTopic++) {
                for (int scoreIndex = 0; scoreIndex < scoreSize; scoreIndex++) {
                    userTopic2ItemTopicScoreProbabilities[userTopic][itemTopic][scoreIndex] = userTopic2ItemTopicScoreSums[userTopic][itemTopic][scoreIndex] * scale;
                }
                for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                    userTopic2ItemTopicItemProbabilities[userTopic][itemTopic][itemIndex] = userTopic2ItemTopicItemSums[userTopic][itemTopic][itemIndex] * scale;
                }
            }
        }
    }

}
