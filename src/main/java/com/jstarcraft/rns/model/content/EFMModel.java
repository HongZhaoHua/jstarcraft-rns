package com.jstarcraft.rns.model.content;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.attribute.MemoryQualityAttribute;
import com.jstarcraft.ai.math.MathUtility;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.model.MatrixFactorizationModel;

import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * 
 * EFM推荐器
 * 
 * <pre>
 * Explicit factor models for explainable recommendation based on phrase-level sentiment analysis
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class EFMModel extends MatrixFactorizationModel {

    protected String commentField;
    protected int commentDimension;
    protected int numberOfFeatures;
    protected int numberOfExplicitFeatures;
    protected int numberOfImplicitFeatures;
    protected float scoreScale;
    protected DenseMatrix featureFactors;
    protected DenseMatrix userExplicitFactors;
    protected DenseMatrix userImplicitFactors;
    protected DenseMatrix itemExplicitFactors;
    protected DenseMatrix itemImplicitFactors;
    protected SparseMatrix userFeatures;
    protected SparseMatrix itemFeatures;
    protected float attentionRegularization;
    protected float qualityRegularization;
    protected float explicitRegularization;
    protected float implicitRegularization;
    protected float featureRegularization;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        commentField = configuration.getString("data.model.fields.comment");
        commentDimension = model.getQualityInner(commentField);
        MemoryQualityAttribute attribute = (MemoryQualityAttribute) space.getQualityAttribute(commentField);
        Object[] wordValues = attribute.getDatas();

        scoreScale = maximumScore - minimumScore;
        numberOfExplicitFeatures = configuration.getInteger("recommender.factor.explicit", 5);
        numberOfImplicitFeatures = factorSize - numberOfExplicitFeatures;
        attentionRegularization = configuration.getFloat("recommender.regularization.lambdax", 0.001F);
        qualityRegularization = configuration.getFloat("recommender.regularization.lambday", 0.001F);
        explicitRegularization = configuration.getFloat("recommender.regularization.lambdau", 0.001F);
        implicitRegularization = configuration.getFloat("recommender.regularization.lambdah", 0.001F);
        featureRegularization = configuration.getFloat("recommender.regularization.lambdav", 0.001F);

        Map<String, Integer> featureDictionaries = new HashMap<>();
        Map<Integer, StringBuilder> userDictionaries = new HashMap<>();
        Map<Integer, StringBuilder> itemDictionaries = new HashMap<>();

        numberOfFeatures = 0;
        // // TODO 此处保证所有特征都会被识别
        // for (Object value : wordValues) {
        // String wordValue = (String) value;
        // String[] words = wordValue.split(" ");
        // for (String word : words) {
        // // TODO 此处似乎是Bug,不应该再将word划分为更细粒度.
        // String feature = word.split(":")[0];
        // if (!featureDictionaries.containsKey(feature) &&
        // StringUtils.isNotEmpty(feature)) {
        // featureDictionaries.put(feature, numberOfWords);
        // numberOfWords++;
        // }
        // }
        // }

        for (DataInstance sample : model) {
            int userIndex = sample.getQualityFeature(userDimension);
            int itemIndex = sample.getQualityFeature(itemDimension);
            int wordIndex = sample.getQualityFeature(commentDimension);
            String wordValue = (String) wordValues[wordIndex];
            String[] words = wordValue.split(" ");
            StringBuilder buffer;
            for (String word : words) {
                // TODO 此处似乎是Bug,不应该再将word划分为更细粒度.
                String feature = word.split(":")[0];
                if (!featureDictionaries.containsKey(feature) && !StringUtility.isEmpty(feature)) {
                    featureDictionaries.put(feature, numberOfFeatures++);
                }
                buffer = userDictionaries.get(userIndex);
                if (buffer != null) {
                    buffer.append(" ").append(word);
                } else {
                    userDictionaries.put(userIndex, new StringBuilder(word));
                }
                buffer = itemDictionaries.get(itemIndex);
                if (buffer != null) {
                    buffer.append(" ").append(word);
                } else {
                    itemDictionaries.put(itemIndex, new StringBuilder(word));
                }
            }
        }

        // Create V,U1,H1,U2,H2
        featureFactors = DenseMatrix.valueOf(numberOfFeatures, numberOfExplicitFeatures);
        featureFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(0.01F));
        });
        userExplicitFactors = DenseMatrix.valueOf(userSize, numberOfExplicitFeatures);
        userExplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        userImplicitFactors = DenseMatrix.valueOf(userSize, numberOfImplicitFeatures);
        userImplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemExplicitFactors = DenseMatrix.valueOf(itemSize, numberOfExplicitFeatures);
        itemExplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });
        itemImplicitFactors = DenseMatrix.valueOf(itemSize, numberOfImplicitFeatures);
        itemImplicitFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(RandomUtility.randomFloat(1F));
        });

        float[] featureValues = new float[numberOfFeatures];

        // compute UserFeatureAttention
        HashMatrix userTable = new HashMatrix(true, userSize, numberOfFeatures, new Long2FloatRBTreeMap());
        for (Entry<Integer, StringBuilder> term : userDictionaries.entrySet()) {
            int userIndex = term.getKey();
            String[] words = term.getValue().toString().split(" ");
            for (String word : words) {
                if (!StringUtility.isEmpty(word)) {
                    int featureIndex = featureDictionaries.get(word.split(":")[0]);
                    featureValues[featureIndex] += 1F;
                }
            }
            for (int featureIndex = 0; featureIndex < numberOfFeatures; featureIndex++) {
                if (featureValues[featureIndex] != 0F) {
                    float value = (float) (1F + (scoreScale - 1F) * (2F / (1F + Math.exp(-featureValues[featureIndex])) - 1F));
                    userTable.setValue(userIndex, featureIndex, value);
                    featureValues[featureIndex] = 0F;
                }
            }
        }
        userFeatures = SparseMatrix.valueOf(userSize, numberOfFeatures, userTable);
        // compute ItemFeatureQuality
        HashMatrix itemTable = new HashMatrix(true, itemSize, numberOfFeatures, new Long2FloatRBTreeMap());
        for (Entry<Integer, StringBuilder> term : itemDictionaries.entrySet()) {
            int itemIndex = term.getKey();
            String[] words = term.getValue().toString().split(" ");
            for (String word : words) {
                if (!StringUtility.isEmpty(word)) {
                    int featureIndex = featureDictionaries.get(word.split(":")[0]);
                    featureValues[featureIndex] += Double.parseDouble(word.split(":")[1]);
                }
            }
            for (int featureIndex = 0; featureIndex < numberOfFeatures; featureIndex++) {
                if (featureValues[featureIndex] != 0F) {
                    float value = (float) (1F + (scoreScale - 1F) / (1F + Math.exp(-featureValues[featureIndex])));
                    itemTable.setValue(itemIndex, featureIndex, value);
                    featureValues[featureIndex] = 0F;
                }
            }
        }
        itemFeatures = SparseMatrix.valueOf(itemSize, numberOfFeatures, itemTable);

        logger.info("numUsers:" + userSize);
        logger.info("numItems:" + itemSize);
        logger.info("numFeatures:" + numberOfFeatures);
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            totalError = 0F;
            for (int featureIndex = 0; featureIndex < numberOfFeatures; featureIndex++) {
                if (userFeatures.getColumnScope(featureIndex) > 0 && itemFeatures.getColumnScope(featureIndex) > 0) {
                    SparseVector userVector = userFeatures.getColumnVector(featureIndex);
                    SparseVector itemVector = itemFeatures.getColumnVector(featureIndex);
                    // TODO 此处需要重构,应该避免不断构建SparseVector.
                    int feature = featureIndex;
                    ArrayVector userFactors = new ArrayVector(userVector);
                    userFactors.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predictUserFactor(scalar, element.getIndex(), feature));
                    });
                    ArrayVector itemFactors = new ArrayVector(itemVector);
                    itemFactors.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predictItemFactor(scalar, element.getIndex(), feature));
                    });
                    for (int factorIndex = 0; factorIndex < numberOfExplicitFeatures; factorIndex++) {
                        DenseVector factorUsersVector = userExplicitFactors.getColumnVector(factorIndex);
                        DenseVector factorItemsVector = itemExplicitFactors.getColumnVector(factorIndex);
                        float numerator = attentionRegularization * scalar.dotProduct(factorUsersVector, userVector).getValue() + qualityRegularization * scalar.dotProduct(factorItemsVector, itemVector).getValue();
                        float denominator = attentionRegularization * scalar.dotProduct(factorUsersVector, userFactors).getValue() + qualityRegularization * scalar.dotProduct(factorItemsVector, itemFactors).getValue() + featureRegularization * featureFactors.getValue(featureIndex, factorIndex) + MathUtility.EPSILON;
                        featureFactors.setValue(featureIndex, factorIndex, (float) (featureFactors.getValue(featureIndex, factorIndex) * Math.sqrt(numerator / denominator)));
                    }
                }
            }

            // Update UserFeatureMatrix by fixing the others
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                if (scoreMatrix.getRowScope(userIndex) > 0 && userFeatures.getRowScope(userIndex) > 0) {
                    SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                    SparseVector attentionVector = userFeatures.getRowVector(userIndex);
                    // TODO 此处需要重构,应该避免不断构建SparseVector.
                    int user = userIndex;
                    ArrayVector itemPredictsVector = new ArrayVector(userVector);
                    itemPredictsVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predict(user, element.getIndex()));
                    });
                    ArrayVector attentionPredVector = new ArrayVector(attentionVector);
                    attentionPredVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predictUserFactor(scalar, user, element.getIndex()));
                    });
                    for (int factorIndex = 0; factorIndex < numberOfExplicitFeatures; factorIndex++) {
                        DenseVector factorItemsVector = itemExplicitFactors.getColumnVector(factorIndex);
                        DenseVector featureVector = featureFactors.getColumnVector(factorIndex);
                        float numerator = scalar.dotProduct(factorItemsVector, userVector).getValue() + attentionRegularization * scalar.dotProduct(featureVector, attentionVector).getValue();
                        float denominator = scalar.dotProduct(factorItemsVector, itemPredictsVector).getValue() + attentionRegularization * scalar.dotProduct(featureVector, attentionPredVector).getValue() + explicitRegularization * userExplicitFactors.getValue(userIndex, factorIndex) + MathUtility.EPSILON;
                        userExplicitFactors.setValue(userIndex, factorIndex, (float) (userExplicitFactors.getValue(userIndex, factorIndex) * Math.sqrt(numerator / denominator)));
                    }
                }
            }

            // Update ItemFeatureMatrix by fixing the others
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                if (scoreMatrix.getColumnScope(itemIndex) > 0 && itemFeatures.getRowScope(itemIndex) > 0) {
                    SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
                    SparseVector qualityVector = itemFeatures.getRowVector(itemIndex);
                    // TODO 此处需要重构,应该避免不断构建SparseVector.
                    int item = itemIndex;
                    ArrayVector userPredictsVector = new ArrayVector(itemVector);
                    userPredictsVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predict(element.getIndex(), item));
                    });
                    ArrayVector qualityPredVector = new ArrayVector(qualityVector);
                    qualityPredVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predictItemFactor(scalar, item, element.getIndex()));
                    });
                    for (int factorIndex = 0; factorIndex < numberOfExplicitFeatures; factorIndex++) {
                        DenseVector factorUsersVector = userExplicitFactors.getColumnVector(factorIndex);
                        DenseVector featureVector = featureFactors.getColumnVector(factorIndex);
                        float numerator = scalar.dotProduct(factorUsersVector, itemVector).getValue() + qualityRegularization * scalar.dotProduct(featureVector, qualityVector).getValue();
                        float denominator = scalar.dotProduct(factorUsersVector, userPredictsVector).getValue() + qualityRegularization * scalar.dotProduct(featureVector, qualityPredVector).getValue() + explicitRegularization * itemExplicitFactors.getValue(itemIndex, factorIndex) + MathUtility.EPSILON;
                        itemExplicitFactors.setValue(itemIndex, factorIndex, (float) (itemExplicitFactors.getValue(itemIndex, factorIndex) * Math.sqrt(numerator / denominator)));
                    }
                }
            }

            // Update UserHiddenMatrix by fixing the others
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                if (scoreMatrix.getRowScope(userIndex) > 0) {
                    SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                    // TODO 此处需要重构,应该避免不断构建SparseVector.
                    int user = userIndex;
                    ArrayVector itemPredictsVector = new ArrayVector(userVector);
                    itemPredictsVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predict(user, element.getIndex()));
                    });
                    for (int factorIndex = 0; factorIndex < numberOfImplicitFeatures; factorIndex++) {
                        DenseVector hiddenItemsVector = itemImplicitFactors.getColumnVector(factorIndex);
                        float numerator = scalar.dotProduct(hiddenItemsVector, userVector).getValue();
                        float denominator = scalar.dotProduct(hiddenItemsVector, itemPredictsVector).getValue() + implicitRegularization * userImplicitFactors.getValue(userIndex, factorIndex) + MathUtility.EPSILON;
                        userImplicitFactors.setValue(userIndex, factorIndex, (float) (userImplicitFactors.getValue(userIndex, factorIndex) * Math.sqrt(numerator / denominator)));
                    }
                }
            }

            // Update ItemHiddenMatrix by fixing the others
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                if (scoreMatrix.getColumnScope(itemIndex) > 0) {
                    SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
                    // TODO 此处需要重构,应该避免不断构建SparseVector.
                    int item = itemIndex;
                    ArrayVector userPredictsVector = new ArrayVector(itemVector);
                    userPredictsVector.iterateElement(MathCalculator.SERIAL, (element) -> {
                        element.setValue(predict(element.getIndex(), item));
                    });
                    for (int factorIndex = 0; factorIndex < numberOfImplicitFeatures; factorIndex++) {
                        DenseVector hiddenUsersVector = userImplicitFactors.getColumnVector(factorIndex);
                        float numerator = scalar.dotProduct(hiddenUsersVector, itemVector).getValue();
                        float denominator = scalar.dotProduct(hiddenUsersVector, userPredictsVector).getValue() + implicitRegularization * itemImplicitFactors.getValue(itemIndex, factorIndex) + MathUtility.EPSILON;
                        itemImplicitFactors.setValue(itemIndex, factorIndex, (float) (itemImplicitFactors.getValue(itemIndex, factorIndex) * Math.sqrt(numerator / denominator)));
                    }
                }
            }

            // Compute loss value
            for (MatrixScalar term : scoreMatrix) {
                int userIndex = term.getRow();
                int itemIndex = term.getColumn();
                double rating = term.getValue();
                double predRating = scalar.dotProduct(userExplicitFactors.getRowVector(userIndex), itemExplicitFactors.getRowVector(itemIndex)).getValue() + scalar.dotProduct(userImplicitFactors.getRowVector(userIndex), itemImplicitFactors.getRowVector(itemIndex)).getValue();
                totalError += (rating - predRating) * (rating - predRating);
            }

            for (MatrixScalar term : userFeatures) {
                int userIndex = term.getRow();
                int featureIndex = term.getColumn();
                double real = term.getValue();
                double pred = predictUserFactor(scalar, userIndex, featureIndex);
                totalError += (real - pred) * (real - pred);
            }

            for (MatrixScalar term : itemFeatures) {
                int itemIndex = term.getRow();
                int featureIndex = term.getColumn();
                double real = term.getValue();
                double pred = predictItemFactor(scalar, itemIndex, featureIndex);
                totalError += (real - pred) * (real - pred);
            }

            totalError += explicitRegularization * (userExplicitFactors.getNorm(2F, false) + itemExplicitFactors.getNorm(2F, false));
            totalError += implicitRegularization * (userImplicitFactors.getNorm(2F, false) + itemImplicitFactors.getNorm(2F, false));
            totalError += featureRegularization * featureFactors.getNorm(2F, false);

            logger.info("iter:" + epocheIndex + ", loss:" + totalError);
        }
    }

    protected float predictUserFactor(DefaultScalar scalar, int userIndex, int featureIndex) {
        return scalar.dotProduct(userExplicitFactors.getRowVector(userIndex), featureFactors.getRowVector(featureIndex)).getValue();
    }

    protected float predictItemFactor(DefaultScalar scalar, int itemIndex, int featureIndex) {
        return scalar.dotProduct(itemExplicitFactors.getRowVector(itemIndex), featureFactors.getRowVector(featureIndex)).getValue();
    }

    @Override
    protected float predict(int userIndex, int itemIndex) {
        DefaultScalar scalar = DefaultScalar.getInstance();
        return scalar.dotProduct(userExplicitFactors.getRowVector(userIndex), itemExplicitFactors.getRowVector(itemIndex)).getValue() + scalar.dotProduct(userImplicitFactors.getRowVector(userIndex), itemImplicitFactors.getRowVector(itemIndex)).getValue();
    }

}
