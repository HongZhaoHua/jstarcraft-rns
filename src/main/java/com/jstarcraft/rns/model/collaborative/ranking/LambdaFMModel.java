package com.jstarcraft.rns.model.collaborative.ranking;

import java.util.Iterator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.module.ArrayInstance;
import com.jstarcraft.ai.data.processor.DataSplitter;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.rns.data.processor.QualityFeatureDataSplitter;
import com.jstarcraft.rns.model.FactorizationMachineModel;

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
public abstract class LambdaFMModel extends FactorizationMachineModel {

    protected int lossType;

    protected MathVector positiveVector, negativeVector;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // TODO 此处代码可以消除(使用常量Marker代替或者使用binarize.threshold)
        for (MatrixScalar term : scoreMatrix) {
            term.setValue(1F);
        }

        lossType = configuration.getInteger("losstype", 3);

        biasRegularization = configuration.getFloat("recommender.fm.regw0", 0.1F);
        weightRegularization = configuration.getFloat("recommender.fm.regW", 0.1F);
        factorRegularization = configuration.getFloat("recommender.fm.regF", 0.001F);
    }

    protected abstract float getGradientValue(DataModule[] modules, ArrayInstance positive, ArrayInstance negative, DefaultScalar scalar);

    @Override
    protected void doPractice() {
        ArrayInstance positive = new ArrayInstance(marker.getQualityOrder(), marker.getQuantityOrder());
        ArrayInstance negative = new ArrayInstance(marker.getQualityOrder(), marker.getQuantityOrder());

        DefaultScalar scalar = DefaultScalar.getInstance();

        DataSplitter splitter = new QualityFeatureDataSplitter(userDimension);
        DataModule[] modules = splitter.split(marker, userSize);

        DenseVector positiveSum = DenseVector.valueOf(factorSize);
        DenseVector negativeSum = DenseVector.valueOf(factorSize);

        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            long totalTime = 0;
            totalError = 0F;
            for (int sampleIndex = 0, sampleTimes = userSize * 50; sampleIndex < sampleTimes; sampleIndex++) {
                long current = System.currentTimeMillis();
                float gradient = getGradientValue(modules, positive, negative, scalar);
                totalTime += (System.currentTimeMillis() - current);

                sum(positiveVector, positiveSum);
                sum(negativeVector, negativeSum);
                int leftIndex = 0, rightIndex = 0;
                Iterator<VectorScalar> leftIterator = positiveVector.iterator();
                Iterator<VectorScalar> rightIterator = negativeVector.iterator();
                for (int index = 0; index < marker.getQualityOrder(); index++) {
                    VectorScalar leftTerm = leftIterator.next();
                    VectorScalar rightTerm = rightIterator.next();
                    leftIndex = leftTerm.getIndex();
                    rightIndex = rightTerm.getIndex();
                    if (leftIndex == rightIndex) {
                        weightVector.shiftValue(leftIndex, learnRatio * (gradient * 0F - weightRegularization * weightVector.getValue(leftIndex)));
                        totalError += weightRegularization * weightVector.getValue(leftIndex) * weightVector.getValue(leftIndex);

                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            float positiveFactor = positiveSum.getValue(factorIndex) * leftTerm.getValue() - featureFactors.getValue(leftIndex, factorIndex) * leftTerm.getValue() * leftTerm.getValue();
                            float negativeFactor = negativeSum.getValue(factorIndex) * rightTerm.getValue() - featureFactors.getValue(rightIndex, factorIndex) * rightTerm.getValue() * rightTerm.getValue();

                            featureFactors.shiftValue(leftIndex, factorIndex, learnRatio * (gradient * (positiveFactor - negativeFactor) - factorRegularization * featureFactors.getValue(leftIndex, factorIndex)));
                            totalError += factorRegularization * featureFactors.getValue(leftIndex, factorIndex) * featureFactors.getValue(leftIndex, factorIndex);
                        }
                    } else {
                        weightVector.shiftValue(leftIndex, learnRatio * (gradient * leftTerm.getValue() - weightRegularization * weightVector.getValue(leftIndex)));
                        totalError += weightRegularization * weightVector.getValue(leftIndex) * weightVector.getValue(leftIndex);
                        weightVector.shiftValue(rightIndex, learnRatio * (gradient * -rightTerm.getValue() - weightRegularization * weightVector.getValue(rightIndex)));
                        totalError += weightRegularization * weightVector.getValue(rightIndex) * weightVector.getValue(rightIndex);

                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            float positiveFactor = positiveSum.getValue(factorIndex) * leftTerm.getValue() - featureFactors.getValue(leftIndex, factorIndex) * leftTerm.getValue() * leftTerm.getValue();
                            featureFactors.shiftValue(leftIndex, factorIndex, learnRatio * (gradient * positiveFactor - factorRegularization * featureFactors.getValue(leftIndex, factorIndex)));
                            totalError += factorRegularization * featureFactors.getValue(leftIndex, factorIndex) * featureFactors.getValue(leftIndex, factorIndex);

                            float negativeFactor = negativeSum.getValue(factorIndex) * rightTerm.getValue() - featureFactors.getValue(rightIndex, factorIndex) * rightTerm.getValue() * rightTerm.getValue();
                            featureFactors.shiftValue(rightIndex, factorIndex, learnRatio * (gradient * -negativeFactor - factorRegularization * featureFactors.getValue(rightIndex, factorIndex)));
                            totalError += factorRegularization * featureFactors.getValue(rightIndex, factorIndex) * featureFactors.getValue(rightIndex, factorIndex);
                        }
                    }
                }
            }
            System.out.println(totalTime);

            totalError *= 0.5;
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

    protected void isLearned(int iteration) {
        if (learnRatio < 0F) {
            return;
        }
        if (isLearned && iteration > 1) {
            learnRatio = Math.abs(currentError) > Math.abs(totalError) ? learnRatio * 1.05F : learnRatio * 0.5F;
        } else if (learnDecay > 0 && learnDecay < 1) {
            learnRatio *= learnDecay;
        }
        // limit to max-learn-rate after update
        if (learnLimit > 0 && learnRatio > learnLimit) {
            learnRatio = learnLimit;
        }
    }

    private void sum(MathVector vector, DenseVector sum) {
        // TODO 考虑调整为向量操作.
        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
            float value = 0F;
            for (VectorScalar term : vector) {
                value += featureFactors.getValue(term.getIndex(), factorIndex) * term.getValue();
            }
            sum.setValue(factorIndex, value);
        }
    }

}
