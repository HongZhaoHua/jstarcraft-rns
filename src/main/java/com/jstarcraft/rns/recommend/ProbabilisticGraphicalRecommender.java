package com.jstarcraft.rns.recommend;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.configure.Configurator;
import com.jstarcraft.rns.recommend.exception.RecommendException;

import it.unimi.dsi.fastutil.floats.Float2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatRBTreeSet;
import it.unimi.dsi.fastutil.floats.FloatSet;

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

    /** 分数索引 (TODO 考虑取消或迁移.本质为连续特征离散化) */
    protected Float2IntLinkedOpenHashMap scoreIndexes;

    protected int numberOfScores;

    /**
     * sample lag (if -1 only one sample taken)
     */
    protected int numberOfSamples;

    /**
     * setup init member method
     *
     * @throws RecommendException if error occurs during setting up
     */
    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        numberOfFactors = configuration.getInteger("recommender.topic.number", 10);
        burnIn = configuration.getInteger("recommender.pgm.burnin", 100);
        numberOfSamples = configuration.getInteger("recommender.pgm.samplelag", 100);

        // TODO 此处会与scoreIndexes一起重构,本质为连续特征离散化.
        FloatSet scores = new FloatRBTreeSet();
        for (MatrixScalar term : scoreMatrix) {
            scores.add(term.getValue());
        }
        scores.remove(0F);
        scoreIndexes = new Float2IntLinkedOpenHashMap();
        int index = 0;
        for (float score : scores) {
            scoreIndexes.put(score, index++);
        }
        numberOfScores = scoreIndexes.size();
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
