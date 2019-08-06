package com.jstarcraft.rns.recommend.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;

/**
 * 
 * LLORMA推荐器
 * 
 * <pre>
 * Local Low-Rank Matrix Approximation
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class LLORMARecommender extends MatrixFactorizationRecommender {
    private int numberOfGlobalFactors, numberOfLocalFactors;
    private int globalEpocheSize, localEpocheSize;
    private int numberOfThreads;
    private float globalUserRegularization, globalItemRegularization, localUserRegularization, localItemRegularization;
    private float globalLearnRatio, localLearnRatio;

    private int numberOfModels;
    private DenseMatrix globalUserFactors, globalItemFactors;

    private DenseMatrix[] userMatrixes;

    private DenseMatrix[] itemMatrixes;

    private int[] anchorUsers;
    private int[] anchorItems;

    /*
     * (non-Javadoc)
     *
     * @see net.librecommender.recommender.AbstractRecommender#setup()
     */
    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        numberOfGlobalFactors = configuration.getInteger("recommender.global.factors.num", 20);
        numberOfLocalFactors = factorSize;

        globalEpocheSize = configuration.getInteger("recommender.global.iteration.maximum", 100);
        localEpocheSize = epocheSize;

        globalUserRegularization = configuration.getFloat("recommender.global.user.regularization", 0.01F);
        globalItemRegularization = configuration.getFloat("recommender.global.item.regularization", 0.01F);
        localUserRegularization = userRegularization;
        localItemRegularization = itemRegularization;

        globalLearnRatio = configuration.getFloat("recommender.global.iteration.learnrate", 0.01F);
        localLearnRatio = configuration.getFloat("recommender.iteration.learnrate", 0.01F);

        numberOfThreads = configuration.getInteger("recommender.thread.count", 4);
        numberOfModels = configuration.getInteger("recommender.model.num", 50);

        numberOfThreads = numberOfThreads > numberOfModels ? numberOfModels : numberOfThreads;

        // global svd P Q to calculate the kernel value between users (or items)
        globalUserFactors = DenseMatrix.valueOf(userSize, numberOfGlobalFactors);
        globalUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        globalItemFactors = DenseMatrix.valueOf(itemSize, numberOfGlobalFactors);
        globalItemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
    }

    // global svd P Q
    private void practiceGlobalModel(DefaultScalar scalar) {
        for (int epocheIndex = 0; epocheIndex < globalEpocheSize; epocheIndex++) {
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow(); // user
                int itemIndex = term.getColumn(); // item
                float score = term.getValue();

                // TODO 考虑重构,减少userVector与itemVector的重复构建
                DenseVector userVector = globalUserFactors.getRowVector(userIndex);
                DenseVector itemVector = globalItemFactors.getRowVector(itemIndex);
                float predict = scalar.dotProduct(userVector, itemVector).getValue();
                float error = score - predict;

                // update factors
                for (int factorIndex = 0; factorIndex < numberOfGlobalFactors; factorIndex++) {
                    float userFactor = globalUserFactors.getValue(userIndex, factorIndex);
                    float itemFactor = globalItemFactors.getValue(itemIndex, factorIndex);
                    globalUserFactors.shiftValue(userIndex, factorIndex, globalLearnRatio * (error * itemFactor - globalUserRegularization * userFactor));
                    globalItemFactors.shiftValue(itemIndex, factorIndex, globalLearnRatio * (error * userFactor - globalItemRegularization * itemFactor));
                }
            }
        }

        userMatrixes = new DenseMatrix[numberOfModels];
        itemMatrixes = new DenseMatrix[numberOfModels];
        anchorUsers = new int[numberOfModels];
        anchorItems = new int[numberOfModels];
        // end of training
    }

    /**
     * Calculate similarity between two users, based on the global base SVD.
     *
     * @param leftUserIndex  The first user's ID.
     * @param rightUserIndex The second user's ID.
     * @return The similarity value between two users idx1 and idx2.
     */
    private float getUserSimilarity(DefaultScalar scalar, int leftUserIndex, int rightUserIndex) {
        float similarity;
        // TODO 减少向量的重复构建
        DenseVector leftUserVector = globalUserFactors.getRowVector(leftUserIndex);
        DenseVector rightUserVector = globalUserFactors.getRowVector(rightUserIndex);
        similarity = (float) (1 - 2F / Math.PI * Math.acos(scalar.dotProduct(leftUserVector, rightUserVector).getValue() / (Math.sqrt(scalar.dotProduct(leftUserVector, leftUserVector).getValue()) * Math.sqrt(scalar.dotProduct(rightUserVector, rightUserVector).getValue()))));
        if (Double.isNaN(similarity)) {
            similarity = 0F;
        }
        return similarity;
    }

    /**
     * Calculate similarity between two items, based on the global base SVD.
     *
     * @param leftItemIndex  The first item's ID.
     * @param rightItemIndex The second item's ID.
     * @return The similarity value between two items idx1 and idx2.
     */
    private float getItemSimilarity(DefaultScalar scalar, int leftItemIndex, int rightItemIndex) {
        float similarity;
        // TODO 减少向量的重复构建
        DenseVector leftItemVector = globalItemFactors.getRowVector(leftItemIndex);
        DenseVector rightItemVector = globalItemFactors.getRowVector(rightItemIndex);
        similarity = (float) (1 - 2D / Math.PI * Math.acos(scalar.dotProduct(leftItemVector, rightItemVector).getValue() / (Math.sqrt(scalar.dotProduct(leftItemVector, leftItemVector).getValue()) * Math.sqrt(scalar.dotProduct(rightItemVector, rightItemVector).getValue()))));
        if (Double.isNaN(similarity)) {
            similarity = 0F;
        }
        return similarity;
    }

    /**
     * Given the similarity, it applies the given kernel. This is done either for
     * all users or for all items.
     *
     * @param size          The length of user or item vector.
     * @param anchorIdx     The identifier of anchor point.
     * @param type          The type of kernel.
     * @param width         Kernel width.
     * @param isItemFeature return item kernel if yes, return user kernel otherwise.
     * @return The kernel-smoothed values for all users or all items.
     */
    private DenseVector kernelSmoothing(DefaultScalar scalar, int size, int anchorIdx, KernelSmoother type, float width, boolean isItemFeature) {
        DenseVector featureVector = DenseVector.valueOf(size);
        // TODO 此处似乎有Bug?
        featureVector.setValue(anchorIdx, 1F);
        for (int index = 0; index < size; index++) {
            float similarity;
            if (isItemFeature) {
                similarity = getItemSimilarity(scalar, index, anchorIdx);
            } else { // userFeature
                similarity = getUserSimilarity(scalar, index, anchorIdx);
            }
            featureVector.setValue(index, type.kernelize(similarity, width));
        }
        return featureVector;
    }

    private void practiceLocalModels(DefaultScalar scalar) {
        // Pre-calculating similarity:
        int completeModelCount = 0;

        // TODO 此处的变量与矩阵可以整合到LLORMALearner,LLORMALearner变成任务.
        LLORMALearner[] learners = new LLORMALearner[numberOfThreads];

        int modelCount = 0;
        int[] runningThreadList = new int[numberOfThreads];
        int runningThreadCount = 0;
        int waitingThreadPointer = 0;
        int nextRunningSlot = 0;

        // Parallel training:
        while (completeModelCount < numberOfModels) {
            int randomUserIndex = RandomUtility.randomInteger(userSize);
            // TODO 考虑重构
            SparseVector userVector = scoreMatrix.getRowVector(randomUserIndex);
            if (userVector.getElementSize() == 0) {
                continue;
            }
            // TODO 此处的并发模型有问题,需要重构.否则当第一次runningThreadCount >=
            // numThreads之后,都是单线程执行.
            if (runningThreadCount < numberOfThreads && modelCount < numberOfModels) {
                // Selecting a new anchor point:
                int randomItemIndex = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));
                anchorUsers[modelCount] = randomUserIndex;
                anchorItems[modelCount] = randomItemIndex;
                // Preparing weight vectors:
                DenseVector userWeights = kernelSmoothing(scalar, userSize, randomUserIndex, KernelSmoother.EPANECHNIKOV_KERNEL, 0.8F, false);
                DenseVector itemWeights = kernelSmoothing(scalar, itemSize, randomItemIndex, KernelSmoother.EPANECHNIKOV_KERNEL, 0.8F, true);
                DenseMatrix localUserFactors = DenseMatrix.valueOf(userSize, numberOfLocalFactors);
                localUserFactors.iterateElement(MathCalculator.SERIAL, (element) -> {
                    element.setValue(distribution.sample().floatValue());
                });
                DenseMatrix localItemFactors = DenseMatrix.valueOf(itemSize, numberOfLocalFactors);
                localItemFactors.iterateElement(MathCalculator.SERIAL, (element) -> {
                    element.setValue(distribution.sample().floatValue());
                });
                // Starting a new local model learning:
                learners[nextRunningSlot] = new LLORMALearner(modelCount, numberOfLocalFactors, localLearnRatio, localUserRegularization, localItemRegularization, localEpocheSize, localUserFactors, localItemFactors, userWeights, itemWeights, scoreMatrix);
                learners[nextRunningSlot].start();
                runningThreadList[runningThreadCount] = modelCount;
                runningThreadCount++;
                modelCount++;
                nextRunningSlot++;
            } else if (runningThreadCount > 0) {
                // Joining a local model which was done with learning:
                try {
                    learners[waitingThreadPointer].join();
                } catch (InterruptedException ie) {
                    logger.error("Join failed: " + ie);
                }
                LLORMALearner learner = learners[waitingThreadPointer];
                userMatrixes[learner.getIndex()] = learner.getUserFactors();
                itemMatrixes[learner.getIndex()] = learner.getItemFactors();
                nextRunningSlot = waitingThreadPointer;
                waitingThreadPointer = (waitingThreadPointer + 1) % numberOfThreads;
                runningThreadCount--;
                completeModelCount++;
            }
        }
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        practiceGlobalModel(scalar);
        practiceLocalModels(scalar);
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        float weightSum = 0F;
        float valueSum = 0F;
        for (int iterationStep = 0; iterationStep < numberOfModels; iterationStep++) {
            float weight = KernelSmoother.EPANECHNIKOV_KERNEL.kernelize(getUserSimilarity(scalar, anchorUsers[iterationStep], userIndex), 0.8F) * KernelSmoother.EPANECHNIKOV_KERNEL.kernelize(getItemSimilarity(scalar, anchorItems[iterationStep], itemIndex), 0.8F);
            float value = (scalar.dotProduct(userMatrixes[iterationStep].getRowVector(userIndex), itemMatrixes[iterationStep].getRowVector(itemIndex)).getValue()) * weight;
            weightSum += weight;
            valueSum += value;
        }
        float score = valueSum / weightSum;
        if (Float.isNaN(score) || score == 0F) {
            score = meanScore;
        }
        instance.setQuantityMark(score);
    }

}
