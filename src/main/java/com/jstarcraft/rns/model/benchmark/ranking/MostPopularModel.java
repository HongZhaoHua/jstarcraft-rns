package com.jstarcraft.rns.model.benchmark.ranking;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.rns.model.AbstractModel;

/**
 * 
 * Most Popular推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "itemDimension", "populars" })
public class MostPopularModel extends AbstractModel {

    private int[] populars;

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        populars = new int[itemSize];
    }

    @Override
    protected void doPractice() {
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            populars[itemIndex] = scoreMatrix.getColumnScope(itemIndex);
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(populars[itemIndex]);
    }

}
