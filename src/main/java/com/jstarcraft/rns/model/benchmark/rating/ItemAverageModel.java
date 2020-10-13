package com.jstarcraft.rns.model.benchmark.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.core.common.option.Option;
import com.jstarcraft.rns.model.AbstractModel;

/**
 * 
 * Item Average推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "itemDimension", "itemMeans" })
public class ItemAverageModel extends AbstractModel {

    /** 物品平均分数 */
    private float[] itemMeans;

    @Override
    public void prepare(Option configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        itemMeans = new float[itemSize];
    }

    @Override
    protected void doPractice() {
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
            itemMeans[itemIndex] = itemVector.getElementSize() == 0 ? meanScore : itemVector.getSum(false) / itemVector.getElementSize();
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(itemMeans[itemIndex]);
    }

}
