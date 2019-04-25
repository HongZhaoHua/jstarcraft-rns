package com.jstarcraft.recommendation.recommender.context.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.ai.utility.Float2FloatKeyValue;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.DataInstance;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.exception.RecommendationException;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;
import com.jstarcraft.recommendation.utility.LogisticUtility;

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
public class RankGeoFMRecommender extends MatrixFactorizationRecommender {

	protected DenseMatrix explicitUserFactors, implicitUserFactors, itemFactors;

	protected ArrayVector[] neighborWeights;

	protected float margin, radius, balance;

	protected DenseVector E;

	protected DenseMatrix geoInfluences;

	protected int knn;

	protected Float2FloatKeyValue[] itemLocations;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		margin = configuration.getFloat("rec.ranking.margin", 0.3F);
		radius = configuration.getFloat("rec.regularization.radius", 1F);
		balance = configuration.getFloat("rec.regularization.balance", 0.2F);
		knn = configuration.getInteger("rec.item.nearest.neighbour.number", 300);

		geoInfluences = DenseMatrix.valueOf(numberOfItems, numberOfFactors);

		explicitUserFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		explicitUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		implicitUserFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		implicitUserFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		itemFactors = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
		itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});

		itemLocations = new Float2FloatKeyValue[numberOfItems];
		InstanceAccessor locationModel = space.getModule("location");
		for (DataInstance instance : locationModel) {
			int itemIndex = instance.getDiscreteFeature(0);
			Float2FloatKeyValue itemLocation = new Float2FloatKeyValue(instance.getContinuousFeature(0), instance.getContinuousFeature(1));
			itemLocations[itemIndex] = itemLocation;
		}
		calculateNeighborWeightMatrix(knn);

		E = DenseVector.valueOf(numberOfItems + 1);
		E.setValue(1, 1F);
		for (int itemIndex = 2; itemIndex <= numberOfItems; itemIndex++) {
			E.setValue(itemIndex, E.getValue(itemIndex - 1) + 1F / itemIndex);
		}

		geoInfluences = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
	}

	@Override
	protected void doPractice() {
		DefaultScalar scalar = DefaultScalar.getInstance();
		DenseMatrix explicitUserDeltas = DenseMatrix.valueOf(explicitUserFactors.getRowSize(), explicitUserFactors.getColumnSize());
		DenseMatrix implicitUserDeltas = DenseMatrix.valueOf(implicitUserFactors.getRowSize(), implicitUserFactors.getColumnSize());
		DenseMatrix itemDeltas = DenseMatrix.valueOf(itemFactors.getRowSize(), itemFactors.getColumnSize());

		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			calculateGeoInfluenceMatrix();

			totalLoss = 0F;
			explicitUserDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
				element.setValue(explicitUserFactors.getValue(element.getRow(), element.getColumn()));
			});
			implicitUserDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
				element.setValue(implicitUserFactors.getValue(element.getRow(), element.getColumn()));
			});
			itemDeltas.iterateElement(MathCalculator.PARALLEL, (element) -> {
				element.setValue(itemFactors.getValue(element.getRow(), element.getColumn()));
			});

			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				SparseVector userVector = trainMatrix.getRowVector(userIndex);
				for (VectorScalar term : userVector) {
					int positiveItemIndex = term.getIndex();

					int sampleCount = 0;
					float positiveScore = scalar.dotProduct(explicitUserDeltas.getRowVector(userIndex), itemDeltas.getRowVector(positiveItemIndex)).getValue() + scalar.dotProduct(implicitUserDeltas.getRowVector(userIndex), geoInfluences.getRowVector(positiveItemIndex)).getValue();
					float positiveValue = term.getValue();

					int negativeItemIndex;
					float negativeScore;
					float negativeValue;

					while (true) {
						negativeItemIndex = RandomUtility.randomInteger(numberOfItems);
						negativeScore = scalar.dotProduct(explicitUserDeltas.getRowVector(userIndex), itemDeltas.getRowVector(negativeItemIndex)).getValue() + scalar.dotProduct(implicitUserDeltas.getRowVector(userIndex), geoInfluences.getRowVector(negativeItemIndex)).getValue();
						negativeValue = 0F;
						for (VectorScalar rateTerm : userVector) {
							if (rateTerm.getIndex() == negativeItemIndex) {
								negativeValue = rateTerm.getValue();
							}
						}

						sampleCount++;
						if ((indicator(positiveValue, negativeValue) && indicator(negativeScore + margin, positiveScore)) || sampleCount > numberOfItems) {
							break;
						}
					}

					if (indicator(positiveValue, negativeValue) && indicator(negativeScore + margin, positiveScore)) {
						int sampleIndex = numberOfItems / sampleCount;

						float s = LogisticUtility.getValue(negativeScore + margin - positiveScore);
						totalLoss += E.getValue(sampleIndex) * s;

						float uij = s * (1 - s);
						float error = E.getValue(sampleIndex) * uij * learnRate;
						DenseVector positiveItemVector = itemFactors.getRowVector(positiveItemIndex);
						DenseVector negativeItemVector = itemFactors.getRowVector(negativeItemIndex);
						DenseVector explicitUserVector = explicitUserFactors.getRowVector(userIndex);

						DenseVector positiveGeoVector = geoInfluences.getRowVector(positiveItemIndex);
						DenseVector negativeGeoVector = geoInfluences.getRowVector(negativeItemIndex);
						DenseVector implicitUserVector = implicitUserFactors.getRowVector(userIndex);

						// TODO 可以并发计算
						for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
							explicitUserVector.setValue(factorIndex, explicitUserVector.getValue(factorIndex) - (negativeItemVector.getValue(factorIndex) - positiveItemVector.getValue(factorIndex)) * error);
							implicitUserVector.setValue(factorIndex, implicitUserVector.getValue(factorIndex) - (negativeGeoVector.getValue(factorIndex) - positiveGeoVector.getValue(factorIndex)) * error);
						}
						// TODO 可以并发计算
						for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
							float itemDelta = explicitUserVector.getValue(factorIndex) * error;
							positiveItemVector.setValue(factorIndex, positiveItemVector.getValue(factorIndex) + itemDelta);
							negativeItemVector.setValue(factorIndex, negativeItemVector.getValue(factorIndex) - itemDelta);
						}

						float explicitUserDelta = explicitUserVector.getNorm(2);
						if (explicitUserDelta > radius) {
							explicitUserDelta = radius / explicitUserDelta;
						} else {
							explicitUserDelta = 1F;
						}
						float implicitUserDelta = implicitUserVector.getNorm(2);
						if (implicitUserDelta > balance * radius) {
							implicitUserDelta = balance * radius / implicitUserDelta;
						} else {
							implicitUserDelta = 1F;
						}
						float positiveItemDelta = positiveItemVector.getNorm(2);
						if (positiveItemDelta > radius) {
							positiveItemDelta = radius / positiveItemDelta;
						} else {
							positiveItemDelta = 1F;
						}
						float negativeItemDelta = negativeItemVector.getNorm(2);
						if (negativeItemDelta > radius) {
							negativeItemDelta = radius / negativeItemDelta;
						} else {
							negativeItemDelta = 1F;
						}
						// TODO 可以并发计算
						for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
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

			if (isConverged(iterationStep) && isConverged) {
				break;
			}
			isLearned(iterationStep);
			currentLoss = totalLoss;
		}
	}

	/**
	 * @param k_nearest
	 * @return
	 */
	private void calculateNeighborWeightMatrix(Integer k_nearest) {
		Table<Integer, Integer, Float> dataTable = HashBasedTable.create();
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			List<KeyValue<Integer, Float>> locationNeighbors = new ArrayList<>(numberOfItems);
			Float2FloatKeyValue location = itemLocations[itemIndex];
			for (int neighborIndex = 0; neighborIndex < numberOfItems; neighborIndex++) {
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
				dataTable.put(itemIndex, neighborItemIdx, weight);
			}
		}

		SparseMatrix matrix = SparseMatrix.valueOf(numberOfItems, numberOfItems, dataTable);
		neighborWeights = new ArrayVector[numberOfItems];
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
			ArrayVector neighborVector = new ArrayVector(matrix.getRowVector(itemIndex));
			neighborVector.scaleValues(1F / neighborVector.getSum(false));
			neighborWeights[itemIndex] = neighborVector;
		}
	}

	private void calculateGeoInfluenceMatrix() throws RecommendationException {
		for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
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

	private float getDistance(float lat1, float long1, float lat2, float long2) {
		float a, b, R;
		R = 6378137F;
		lat1 = (float) (lat1 * Math.PI / 180F);
		lat2 = (float) (lat2 * Math.PI / 180F);
		a = lat1 - lat2;
		b = (float) ((long1 - long2) * Math.PI / 180F);
		float d;
		float sa2, sb2;
		sa2 = (float) Math.sin(a / 2F);
		sb2 = (float) Math.sin(b / 2F);
		d = (float) (2F * R * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(lat1) * Math.cos(lat2) * sb2 * sb2)));
		return d / 1000F;
	}

	private boolean indicator(double left, double right) {
		return left > right;
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		DefaultScalar scalar = DefaultScalar.getInstance();
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		float value = scalar.dotProduct(explicitUserFactors.getRowVector(userIndex), itemFactors.getRowVector(itemIndex)).getValue();
		value += scalar.dotProduct(implicitUserFactors.getRowVector(userIndex), geoInfluences.getRowVector(itemIndex)).getValue();
		return value;
	}

}
