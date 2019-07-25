package com.jstarcraft.rns.recommend.benchmark.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.modem.ModemDefinition;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.AbstractRecommender;

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
public class ItemAverageRecommender extends AbstractRecommender {

    /** 物品平均分数 */
    private float[] itemMeans;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        itemMeans = new float[numberOfItems];
    }

    @Override
    protected void doPractice() {
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
            itemMeans[itemIndex] = itemVector.getElementSize() == 0 ? meanOfScore : itemVector.getSum(false) / itemVector.getElementSize();
        }
    }

    @Override
    public void predict(DataInstance instance) {
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(itemMeans[itemIndex]);
    }

}
