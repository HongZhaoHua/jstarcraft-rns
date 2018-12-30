package com.jstarcraft.recommendation.recommender.collaborative.ranking;

import java.util.Arrays;
import java.util.Comparator;

import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;
import com.jstarcraft.recommendation.utility.LogisticUtility;
import com.jstarcraft.recommendation.utility.SampleUtility;

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
	private int[][] negativeIndexes;
	private float[] negativeValues;
	private Integer[] orderIndexes;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);
		dynamicRho = configuration.getFloat("rec.item.distribution.parameter");
		numberOfOrders = configuration.getInteger("rec.number.orders", 10);

		DefaultScalar sum = DefaultScalar.getInstance();
		sum.setValue(0F);
		orderProbabilities = DenseVector.valueOf(numberOfOrders);
		orderProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			int index = scalar.getIndex();
			float value = (float) (Math.exp(-(index + 1) / (numberOfOrders * dynamicRho)));
			sum.shiftValue(value);
			scalar.setValue(sum.getValue());
		});
		negativeIndexes = new int[numberOfOrders][marker.getDiscreteOrder()];
		negativeValues = new float[numberOfOrders];
		orderIndexes = new Integer[numberOfOrders];
		for (int index = 0; index < numberOfOrders; index++) {
			orderIndexes[index] = index;
		}
	}

	@Override
	protected float getGradientValue(DefaultScalar scalar, int[] dataPaginations, int[] dataPositions) {
		int userIndex;
		while (true) {
			userIndex = RandomUtility.randomInteger(numberOfUsers);
			SparseVector userVector = trainMatrix.getRowVector(userIndex);
			if (userVector.getElementSize() == 0 || userVector.getElementSize() == numberOfItems) {
				continue;
			}

			int from = dataPaginations[userIndex], to = dataPaginations[userIndex + 1];
			int positivePosition = dataPositions[RandomUtility.randomInteger(from, to)];
			for (int index = 0; index < negativeKeys.length; index++) {
				positiveKeys[index] = marker.getDiscreteFeature(index, positivePosition);
			}
			// TODO negativeGroup.size()可能永远达不到numberOfNegatives,需要处理
			for (int orderIndex = 0; orderIndex < numberOfOrders; orderIndex++) {
				int negativeItemIndex = RandomUtility.randomInteger(numberOfItems - userVector.getElementSize());
				for (int position = 0, size = userVector.getElementSize(); position < size; position++) {
					if (negativeItemIndex >= userVector.getIndex(position)) {
						negativeItemIndex++;
						continue;
					}
					break;
				}
				negativeKeys = negativeIndexes[orderIndex];
				// TODO 注意,此处为了故意制造负面特征.
				int negativePosition = dataPositions[RandomUtility.randomInteger(from, to)];
				for (int index = 0; index < negativeKeys.length; index++) {
					negativeKeys[index] = marker.getDiscreteFeature(index, negativePosition);
				}
				negativeKeys[itemDimension] = negativeItemIndex;
				MathVector vector = getFeatureVector(negativeKeys);
				negativeValues[orderIndex] = predict(scalar, vector);
			}

			int orderIndex = SampleUtility.binarySearch(orderProbabilities, 0, orderProbabilities.getElementSize() - 1, RandomUtility.randomFloat(orderProbabilities.getValue(orderProbabilities.getElementSize() - 1)));
			Arrays.sort(orderIndexes, new Comparator<Integer>() {
				@Override
				public int compare(Integer leftIndex, Integer rightIndex) {
					return (negativeValues[leftIndex] > negativeValues[rightIndex] ? -1 : (negativeValues[leftIndex] < negativeValues[rightIndex] ? 1 : 0));
				}
			});
			negativeKeys = negativeIndexes[orderIndexes[orderIndex]];
			break;
		}

		positiveVector = getFeatureVector(positiveKeys);
		negativeVector = getFeatureVector(negativeKeys);

		float positiveScore = predict(scalar, positiveVector);
		float negativeScore = predict(scalar, negativeVector);

		float error = positiveScore - negativeScore;

		// 由于pij_real默认为1,所以简化了loss的计算.
		// loss += -pij_real * Math.log(pij) - (1 - pij_real) *
		// Math.log(1 - pij);
		totalLoss += (float) -Math.log(LogisticUtility.getValue(error));
		float gradient = calaculateGradientValue(lossType, error);
		return gradient;
	}

}
