package com.jstarcraft.rns.model.context.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Option;
import com.jstarcraft.core.utility.Float2FloatKeyValue;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.rns.model.MatrixFactorizationModel;
import com.jstarcraft.rns.model.exception.ModelException;
import com.jstarcraft.rns.utility.LogisticUtility;

import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * 
 * Rank GeoFM推荐器
 * 
 * <pre>
 * Rank-GeoFM: A ranking based geographical factorization method for point of interest recommendation
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class RankGeoFMModel extends MatrixFactorizationModel {

    protected DenseMatrix explicitUserFactors, implicitUserFactors, itemFactors;

    protected ArrayVector[] neighborWeights;

    protected float margin, radius, balance;

    protected DenseVector E;

    protected DenseMatrix geoInfluences;

    protected int knn;

    protected Float2FloatKeyValue[] itemLocations;

    private String longitudeField, latitudeField;

    private int longitudeDimension, latitudeDimension;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        margin = configuration.getFloat("recommender.ranking.margin", 0.3F);
        radius = configuration.getFloat("recommender.regularization.radius", 1F);
        balance = configuration.getFloat("recommender.regularization.balance", 0.2F);
        knn = configuration.getInteger("recommender.item.nearest.neighbour.number", 300);

        longitudeField = configuration.getString("data.model.fields.longitude");
        latitudeField = configuration.getString("data.model.fields.latitude");

        DataModule locationModel = space.getModule("location");
        longitudeDimension = locationModel.getQuantityInner(longitudeField);
        latitudeDimension = locationModel.getQuantityInner(latitudeField);

        geoInfluences = DenseMatrix.valueOf(itemSize, factorSize);

        explicitUserFactors = DenseMatrix.valueOf(userSize, factorSize);
        explicitUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        implicitUserFactors = DenseMatrix.valueOf(userSize, factorSize);
        implicitUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });
        itemFactors = DenseMatrix.valueOf(itemSize, factorSize);
        itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
            scalar.setValue(distribution.sample().floatValue());
        });

        itemLocations = new Float2FloatKeyValue[itemSize];

        int itemDimension = locationModel.getQualityInner(itemField);
        for (DataInstance instance : locationModel) {
            int itemIndex = instance.getQualityFeature(itemDimension);
            Float2FloatKeyValue itemLocation = new Float2FloatKeyValue(instance.getQuantityFeature(longitudeDimension), instance.getQuantityFeature(latitudeDimension));
            itemLocations[itemIndex] = itemLocation;
        }
        calculateNeighborWeightMatrix(knn);

        E = DenseVector.valueOf(itemSize + 1);
        E.setValue(1, 1F);
        for (int itemIndex = 2; itemIndex <= itemSize; itemIndex++) {
            E.setValue(itemIndex, E.getValue(itemIndex - 1) + 1F / itemIndex);
        }

        geoInfluences = DenseMatrix.valueOf(itemSize, factorSize);
    }

    @Override
    protected void doPractice() {
        DefaultScalar scalar = DefaultScalar.getInstance();
        DenseMatrix explicitUserDeltas = DenseMatrix.valueOf(explicitUserFactors.getRowSize(), explicitUserFactors.getColumnSize());
        DenseMatrix implicitUserDeltas = DenseMatrix.valueOf(implicitUserFactors.getRowSize(), implicitUserFactors.getColumnSize());
        DenseMatrix itemDeltas = DenseMatrix.valueOf(itemFactors.getRowSize(), itemFactors.getColumnSize());

        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            calculateGeoInfluenceMatrix();

            totalError = 0F;
            explicitUserDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
                element.setValue(explicitUserFactors.getValue(element.getRow(), element.getColumn()));
            });
            implicitUserDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
                element.setValue(implicitUserFactors.getValue(element.getRow(), element.getColumn()));
            });
            itemDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
                element.setValue(itemFactors.getValue(element.getRow(), element.getColumn()));
            });

            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                for (VectorScalar term : userVector) {
                    int positiveItemIndex = term.getIndex();

                    int sampleCount = 0;
                    float positiveScore = scalar.dotProduct(explicitUserDeltas.getRowVector(userIndex), itemDeltas.getRowVector(positiveItemIndex)).getValue() + scalar.dotProduct(implicitUserDeltas.getRowVector(userIndex), geoInfluences.getRowVector(positiveItemIndex)).getValue();
                    float positiveValue = term.getValue();

                    int negativeItemIndex;
                    float negativeScore;
                    float negativeValue;

                    while (true) {
                        negativeItemIndex = RandomUtility.randomInteger(itemSize);
                        negativeScore = scalar.dotProduct(explicitUserDeltas.getRowVector(userIndex), itemDeltas.getRowVector(negativeItemIndex)).getValue() + scalar.dotProduct(implicitUserDeltas.getRowVector(userIndex), geoInfluences.getRowVector(negativeItemIndex)).getValue();
                        negativeValue = 0F;
                        for (VectorScalar rateTerm : userVector) {
                            if (rateTerm.getIndex() == negativeItemIndex) {
                                negativeValue = rateTerm.getValue();
                            }
                        }

                        sampleCount++;
                        if ((indicator(positiveValue, negativeValue) && indicator(negativeScore + margin, positiveScore)) || sampleCount > itemSize) {
                            break;
                        }
                    }

                    if (indicator(positiveValue, negativeValue) && indicator(negativeScore + margin, positiveScore)) {
                        int sampleIndex = itemSize / sampleCount;

                        float s = LogisticUtility.getValue(negativeScore + margin - positiveScore);
                        totalError += E.getValue(sampleIndex) * s;

                        float uij = s * (1 - s);
                        float error = E.getValue(sampleIndex) * uij * learnRatio;
                        DenseVector positiveItemVector = itemFactors.getRowVector(positiveItemIndex);
                        DenseVector negativeItemVector = itemFactors.getRowVector(negativeItemIndex);
                        DenseVector explicitUserVector = explicitUserFactors.getRowVector(userIndex);

                        DenseVector positiveGeoVector = geoInfluences.getRowVector(positiveItemIndex);
                        DenseVector negativeGeoVector = geoInfluences.getRowVector(negativeItemIndex);
                        DenseVector implicitUserVector = implicitUserFactors.getRowVector(userIndex);

                        // TODO 可以并发计算
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            explicitUserVector.setValue(factorIndex, explicitUserVector.getValue(factorIndex) - (negativeItemVector.getValue(factorIndex) - positiveItemVector.getValue(factorIndex)) * error);
                            implicitUserVector.setValue(factorIndex, implicitUserVector.getValue(factorIndex) - (negativeGeoVector.getValue(factorIndex) - positiveGeoVector.getValue(factorIndex)) * error);
                        }
                        // TODO 可以并发计算
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            float itemDelta = explicitUserVector.getValue(factorIndex) * error;
                            positiveItemVector.setValue(factorIndex, positiveItemVector.getValue(factorIndex) + itemDelta);
                            negativeItemVector.setValue(factorIndex, negativeItemVector.getValue(factorIndex) - itemDelta);
                        }

                        float explicitUserDelta = explicitUserVector.getNorm(2, true);
                        if (explicitUserDelta > radius) {
                            explicitUserDelta = radius / explicitUserDelta;
                        } else {
                            explicitUserDelta = 1F;
                        }
                        float implicitUserDelta = implicitUserVector.getNorm(2F, true);
                        if (implicitUserDelta > balance * radius) {
                            implicitUserDelta = balance * radius / implicitUserDelta;
                        } else {
                            implicitUserDelta = 1F;
                        }
                        float positiveItemDelta = positiveItemVector.getNorm(2, true);
                        if (positiveItemDelta > radius) {
                            positiveItemDelta = radius / positiveItemDelta;
                        } else {
                            positiveItemDelta = 1F;
                        }
                        float negativeItemDelta = negativeItemVector.getNorm(2, true);
                        if (negativeItemDelta > radius) {
                            negativeItemDelta = radius / negativeItemDelta;
                        } else {
                            negativeItemDelta = 1F;
                        }
                        // TODO 可以并发计算
                        for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                            if (explicitUserDelta != 1F) {
                                explicitUserVector.setValue(factorIndex, explicitUserVector.getValue(factorIndex) * explicitUserDelta);
                            }
                            if (implicitUserDelta != 1F) {
                                implicitUserVector.setValue(factorIndex, implicitUserVector.getValue(factorIndex) * implicitUserDelta);
                            }
                            if (positiveItemDelta != 1F) {
                                positiveItemVector.setValue(factorIndex, positiveItemVector.getValue(factorIndex) * positiveItemDelta);
                            }
                            if (negativeItemDelta != 1F) {
                                negativeItemVector.setValue(factorIndex, negativeItemVector.getValue(factorIndex) * negativeItemDelta);
                            }
                        }
                    }
                }
            }

            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            isLearned(epocheIndex);
            currentError = totalError;
        }
    }

    /**
     * @param k_nearest
     * @return
     */
    private void calculateNeighborWeightMatrix(Integer k_nearest) {
        HashMatrix dataTable = new HashMatrix(true, itemSize, itemSize, new Long2FloatRBTreeMap());
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            List<KeyValue<Integer, Float>> locationNeighbors = new ArrayList<>(itemSize);
            Float2FloatKeyValue location = itemLocations[itemIndex];
            for (int neighborIndex = 0; neighborIndex < itemSize; neighborIndex++) {
                if (itemIndex != neighborIndex) {
                    Float2FloatKeyValue neighborLocation = itemLocations[neighborIndex];
                    float distance = getDistance(location.getKey(), location.getValue(), neighborLocation.getKey(), neighborLocation.getValue());
                    locationNeighbors.add(new KeyValue<>(neighborIndex, distance));
                }
            }
            Collections.sort(locationNeighbors, (left, right) -> {
                // 升序
                return left.getValue().compareTo(right.getValue());
            });
            locationNeighbors = locationNeighbors.subList(0, k_nearest);

            for (int index = 0; index < locationNeighbors.size(); index++) {
                int neighborItemIdx = locationNeighbors.get(index).getKey();
                float weight;
                if (locationNeighbors.get(index).getValue() < 0.5F) {
                    weight = 1F / 0.5F;
                } else {
                    weight = 1F / (locationNeighbors.get(index).getValue());
                }
                dataTable.setValue(itemIndex, neighborItemIdx, weight);
            }
        }

        SparseMatrix matrix = SparseMatrix.valueOf(itemSize, itemSize, dataTable);
        neighborWeights = new ArrayVector[itemSize];
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            ArrayVector neighborVector = new ArrayVector(matrix.getRowVector(itemIndex));
            neighborVector.scaleValues(1F / neighborVector.getSum(false));
            neighborWeights[itemIndex] = neighborVector;
        }
    }

    private void calculateGeoInfluenceMatrix() throws ModelException {
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            ArrayVector neighborVector = neighborWeights[itemIndex];
            if (neighborVector.getElementSize() == 0) {
                continue;
            }
            DenseVector geoVector = geoInfluences.getRowVector(itemIndex);
            geoVector.setValues(0F);
            for (VectorScalar term : neighborVector) {
                DenseVector itemVector = itemFactors.getRowVector(term.getIndex());
                geoVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
                    int index = scalar.getIndex();
                    float value = scalar.getValue();
                    scalar.setValue(value + itemVector.getValue(index) * term.getValue());
                });
            }
        }
    }

    private float getDistance(float leftLatitude, float leftLongitude, float rightLatitude, float rightLongitude) {
        float radius = 6378137F;
        leftLatitude = (float) (leftLatitude * Math.PI / 180F);
        rightLatitude = (float) (rightLatitude * Math.PI / 180F);
        float latitude = leftLatitude - rightLatitude;
        float longitude = (float) ((leftLongitude - rightLongitude) * Math.PI / 180F);
        latitude = (float) Math.sin(latitude / 2F);
        longitude = (float) Math.sin(longitude / 2F);
        float distance = (float) (2F * radius * Math.asin(Math.sqrt(latitude * latitude + Math.cos(leftLatitude) * Math.cos(rightLatitude) * longitude * longitude)));
        return distance / 1000F;
    }

    private boolean indicator(double left, double right) {
        return left > right;
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        DefaultScalar scalar = DefaultScalar.getInstance();
        float value = scalar.dotProduct(explicitUserFactors.getRowVector(userIndex), itemFactors.getRowVector(itemIndex)).getValue();
        value += scalar.dotProduct(implicitUserFactors.getRowVector(userIndex), geoInfluences.getRowVector(itemIndex)).getValue();
        instance.setQuantityMark(value);
    }

}
