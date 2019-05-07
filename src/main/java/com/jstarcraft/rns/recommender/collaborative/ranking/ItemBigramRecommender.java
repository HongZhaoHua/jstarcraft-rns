package com.jstarcraft.rns.recommender.collaborative.ranking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.KeyValue;
import com.jstarcraft.core.utility.RandomUtility;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.configurator.Configuration;
import com.jstarcraft.rns.recommender.ProbabilisticGraphicalRecommender;
import com.jstarcraft.rns.utility.GammaUtility;
import com.jstarcraft.rns.utility.SampleUtility;

/**
 * 
 * Item Bigram推荐器
 * 
 * <pre>
 * Topic modeling: beyond bag-of-words
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class ItemBigramRecommender extends ProbabilisticGraphicalRecommender {

	/** 上下文字段 */
	private String instantField;

	/** 上下文维度 */
	private int instantDimension;

	private Map<Integer, List<Integer>> userItemMap;

	/**
	 * k: current topic; j: previously rated item; i: current item
	 */
	private int[][][] topicItemBigramTimes;
	private DenseMatrix topicItemProbabilities;
	private float[][][] topicItemBigramProbabilities, topicItemBigramSums;

	private DenseMatrix beta;

	/**
	 * vector of hyperparameters for alpha
	 */
	private DenseVector alpha;

	/**
	 * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
	 */
	private float initAlpha;

	/**
	 * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
	 */
	private float initBeta;

	/**
	 * cumulative statistics of theta, phi
	 */
	private DenseMatrix userTopicSums;

	/**
	 * entry[u, k]: number of tokens assigned to topic k, given user u.
	 */
	private DenseMatrix userTopicTimes;

	/**
	 * entry[u]: number of tokens rated by user u.
	 */
	private DenseVector userTokenNumbers;

	/**
	 * posterior probabilities of parameters
	 */
	private DenseMatrix userTopicProbabilities;

	/**
	 * entry[u, i, k]: topic assignment as sparse structure
	 */
	// TODO 考虑DenseMatrix支持Integer类型
	private Table<Integer, Integer, Integer> topicAssignments;

	private DenseVector randomProbabilities;

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		initAlpha = configuration.getFloat("rec.user.dirichlet.prior", 0.01F);
		initBeta = configuration.getFloat("rec.topic.dirichlet.prior", 0.01F);

		instantField = configuration.getString("data.model.fields.instant");
		instantDimension = model.getQualityInner(instantField);
		Table<Integer, Integer, Integer> instantTabel = HashBasedTable.create();
		for (DataInstance sample : model) {
			Integer instant = instantTabel.get(sample.getQualityFeature(userDimension), sample.getQualityFeature(itemDimension));
			if (instant == null) {
				instant = sample.getQualityFeature(instantDimension);
			} else {
				instant = sample.getQualityFeature(instantDimension) > instant ? sample.getQualityFeature(instantDimension) : instant;
			}
			instantTabel.put(sample.getQualityFeature(userDimension), sample.getQualityFeature(itemDimension), instant);
		}
		// build the training data, sorting by date
		userItemMap = new HashMap<>();
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			// TODO 考虑优化
			SparseVector userVector = trainMatrix.getRowVector(userIndex);
			if (userVector.getElementSize() == 0) {
				continue;
			}

			// 按照时间排序
			List<KeyValue<Integer, Integer>> instants = new ArrayList<>(userVector.getElementSize());
			for (VectorScalar term : userVector) {
				int itemIndex = term.getIndex();
				instants.add(new KeyValue<>(itemIndex, instantTabel.get(userIndex, itemIndex)));
			}
			Collections.sort(instants, (left, right) -> {
				// 升序
				return left.getValue().compareTo(right.getValue());
			});
			List<Integer> items = new ArrayList<>(userVector.getElementSize());
			for (KeyValue<Integer, Integer> term : instants) {
				items.add(term.getKey());
			}

			userItemMap.put(userIndex, items);
		}

		// count variables
		// initialize count variables.
		userTopicTimes = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		userTokenNumbers = DenseVector.valueOf(numberOfUsers);

		// 注意:numItems + 1的最后一个元素代表没有任何记录的概率
		topicItemBigramTimes = new int[numberOfFactors][numberOfItems + 1][numberOfItems];
		topicItemProbabilities = DenseMatrix.valueOf(numberOfFactors, numberOfItems + 1);

		// Logs.debug("topicPreItemCurItemNum consumes {} bytes",
		// Strings.toString(Memory.bytes(topicPreItemCurItemNum)));

		// parameters
		userTopicSums = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		topicItemBigramSums = new float[numberOfFactors][numberOfItems + 1][numberOfItems];
		topicItemBigramProbabilities = new float[numberOfFactors][numberOfItems + 1][numberOfItems];

		// hyper-parameters
		alpha = DenseVector.valueOf(numberOfFactors);
		alpha.setValues(initAlpha);

		beta = DenseMatrix.valueOf(numberOfFactors, numberOfItems + 1);
		beta.setValues(initBeta);

		// initialization
		topicAssignments = HashBasedTable.create();
		for (Entry<Integer, List<Integer>> term : userItemMap.entrySet()) {
			int userIndex = term.getKey();
			List<Integer> items = term.getValue();

			for (int index = 0; index < items.size(); index++) {
				int nextItemIndex = items.get(index);
				// TODO 需要重构
				int topicIndex = RandomUtility.randomInteger(numberOfFactors);
				topicAssignments.put(userIndex, nextItemIndex, topicIndex);

				userTopicTimes.shiftValue(userIndex, topicIndex, 1F);
				userTokenNumbers.shiftValue(userIndex, 1F);

				int previousItemIndex = index > 0 ? items.get(index - 1) : numberOfItems;
				topicItemBigramTimes[topicIndex][previousItemIndex][nextItemIndex]++;
				topicItemProbabilities.shiftValue(topicIndex, previousItemIndex, 1F);
			}
		}

		randomProbabilities = DenseVector.valueOf(numberOfFactors);
	}

	@Override
	protected void eStep() {
		float sumAlpha = alpha.getSum(false);
		DenseVector topicVector = DenseVector.valueOf(numberOfFactors);
		topicVector.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(beta.getRowVector(scalar.getIndex()).getSum(false));
		});

		for (Entry<Integer, List<Integer>> term : userItemMap.entrySet()) {
			int userIndex = term.getKey();
			List<Integer> items = term.getValue();

			for (int index = 0; index < items.size(); index++) {
				int nextItemIndex = items.get(index);
				int assignmentIndex = topicAssignments.get(userIndex, nextItemIndex);

				userTopicTimes.shiftValue(userIndex, assignmentIndex, -1F);
				userTokenNumbers.shiftValue(userIndex, -1F);

				int previousItemIndex = index > 0 ? items.get(index - 1) : numberOfItems;
				topicItemBigramTimes[assignmentIndex][previousItemIndex][nextItemIndex]--;
				topicItemProbabilities.shiftValue(assignmentIndex, previousItemIndex, -1F);

				// 计算概率
				DefaultScalar sum = DefaultScalar.getInstance();
				sum.setValue(0F);
				randomProbabilities.iterateElement(MathCalculator.SERIAL, (scalar) -> {
					int topicIndex = scalar.getIndex();
					float userProbability = (userTopicTimes.getValue(userIndex, assignmentIndex) + alpha.getValue(topicIndex)) / (userTokenNumbers.getValue(userIndex) + sumAlpha);
					float topicProbability = (topicItemBigramTimes[topicIndex][previousItemIndex][nextItemIndex] + beta.getValue(topicIndex, previousItemIndex)) / (topicItemProbabilities.getValue(topicIndex, previousItemIndex) + topicVector.getValue(topicIndex));
					float value = userProbability * topicProbability;
					sum.shiftValue(value);
					scalar.setValue(sum.getValue());
				});

				int randomIndex = SampleUtility.binarySearch(randomProbabilities, 0, randomProbabilities.getElementSize() - 1, RandomUtility.randomFloat(sum.getValue()));
				topicAssignments.put(userIndex, nextItemIndex, randomIndex);
				userTopicTimes.shiftValue(userIndex, randomIndex, 1F);
				userTokenNumbers.shiftValue(userIndex, 1F);
				topicItemBigramTimes[randomIndex][previousItemIndex][nextItemIndex]++;
				topicItemProbabilities.shiftValue(randomIndex, previousItemIndex, 1F);
			}
		}
	}

	@Override
	protected void mStep() {
		float denominator = 0F;
		float value = 0F;

		float alphaSum = alpha.getSum(false);
		float alphaDigamma = GammaUtility.digamma(alphaSum);
		float alphaValue;
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			// TODO 应该修改为稀疏向量
			value = userTokenNumbers.getValue(userIndex);
			if (value != 0F) {
				denominator += GammaUtility.digamma(value + alphaSum) - alphaDigamma;
			}
		}
		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			alphaValue = alpha.getValue(topicIndex);
			alphaDigamma = GammaUtility.digamma(alphaValue);
			float numerator = 0F;
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				// TODO 应该修改为稀疏矩阵
				value = userTopicTimes.getValue(userIndex, topicIndex);
				if (value != 0F) {
					numerator += GammaUtility.digamma(value + alphaValue) - alphaDigamma;
				}
			}
			if (numerator != 0D) {
				alpha.setValue(topicIndex, alphaValue * (numerator / denominator));
			}
		}

		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			float betaSum = beta.getRowVector(topicIndex).getSum(false);
			float betaDigamma = GammaUtility.digamma(betaSum);
			float betaValue;
			float[] denominators = new float[numberOfItems + 1];
			for (int itemIndex = 0; itemIndex < numberOfItems + 1; itemIndex++) {
				// TODO 应该修改为稀疏矩阵
				value = topicItemProbabilities.getValue(topicIndex, itemIndex);
				if (value != 0F) {
					denominators[itemIndex] = GammaUtility.digamma(value + betaSum) - betaDigamma;
				}
			}
			for (int previousItemIndex = 0; previousItemIndex < numberOfItems + 1; previousItemIndex++) {
				betaValue = beta.getValue(topicIndex, previousItemIndex);
				betaDigamma = GammaUtility.digamma(betaValue);
				float numerator = 0F;
				denominator = 0F;
				for (int nextItemIndex = 0; nextItemIndex < numberOfItems; nextItemIndex++) {
					// TODO 应该修改为稀疏张量
					value = topicItemBigramTimes[topicIndex][previousItemIndex][nextItemIndex];
					if (value != 0F) {
						numerator += GammaUtility.digamma(value + betaValue) - betaDigamma;
					}
					denominator += denominators[previousItemIndex];
				}
				if (numerator != 0F) {
					beta.setValue(topicIndex, previousItemIndex, betaValue * (numerator / denominator));
				}
			}
		}
	}

	@Override
	protected void readoutParams() {
		float value;
		float sumAlpha = alpha.getSum(false);
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
				value = (userTopicTimes.getValue(userIndex, topicIndex) + alpha.getValue(topicIndex)) / (userTokenNumbers.getValue(userIndex) + sumAlpha);
				userTopicSums.shiftValue(userIndex, topicIndex, value);
			}
		}
		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			float betaTopicValue = beta.getRowVector(topicIndex).getSum(false);
			for (int previousItemIndex = 0; previousItemIndex < numberOfItems + 1; previousItemIndex++) {
				for (int nextItemIndex = 0; nextItemIndex < numberOfItems; nextItemIndex++) {
					value = (topicItemBigramTimes[topicIndex][previousItemIndex][nextItemIndex] + beta.getValue(topicIndex, previousItemIndex)) / (topicItemProbabilities.getValue(topicIndex, previousItemIndex) + betaTopicValue);
					topicItemBigramSums[topicIndex][previousItemIndex][nextItemIndex] += value;
				}
			}
		}
		if (logger.isInfoEnabled()) {
			String message = StringUtility.format("sumAlpha is {}", sumAlpha);
			logger.info(message);
		}
		numberOfStatistics++;
	}

	@Override
	protected void estimateParams() {
		userTopicProbabilities = DenseMatrix.copyOf(userTopicSums);
		userTopicProbabilities.scaleValues(1F / numberOfStatistics);

		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			for (int previousItemIndex = 0; previousItemIndex < numberOfItems + 1; previousItemIndex++) {
				for (int nextItemIndex = 0; nextItemIndex < numberOfItems; nextItemIndex++) {
					topicItemBigramProbabilities[topicIndex][previousItemIndex][nextItemIndex] = topicItemBigramSums[topicIndex][previousItemIndex][nextItemIndex] / numberOfStatistics;
				}
			}
		}
	}

	@Override
	public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
		List<Integer> items = userItemMap.get(userIndex);
		int rateIndex = items == null ? numberOfItems : items.get(items.size() - 1); // last
		// rated
		// item
		float value = 0F;
		for (int topicIndex = 0; topicIndex < numberOfFactors; topicIndex++) {
			value += userTopicProbabilities.getValue(userIndex, topicIndex) * topicItemBigramProbabilities[topicIndex][rateIndex][itemIndex];
		}

		return value;
	}

}
