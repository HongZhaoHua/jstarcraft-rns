package com.jstarcraft.rns.model.collaborative;

import java.util.Comparator;

import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.correlation.Correlation;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.Integer2FloatKeyValue;
import com.jstarcraft.rns.model.AbstractModel;
import com.jstarcraft.rns.utility.Knn;

/**
 * 
 * User KNN推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public abstract class UserKNNModel extends AbstractModel {

    /** 邻居数量 */
    private int neighborSize;

    protected DenseVector userMeans;

    /**
     * user's nearest neighbors for kNN > 0
     */
    protected MathVector[] userNeighbors;

    protected SparseVector[] userVectors;

    protected SparseVector[] itemVectors;

    private Comparator<Integer2FloatKeyValue> comparator = new Comparator<Integer2FloatKeyValue>() {

        @Override
        public int compare(Integer2FloatKeyValue left, Integer2FloatKeyValue right) {
            int compare = -(Float.compare(left.getValue(), right.getValue()));
            if (compare == 0) {
                compare = Integer.compare(left.getKey(), right.getKey());
            }
            return compare;
        }

    };

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        neighborSize = configuration.getInteger("recommender.neighbors.knn.number");
        // TODO 设置容量
        userNeighbors = new MathVector[userSize];
        Knn[] knns = new Knn[userSize];
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            knns[userIndex] = new Knn(neighborSize, comparator);
        }
        // TODO 修改为配置枚举
        try {
            Class<Correlation> correlationClass = (Class<Correlation>) Class.forName(configuration.getString("recommender.correlation.class"));
            Correlation correlation = ReflectionUtility.getInstance(correlationClass);
            correlation.calculateCoefficients(scoreMatrix, false, (leftIndex, rightIndex, coefficient) -> {
                if (leftIndex == rightIndex) {
                    return;
                }
                // 忽略相似度为0的物品
                if (coefficient == 0F) {
                    return;
                }
                knns[leftIndex].updateNeighbor(rightIndex, coefficient);
                knns[rightIndex].updateNeighbor(leftIndex, coefficient);
            });
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            userNeighbors[userIndex] = knns[userIndex].getNeighbors();
        }

        userMeans = DenseVector.valueOf(userSize);

        userVectors = new SparseVector[userSize];
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            userVectors[userIndex] = scoreMatrix.getRowVector(userIndex);
        }

        itemVectors = new SparseVector[itemSize];
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            itemVectors[itemIndex] = scoreMatrix.getColumnVector(itemIndex);
        }
    }

    @Override
    protected void doPractice() {
        meanScore = scoreMatrix.getSum(false) / scoreMatrix.getElementSize();
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            SparseVector userVector = scoreMatrix.getRowVector(userIndex);
            userMeans.setValue(userIndex, userVector.getElementSize() > 0 ? userVector.getSum(false) / userVector.getElementSize() : meanScore);
        }
    }

}
