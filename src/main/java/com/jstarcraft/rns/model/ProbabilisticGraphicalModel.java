package com.jstarcraft.rns.model;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.matrix.MatrixScalar;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.model.exception.RecommendException;

import it.unimi.dsi.fastutil.floats.Float2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.floats.FloatRBTreeSet;
import it.unimi.dsi.fastutil.floats.FloatSet;

/**
 * 概率图推荐器
 * 
 * @author Birdy
 *
 */
public abstract class ProbabilisticGraphicalModel extends EpocheModel {

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
    protected int factorSize;

    /** 分数索引 (TODO 考虑取消或迁移.本质为连续特征离散化) */
    protected Float2IntLinkedOpenHashMap scoreIndexes;

    protected int scoreSize;

    /**
     * sample lag (if -1 only one sample taken)
     */
    protected int sampleSize;

    /**
     * setup init member method
     *
     * @throws RecommendException if error occurs during setting up
     */
    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        factorSize = configuration.getInteger("recommender.topic.number", 10);
        burnIn = configuration.getInteger("recommender.pgm.burnin", 100);
        sampleSize = configuration.getInteger("recommender.pgm.samplelag", 100);

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
        scoreSize = scoreIndexes.size();
    }

    @Override
    protected void doPractice() {
        long now = System.currentTimeMillis();
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
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
            if ((epocheIndex > burnIn) && (epocheIndex % sampleSize == 0)) {
                readoutParameters();
                if (logger.isInfoEnabled()) {
                    String message = StringUtility.format("readoutParams time is {}", System.currentTimeMillis() - now);
                    now = System.currentTimeMillis();
                    logger.info(message);
                }
                estimateParameters();
                if (logger.isInfoEnabled()) {
                    String message = StringUtility.format("estimateParams time is {}", System.currentTimeMillis() - now);
                    now = System.currentTimeMillis();
                    logger.info(message);
                }
            }
            if (isConverged(epocheIndex) && isConverged) {
                break;
            }
            currentError = totalError;
        }
        // retrieve posterior probability distributions
        estimateParameters();
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
    protected void readoutParameters() {

    }

    /**
     * estimate the model parameters
     */
    protected void estimateParameters() {

    }

}
