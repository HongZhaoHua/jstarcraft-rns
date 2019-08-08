package com.jstarcraft.rns.recommend.collaborative.ranking;

import java.util.Arrays;
import java.util.Comparator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.module.ArrayInstance;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.utility.LogisticUtility;
import com.jstarcraft.rns.utility.SampleUtility;

/**
 * 
 * Lambda FM推荐器
 * 
 * <pre>
 * LambdaFM: Learning Optimal Ranking with Factorization Machines Using Lambda Surrogates
 * </pre>
 * 
 * @author Birdy
 *
 */
public class LambdaFMDynamicRecommender extends LambdaFMRecommender {

    // Dynamic
    private float dynamicRho;

    private int numberOfOrders;

    private DenseVector orderProbabilities;

    private ArrayInstance[] negatives;

    private Integer[] orderIndexes;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        dynamicRho = configuration.getFloat("recommender.item.distribution.parameter");
        numberOfOrders = configuration.getInteger("recommender.number.orders", 10);

        DefaultScalar sum = DefaultScalar.getInstance();
        sum.setValue(0F);
        orderProbabilities = DenseVector.valueOf(numberOfOrders);
        orderProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            int index = scalar.getIndex();
            float value = (float) (Math.exp(-(index + 1) / (numberOfOrders * dynamicRho)));
            sum.shiftValue(value);
            scalar.setValue(sum.getValue());
        });
        negatives = new ArrayInstance[numberOfOrders];
        orderIndexes = new Integer[numberOfOrders];
        for (int index = 0; index < numberOfOrders; index++) {
            negatives[index] = new ArrayInstance(model.getQualityOrder(), model.getQuantityOrder());
            orderIndexes[index] = index;
        }
    }

    @Override
    protected float getGradientValue(DataModule[] modules, ArrayInstance positive, ArrayInstance negative, DefaultScalar scalar) {
        int userIndex;
        while (true) {
            userIndex = RandomUtility.randomInteger(userSize);
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            if (userVector.getElementSize() == 0 || userVector.getElementSize() == itemSize) {
                continue;
            }

            DataModule module = modules[userIndex];
            DataInstance instance = module.getInstance(0);
            int positivePosition = RandomUtility.randomInteger(module.getSize());
            instance.setCursor(positivePosition);
            positive.copyInstance(instance);
            // TODO negativeGroup.size()可能永远达不到numberOfNegatives,需要处理
            for (int orderIndex = 0; orderIndex < numberOfOrders; orderIndex++) {
                int negativeItemIndex = RandomUtility.randomInteger(itemSize - userVector.getElementSize());
                for (int position = 0, size = userVector.getElementSize(); position < size; position++) {
                    if (negativeItemIndex >= userVector.getIndex(position)) {
                        negativeItemIndex++;
                        continue;
                    }
                    break;
                }
                // TODO 注意,此处为了故意制造负面特征.
                int negativePosition = RandomUtility.randomInteger(module.getSize());
                instance.setCursor(negativePosition);
                negatives[orderIndex].copyInstance(instance);
                negatives[orderIndex].setQualityFeature(itemDimension, negativeItemIndex);
                MathVector vector = getFeatureVector(negatives[orderIndex]);
                negatives[orderIndex].setQuantityMark(predict(scalar, vector));
            }

            int orderIndex = SampleUtility.binarySearch(orderProbabilities, 0, orderProbabilities.getElementSize() - 1, RandomUtility.randomFloat(orderProbabilities.getValue(orderProbabilities.getElementSize() - 1)));
            Arrays.sort(orderIndexes, new Comparator<Integer>() {
                @Override
                public int compare(Integer leftIndex, Integer rightIndex) {
                    return (negatives[leftIndex].getQuantityMark() > negatives[rightIndex].getQuantityMark() ? -1 : (negatives[leftIndex].getQuantityMark() < negatives[rightIndex].getQuantityMark() ? 1 : 0));
                }
            });
            negative = negatives[orderIndexes[orderIndex]];
            break;
        }

        positiveVector = getFeatureVector(positive);
        negativeVector = getFeatureVector(negative);

        float positiveScore = predict(scalar, positiveVector);
        float negativeScore = predict(scalar, negativeVector);

        float error = positiveScore - negativeScore;

        // 由于pij_real默认为1,所以简化了loss的计算.
        // loss += -pij_real * Math.log(pij) - (1 - pij_real) *
        // Math.log(1 - pij);
        totalError += (float) -Math.log(LogisticUtility.getValue(error));
        float gradient = calaculateGradientValue(lossType, error);
        return gradient;
    }

}
