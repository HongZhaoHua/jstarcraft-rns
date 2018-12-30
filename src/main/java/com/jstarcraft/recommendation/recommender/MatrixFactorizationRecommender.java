package com.jstarcraft.recommendation.recommender;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;

import com.jstarcraft.ai.math.algorithm.distribution.ContinuousProbability;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.recommendation.configure.Configuration;
import com.jstarcraft.recommendation.data.DataSpace;
import com.jstarcraft.recommendation.data.accessor.InstanceAccessor;
import com.jstarcraft.recommendation.data.accessor.SampleAccessor;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * 矩阵分解推荐器
 * 
 * @author Birdy
 *
 */
public abstract class MatrixFactorizationRecommender extends ModelRecommender {

	/** 是否自动调整学习率 */
	protected boolean isLearned;

	/** 衰减率 */
	protected float learnDecay;

	/**
	 * learn rate, maximum learning rate
	 */
	protected float learnRate, learnLimit;

	/**
	 * user latent factors
	 */
	protected DenseMatrix userFactors;

	/**
	 * item latent factors
	 */
	protected DenseMatrix itemFactors;

	/**
	 * the number of latent factors;
	 */
	protected int numberOfFactors;

	/**
	 * user regularization
	 */
	protected float userRegularization;

	/**
	 * item regularization
	 */
	protected float itemRegularization;

	/**
	 * init mean
	 */
	protected float initMean;

	/**
	 * init standard deviation
	 */
	protected float initStd;

	protected ContinuousProbability distribution;

	@Override
	public void prepare(Configuration configuration, SampleAccessor marker, InstanceAccessor model, DataSpace space) {
		super.prepare(configuration, marker, model, space);

		userRegularization = configuration.getFloat("rec.user.regularization", 0.01f);
		itemRegularization = configuration.getFloat("rec.item.regularization", 0.01f);

		numberOfFactors = configuration.getInteger("rec.factor.number", 10);

		isLearned = configuration.getBoolean("rec.learnrate.bolddriver", false);
		learnDecay = configuration.getFloat("rec.learnrate.decay", 1.0f);
		learnRate = configuration.getFloat("rec.iterator.learnrate", 0.01f);
		learnLimit = configuration.getFloat("rec.iterator.learnrate.maximum", 1000.0f);

		// TODO 此处需要重构
		initMean = configuration.getFloat("rec.init.mean", 0F);
		initStd = configuration.getFloat("rec.init.std", 0.1F);

		distribution = new ContinuousProbability(new NormalDistribution(new JDKRandomGenerator(0), initMean, initStd));
		userFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		itemFactors = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
		itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
	}

	protected float predict(int userIndex, int itemIndex) {
		DenseVector userVector = userFactors.getRowVector(userIndex);
		DenseVector itemVector = itemFactors.getRowVector(itemIndex);
		DefaultScalar scalar = DefaultScalar.getInstance();
		return scalar.dotProduct(userVector, itemVector).getValue();
	}

	@Override
	public float predict(int[] dicreteFeatures, float[] continuousFeatures) {
		int userIndex = dicreteFeatures[userDimension];
		int itemIndex = dicreteFeatures[itemDimension];
		return predict(userIndex, itemIndex);
	}

	/**
	 * Update current learning rate after each epoch <br>
	 * <ol>
	 * <li>bold driver: Gemulla et al., Large-scale matrix factorization with
	 * distributed stochastic gradient descent, KDD 2011.</li>
	 * <li>constant decay: Niu et al, Hogwild!: A lock-free approach to
	 * parallelizing stochastic gradient descent, NIPS 2011.</li>
	 * <li>Leon Bottou, Stochastic Gradient Descent Tricks</li>
	 * <li>more ways to adapt learning rate can refer to:
	 * http://www.willamette.edu/~gorr/classes/cs449/momrate.html</li>
	 * </ol>
	 * 
	 * @param iteration
	 *            the current iteration
	 */
	protected void isLearned(int iteration) {
		if (learnRate < 0F) {
			return;
		}
		if (isLearned && iteration > 1) {
			learnRate = Math.abs(currentLoss) > Math.abs(totalLoss) ? learnRate * 1.05F : learnRate * 0.5F;
		} else if (learnDecay > 0 && learnDecay < 1) {
			learnRate *= learnDecay;
		}
		// limit to max-learn-rate after update
		if (learnLimit > 0 && learnRate > learnLimit) {
			learnRate = learnLimit;
		}
	}

	@Deprecated
	// TODO 此方法准备取消,利用向量的有序性代替
	protected List<IntSet> getUserItemSet(SparseMatrix sparseMatrix) {
		List<IntSet> userItemSet = new ArrayList<>(numberOfUsers);
		for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
			SparseVector userVector = sparseMatrix.getRowVector(userIndex);
			IntSet indexes = new IntOpenHashSet();
			for (int position = 0, size = userVector.getElementSize(); position < size; position++) {
				indexes.add(userVector.getIndex(position));
			}
			userItemSet.add(indexes);
		}
		return userItemSet;
	}

}
