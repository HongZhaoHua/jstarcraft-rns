package com.jstarcraft.rns.recommend.benchmark.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.recommend.AbstractRecommender;

/**
 * 
 * Constant Guess推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "constant" })
public class ConstantGuessRecommender extends AbstractRecommender {

    private float constant;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        // 默认使用最高最低分的平均值
        constant = (minimumScore + maximumScore) / 2F;
        // TODO 支持配置分数
        constant = configuration.getFloat("recommend.constant-guess.score", constant);
    }

    @Override
    protected void doPractice() {
    }

    @Override
    public void predict(DataInstance instance) {
        instance.setQuantityMark(constant);
    }

}
