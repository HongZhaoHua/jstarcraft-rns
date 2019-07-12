package com.jstarcraft.rns.recommend;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.exception.RecommendException;

/**
 * 概率图推荐器
 * 
 * @author Birdy
 *
 */
public abstract class ProbabilisticGraphicalRecommender extends ModelRecommender {

	/**
	 * burn-in period
	 */
	protected int burnIn;

	/**
	 * size of statistics
	 */
	protected int numberOfStatistics = 0;

	/**
	 * number of topics
	 */
	protected int numberOfFactors;

	protected int numberOfScores;

	/**
	 * sample lag (if -1 only one sample taken)
	 */
	protected int numberOfSamples;

	/**
	 * setup init member method
	 *
	 * @throws RecommendException
	 *             if error occurs during setting up
	 */
	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		numberOfFactors = configuration.getInteger("recommender.topic.number", 10);
		numberOfScores = scoreIndexes.size();
		burnIn = configuration.getInteger("recommender.pgm.burnin", 100);
		numberOfSamples = configuration.getInteger("recommender.pgm.samplelag", 100);
	}

	@Override
	protected void doPractice() {
		long now = System.currentTimeMillis();
		for (int iter = 1; iter <= numberOfEpoches; iter++) {
			// E-step: infer parameters
			eStep();
			if (logger.isInfoEnabled()) {
				String message = StringUtility.format("eStep time is {}", System.currentTimeMillis() - now);
				now = System.currentTimeMillis();
				logger.info(message);
			}

			// M-step: update hyper-parameters
			mStep();
			if (logger.isInfoEnabled()) {
				String message = StringUtility.format("mStep time is {}", System.currentTimeMillis() - now);
				now = System.currentTimeMillis();
				logger.info(message);
			}
			// get statistics after burn-in
			if ((iter > burnIn) && (iter % numberOfSamples == 0)) {
				readoutParams();
				if (logger.isInfoEnabled()) {
					String message = StringUtility.format("readoutParams time is {}", System.currentTimeMillis() - now);
					now = System.currentTimeMillis();
					logger.info(message);
				}
				estimateParams();
				if (logger.isInfoEnabled()) {
					String message = StringUtility.format("estimateParams time is {}", System.currentTimeMillis() - now);
					now = System.currentTimeMillis();
					logger.info(message);
				}
			}
			if (isConverged(iter) && isConverged) {
				break;
			}
			currentLoss = totalLoss;
		}
		// retrieve posterior probability distributions
		estimateParams();
		if (logger.isInfoEnabled()) {
			String message = StringUtility.format("estimateParams time is {}", System.currentTimeMillis() - now);
			now = System.currentTimeMillis();
			logger.info(message);
		}
	}

	protected boolean isConverged(int iter) {
		return false;
	}

	/**
	 * parameters estimation: used in the training phase
	 */
	protected abstract void eStep();

	/**
	 * update the hyper-parameters
	 */
	protected abstract void mStep();

	/**
	 * read out parameters for each iteration
	 */
	protected void readoutParams() {

	}

	/**
	 * estimate the model parameters
	 */
	protected void estimateParams() {

	}

}
