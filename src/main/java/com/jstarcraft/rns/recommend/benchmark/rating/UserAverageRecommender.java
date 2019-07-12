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
 * User Average推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
@ModemDefinition(value = { "userDimension", "userMeans" })
public class UserAverageRecommender extends AbstractRecommender {

    /** 用户平均分数 */
    private float[] userMeans;

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        userMeans = new float[numberOfUsers];
    }

    @Override
    protected void doPractice() {
        for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            userMeans[userIndex] = userVector.getElementSize() == 0 ? meanOfScore : userVector.getSum(false) / userVector.getElementSize();
        }
    }

    @Override
    public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        return userMeans[userIndex];
    }

}
