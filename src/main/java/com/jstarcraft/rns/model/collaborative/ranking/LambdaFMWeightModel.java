package com.jstarcraft.rns.model.collaborative.ranking;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.module.ArrayInstance;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.utility.LogisticUtility;

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
public class LambdaFMWeightModel extends LambdaFMModel {

    // Weight
    private float[] orderLosses;
    private float epsilon;
    private int Y, N;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        epsilon = configuration.getFloat("epsilon");
        orderLosses = new float[itemSize - 1];
        float orderLoss = 0F;
        for (int orderIndex = 1; orderIndex < itemSize; orderIndex++) {
            orderLoss += 1F / orderIndex;
            orderLosses[orderIndex - 1] = orderLoss;
        }
        for (int rankIndex = 1; rankIndex < itemSize; rankIndex++) {
            orderLosses[rankIndex - 1] /= orderLoss;
        }
    }

    @Override
    protected float getGradientValue(DataModule[] modules, ArrayInstance positive, ArrayInstance negative, DefaultScalar scalar) {
        int userIndex;
        float positiveScore;
        float negativeScore;
        while (true) {
            userIndex = RandomUtility.randomInteger(userSize);
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            if (userVector.getElementSize() == 0 || userVector.getElementSize() == itemSize) {
                continue;
            }

            N = 0;
            Y = itemSize - scoreMatrix.getRowScope(userIndex);
            DataModule module = modules[userIndex];
            DataInstance instance = module.getInstance(0);
            int positivePosition = RandomUtility.randomInteger(module.getSize());
            instance.setCursor(positivePosition);
            positive.copyInstance(instance);
            positiveVector = getFeatureVector(positive);
            positiveScore = predict(scalar, positiveVector);
            do {
                N++;
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
                // TODO 注意,此处为了故意制造负面特征.
                instance.setCursor(negativePosition);
                negative.copyInstance(instance);
                negative.setQualityFeature(itemDimension, negativeItemIndex);
                negativeVector = getFeatureVector(negative);
                negativeScore = predict(scalar, negativeVector);
            } while ((positiveScore - negativeScore > epsilon) && N < Y - 1);
            break;
        }

        float error = positiveScore - negativeScore;

        // 由于pij_real默认为1,所以简化了loss的计算.
        // loss += -pij_real * Math.log(pij) - (1 - pij_real) *
        // Math.log(1 - pij);
        totalError += (float) -Math.log(LogisticUtility.getValue(error));
        float gradient = calaculateGradientValue(lossType, error);
        int orderIndex = (int) ((Y - 1) / N);
        float orderLoss = orderLosses[orderIndex];
        gradient = gradient * orderLoss;
        return gradient;
    }

}
