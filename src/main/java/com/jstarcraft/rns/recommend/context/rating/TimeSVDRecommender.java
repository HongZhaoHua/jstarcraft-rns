package com.jstarcraft.rns.recommend.context.rating;

import java.util.concurrent.TimeUnit;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.collaborative.rating.BiasedMFRecommender;
import com.jstarcraft.rns.recommend.exception.RecommendException;

/**
 * 
 * TimeSVD++推荐器
 * 
 * <pre>
 * Collaborative Filtering with Temporal Dynamics
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class TimeSVDRecommender extends BiasedMFRecommender {

    protected String instantField;

    protected int instantDimension;

    /**
     * the span of days of rating timestamps
     */
    private static int numDays;

    /**
     * {user, mean date}
     */
    private DenseVector userMeanDays;

    /**
     * time decay factor(时间衰退因子)
     */
    private float decay;

    /**
     * number of bins over all the items
     */
    private int numSections;

    /**
     * {user, appender} alpha matrix
     */
    private DenseMatrix userImplicitFactors;

    /**
     * item's implicit influence
     */
    private DenseMatrix itemImplicitFactors;

    private DenseMatrix userExplicitFactors;

    private DenseMatrix itemExplicitFactors;

    /**
     * {item, bin(t)} bias matrix
     */
    private DenseMatrix itemSectionBiases;

    /**
     * {user, day, bias} table
     */
    private Table<Integer, Integer, Float> userDayBiases;

    /**
     * user bias weight parameters
     */
    private DenseVector userBiasWeights;

    /**
     * {user, {appender, day, value} } map
     */
    private Table<Integer, Integer, float[]> userDayFactors;

    /**
     * {user, user scaling stable part}
     */
    private DenseVector userScales;

    /**
     * {user, day, day-specific scaling part}
     */
    private DenseMatrix userDayScales;

    /**
     * minimum, maximum timestamp
     */
    private static int minTimestamp, maxTimestamp;

    /**
     * matrix of time stamp
     */
    // TODO 既包含trainTerm,又包含testTerm
    private Table<Integer, Integer, Integer> instantTabel;

    /*
     * (non-Javadoc)
     *
     * @see net.librecommender.recommender.cf.rating.BiasedMFRecommender#setup()
     */
    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        decay = configuration.getFloat("recommender.learnrate.decay", 0.015F);
        numSections = configuration.getInteger("recommender.numBins", 6);

        instantField = configuration.getString("data.model.fields.instant");
        instantDimension = model.getQualityInner(instantField);

        instantTabel = HashBasedTable.create();
        for (DataInstance sample : model) {
            instantTabel.put(sample.getQualityFeature(userDimension), sample.getQualityFeature(itemDimension), sample.getQualityFeature(instantDimension));
        }
        getMaxAndMinTimeStamp();
        numDays = days(maxTimestamp, minTimestamp) + 1;
        // TODO 考虑重构
        userBiases = DenseVector.valueOf(userSize);
        userBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemBiases = DenseVector.valueOf(itemSize);
        itemBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        userBiasWeights = DenseVector.valueOf(userSize);
        userBiasWeights.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemSectionBiases = DenseMatrix.valueOf(itemSize, numSections);
        itemSectionBiases.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemImplicitFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemImplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        userImplicitFactors = DenseMatrix.valueOf(userSize, factorSize);
        userImplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        userDayBiases = HashBasedTable.create();
        userDayFactors = HashBasedTable.create();
        userScales = DenseVector.valueOf(userSize);
        userScales.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        userDayScales = DenseMatrix.valueOf(userSize, numDays);
        userDayScales.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        userExplicitFactors = DenseMatrix.valueOf(userSize, factorSize);
        userExplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemExplicitFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemExplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        // global average date
        float mean;
        float sum = 0F;
        int count = 0;
        for (MatrixScalar term : scoreMatrix) {
            int userIndex = term.getRow();
            int itemIndex = term.getColumn();
            sum += days(instantTabel.get(userIndex, itemIndex), minTimestamp);
            count++;
        }
        float globalMeanDays = sum / count;
        // compute user's mean of rating timestamps
        userMeanDays = DenseVector.valueOf(userSize);
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            sum = 0F;
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            for (VectorScalar term : userVector) {
                int itemIndex = term.getIndex();
                sum += days(instantTabel.get(userIndex, itemIndex), minTimestamp);
            }
            mean = (userVector.getElementSize() > 0) ? (sum + 0F) / userVector.getElementSize() : globalMeanDays;
            userMeanDays.setValue(userIndex, mean);
        }
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                SparseVector rateVector = scoreMatrix.getRowVector(userIndex);
                int size = rateVector.getElementSize();
                if (size == 0) {
                    continue;
                }
                for (VectorScalar term : rateVector) {
                    int itemExplicitIndex = term.getIndex();
                    float rate = term.getValue();
                    // TODO 此处可以重构.
                    int instant = instantTabel.get(userIndex, itemExplicitIndex);
                    // day t
                    int days = days(instant, minTimestamp);
                    int section = section(days);
                    float deviation = deviation(userIndex, days);
                    float userBias = userBiases.getValue(userIndex);
                    float itemBias = itemBiases.getValue(itemExplicitIndex);

                    float userScale = userScales.getValue(userIndex);
                    float dayScale = userDayScales.getValue(userIndex, days);
                    // lazy initialization
                    // TODO 此处可以重构.
                    if (!userDayBiases.contains(userIndex, days)) {
                        userDayBiases.put(userIndex, days, RandomUtility.randomFloat(1F));
                    }
                    float userDayBias = userDayBiases.get(userIndex, days);
                    float itemSectionBias = itemSectionBiases.getValue(itemExplicitIndex, section);
                    // alpha_u
                    float userWeight = userBiasWeights.getValue(userIndex);
                    // mu bi(t)
                    float predict = meanScore + (itemBias + itemSectionBias) * (userScale + dayScale);
                    // bu(t)
                    predict += userBias + userWeight * deviation + userDayBias;
                    // qi * yj
                    DenseVector itemExplicitVector = itemExplicitFactors.getRowVector(itemExplicitIndex);
                    float sum = 0F;
                    for (VectorScalar rateTerm : rateVector) {
                        int itemImplicitIndex = rateTerm.getIndex();
                        DenseVector itemImpilcitVector = itemImplicitFactors.getRowVector(itemImplicitIndex);
                        sum += scalar.dotProduct(itemImpilcitVector, itemExplicitVector).getValue();
                    }
                    float itemWeight = (float) (size > 0 ? Math.pow(size, -0.5F) : 0F);
                    predict += sum * itemWeight;
                    // qi * pu(t)
                    float[] dayFactors = userDayFactors.get(userIndex, days);
                    if (dayFactors == null) {
                        dayFactors = new float[factorSize];
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            dayFactors[factorIndex] = RandomUtility.randomFloat(1F);
                        }
                        userDayFactors.put(userIndex, days, dayFactors);
                    }
                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        float qik = itemExplicitFactors.getValue(itemExplicitIndex, factorIndex);
                        float puk = userExplicitFactors.getValue(userIndex, factorIndex) + userImplicitFactors.getValue(userIndex, factorIndex) * deviation + dayFactors[factorIndex];
                        predict += puk * qik;
                    }

                    float error = predict - rate;
                    totalError += error * error;

                    // update bi
                    float sgd = error * (userScale + dayScale) + regBias * itemBias;
                    itemBiases.shiftValue(itemExplicitIndex, -learnRatio * sgd);
                    totalError += regBias * itemBias * itemBias;

                    // update bi,bin(t)
                    sgd = error * (userScale + dayScale) + regBias * itemSectionBias;
                    itemSectionBiases.shiftValue(itemExplicitIndex, section, -learnRatio * sgd);
                    totalError += regBias * itemSectionBias * itemSectionBias;

                    // update cu
                    sgd = error * (itemBias + itemSectionBias) + regBias * userScale;
                    userScales.shiftValue(userIndex, -learnRatio * sgd);
                    totalError += regBias * userScale * userScale;

                    // update cut
                    sgd = error * (itemBias + itemSectionBias) + regBias * dayScale;
                    userDayScales.shiftValue(userIndex, days, -learnRatio * sgd);
                    totalError += regBias * dayScale * dayScale;

                    // update bu
                    sgd = error + regBias * userBias;
                    userBiases.shiftValue(userIndex, -learnRatio * sgd);
                    totalError += regBias * userBias * userBias;

                    // update au
                    sgd = error * deviation + regBias * userWeight;
                    userBiasWeights.shiftValue(userIndex, -learnRatio * sgd);
                    totalError += regBias * userWeight * userWeight;

                    // update but
                    sgd = error + regBias * userDayBias;
                    float delta = userDayBias - learnRatio * sgd;
                    userDayBiases.put(userIndex, days, delta);
                    totalError += regBias * userDayBias * userDayBias;

                    for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                        float userExplicitFactor = userExplicitFactors.getValue(userIndex, factorIndex);
                        float itemExplicitFactor = itemExplicitFactors.getValue(itemExplicitIndex, factorIndex);
                        float userImplicitFactor = userImplicitFactors.getValue(userIndex, factorIndex);
                        delta = dayFactors[factorIndex];

                        // TODO 此处可以整合操作
                        sum = 0F;
                        // update userExplicitFactor
                        sgd = error * itemExplicitFactor + userRegularization * userExplicitFactor;
                        userExplicitFactors.shiftValue(userIndex, factorIndex, -learnRatio * sgd);
                        totalError += userRegularization * userExplicitFactor * userExplicitFactor;

                        // update itemExplicitFactors
                        for (VectorScalar rateTerm : rateVector) {
                            int itemImplicitIndex = rateTerm.getIndex();
                            sum += itemImplicitFactors.getValue(itemImplicitIndex, factorIndex);
                        }
                        sgd = error * (userExplicitFactor + userImplicitFactor * deviation + delta + itemWeight * sum) + itemRegularization * itemExplicitFactor;
                        itemExplicitFactors.shiftValue(itemExplicitIndex, factorIndex, -learnRatio * sgd);
                        totalError += itemRegularization * itemExplicitFactor * itemExplicitFactor;

                        // update userImplicitFactors
                        sgd = error * itemExplicitFactor * deviation + userRegularization * userImplicitFactor;
                        userImplicitFactors.shiftValue(userIndex, factorIndex, -learnRatio * sgd);
                        totalError += userRegularization * userImplicitFactor * userImplicitFactor;

                        // update itemImplicitFactors
                        // TODO 此处可以整合操作
                        for (VectorScalar rateTerm : rateVector) {
                            int itemImplicitIndex = rateTerm.getIndex();
                            float itemImplicitFactor = itemImplicitFactors.getValue(itemImplicitIndex, factorIndex);
                            sgd = error * itemWeight * itemExplicitFactor + itemRegularization * itemImplicitFactor;
                            itemImplicitFactors.shiftValue(itemImplicitIndex, factorIndex, -learnRatio * sgd);
                            totalError += itemRegularization * itemImplicitFactor * itemImplicitFactor;
                        }

                        // update pkt
                        sgd = error * itemExplicitFactor + userRegularization * delta;
                        totalError += userRegularization * delta * delta;
                        delta = delta - learnRatio * sgd;
                        dayFactors[factorIndex] = delta;
                    }

                }
            }

            totalError *= 0.5D;
            if (isConverged(epocheIndex)) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

    /**
     * predict a specific rating for user userIdx on item itemIdx.
     *
     * @param userIndex user index
     * @param itemIndex item index
     * @return predictive rating for user userIdx on item itemIdx
     * @throws RecommendException if error occurs
     */
    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        // retrieve the test rating timestamp
        int instant = instance.getQualityFeature(instantDimension);
        int days = days(instant, minTimestamp);
        int section = section(days);
        float deviation = deviation(userIndex, days);
        float value = meanScore;

        // bi(t): eq. (12)
        value += (itemBiases.getValue(itemIndex) + itemSectionBiases.getValue(itemIndex, section)) * (userScales.getValue(userIndex) + userDayScales.getValue(userIndex, days));
        // bu(t): eq. (9)
        value += (userBiases.getValue(userIndex) + userBiasWeights.getValue(userIndex) * deviation + (userDayBiases.contains(userIndex, days) ? userDayBiases.get(userIndex, days) : 0D));

        // qi * yj
        SparseVector userVector = scoreMatrix.getRowVector(userIndex);

        float sum = 0F;
        DenseVector itemExplicitVector = itemExplicitFactors.getRowVector(itemIndex);
        for (VectorScalar term : userVector) {
            DenseVector itemImplicitVector = itemImplicitFactors.getRowVector(term.getIndex());
            sum += scalar.dotProduct(itemImplicitVector, itemExplicitVector).getValue();
        }
        float weight = (float) (userVector.getElementSize() > 0 ? Math.pow(userVector.getElementSize(), -0.5F) : 0F);
        value += sum * weight;

        // qi * pu(t)
        float[] dayFactors = userDayFactors.get(userIndex, days);
        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
            float itemExplicitFactor = itemExplicitFactors.getValue(itemIndex, factorIndex);
            // eq. (13)
            float userExplicitFactor = userExplicitFactors.getValue(userIndex, factorIndex) + userImplicitFactors.getValue(userIndex, factorIndex) * deviation;
            userExplicitFactor += (dayFactors == null ? 0F : dayFactors[factorIndex]);
            value += userExplicitFactor * itemExplicitFactor;
        }
        if (value > maximumScore) {
            value = maximumScore;
        } else if (value < minimumScore) {
            value = minimumScore;
        }
        instance.setQuantityMark(value);
    }

    /**
     * get the time deviation for a specific timestamp
     *
     * @param userIndex the inner id of a user
     * @param days      the time stamp
     * @return the time deviation for a specific timestamp t w.r.t the mean date tu
     */
    private float deviation(int userIndex, int days) {
        float mean = userMeanDays.getValue(userIndex);
        // date difference in days
        float deviation = days - mean;
        return (float) (Math.signum(deviation) * Math.pow(Math.abs(deviation), decay));
    }

    /**
     * get the bin number for a specific time stamp
     *
     * @param days time stamp of a day
     * @return the bin number (starting from 0..numBins-1) for a specific timestamp
     *         t;
     */
    // 将时间戳分段
    private int section(int days) {
        return (int) (days / (numDays + 0D) * numSections);
    }

    /**
     * get the number of days for a given time difference
     *
     * @param duration the difference between two time stamps
     * @return number of days for a given time difference
     */
    private static int days(long duration) {
        return (int) TimeUnit.MILLISECONDS.toDays(duration);
    }

    /**
     * get the number of days between two timestamps
     *
     * @param t1 time stamp 1
     * @param t2 time stamp 2
     * @return number of days between two timestamps
     */
    private static int days(int t1, int t2) {
        return days(Math.abs(t1 - t2));
    }

    /**
     * get the maximum and minimum time stamps in the time matrix
     *
     */
    private void getMaxAndMinTimeStamp() {
        minTimestamp = Integer.MAX_VALUE;
        maxTimestamp = Integer.MIN_VALUE;

        for (Cell<Integer, Integer, Integer> cell : instantTabel.cellSet()) {
            int timeStamp = cell.getValue();
            if (timeStamp < minTimestamp) {
                minTimestamp = timeStamp;
            }

            if (timeStamp > maxTimestamp) {
                maxTimestamp = timeStamp;
            }
        }
    }

}
