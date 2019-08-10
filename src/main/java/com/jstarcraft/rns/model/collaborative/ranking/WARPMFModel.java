package com.jstarcraft.rns.model.collaborative.ranking;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.MatrixFactorizationModel;
import com.jstarcraft.rns.utility.LogisticUtility;

/**
 * 
 * WARP推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class WARPMFModel extends MatrixFactorizationModel {

    private int lossType;

    private float epsilon;

    private float[] orderLosses;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        lossType = configuration.getInteger("losstype", 3);
        epsilon = configuration.getFloat("epsilon");
        orderLosses = new float[itemSize - 1];
        float orderLoss = 0F;
        for (int orderIndex = 1; orderIndex < itemSize; orderIndex++) {
            orderLoss += 1D / orderIndex;
            orderLosses[orderIndex - 1] = orderLoss;
        }
        for (int rankIndex = 1; rankIndex < itemSize; rankIndex++) {
            orderLosses[rankIndex - 1] /= orderLoss;
        }
    }

    @Override
    protected void doPractice() {
        int Y, N;

        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            for (int sampleIndex = 0, sampleTimes = userSize * 100; sampleIndex < sampleTimes; sampleIndex++) {
                int userIndex, positiveItemIndex, negativeItemIndex;
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
                    positiveItemIndex = userVector.getIndex(RandomUtility.randomInteger(userVector.getElementSize()));
                    positiveScore = predict(userIndex, positiveItemIndex);
                    do {
                        N++;
                        negativeItemIndex = RandomUtility.randomInteger(itemSize - userVector.getElementSize());
                        for (int index = 0, size = userVector.getElementSize(); index < size; index++) {
                            if (negativeItemIndex >= userVector.getIndex(index)) {
                                negativeItemIndex++;
                                continue;
                            }
                            break;
                        }
                        negativeScore = predict(userIndex, negativeItemIndex);
                    } while ((positiveScore - negativeScore > epsilon) && N < Y - 1);
                    break;
                }
                // update parameters
                float error = positiveScore - negativeScore;

                float gradient = calaculateGradientValue(lossType, error);
                int orderIndex = (int) ((Y - 1) / N);
                float orderLoss = orderLosses[orderIndex];
                gradient = gradient * orderLoss;

                totalError += -Math.log(LogisticUtility.getValue(error));

                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    float userFactor = userFactors.getValue(userIndex, factorIndex);
                    float positiveFactor = itemFactors.getValue(positiveItemIndex, factorIndex);
                    float negativeFactor = itemFactors.getValue(negativeItemIndex, factorIndex);

                    userFactors.shiftValue(userIndex, factorIndex, learnRatio * (gradient * (positiveFactor - negativeFactor) - userRegularization * userFactor));
                    itemFactors.shiftValue(positiveItemIndex, factorIndex, learnRatio * (gradient * userFactor - itemRegularization * positiveFactor));
                    itemFactors.shiftValue(negativeItemIndex, factorIndex, learnRatio * (gradient * (-userFactor) - itemRegularization * negativeFactor));
                    totalError += userRegularization * userFactor * userFactor + itemRegularization * positiveFactor * positiveFactor + itemRegularization * negativeFactor * negativeFactor;
                }
            }

            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

}
