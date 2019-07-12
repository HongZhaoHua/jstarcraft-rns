package com.jstarcraft.ai.evaluate;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;
import com.jstarcraft.core.utility.RandomUtility;

import it.unimi.dsi.fastutil.ints.Int2FloatRBTreeMap;

public abstract class AbstractEvaluatorTestCase<T> {

    protected abstract Evaluator<T> getEvaluator(SparseMatrix featureMatrix);

    protected abstract float getMeasure();

    @Test
    public void test() throws Exception {
        RandomUtility.setSeed(0L);
        int rowSize = 1000;
        int columnSize = 1000;
        HashMatrix featureTable = new HashMatrix(true, rowSize, columnSize, new Int2FloatRBTreeMap());
        for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
            for (int columnIndex = 0; columnIndex < columnSize; columnIndex++) {
                if (RandomUtility.randomFloat(1F) < 0.5F) {
                    featureTable.setValue(rowIndex, columnIndex, RandomUtility.randomFloat(1F));
                }
            }
        }
        SparseMatrix featureMatrix = SparseMatrix.valueOf(rowSize, columnSize, featureTable);
        Evaluator<T> evaluator = getEvaluator(featureMatrix);
        Integer2FloatKeyValue sum = evaluate(evaluator, featureMatrix);
        Assert.assertThat(sum.getValue() / sum.getKey(), CoreMatchers.equalTo(getMeasure()));
    }

    protected abstract T check(MathVector vector);

    protected abstract List<Integer2FloatKeyValue> recommend(MathVector vector);

    private Integer2FloatKeyValue evaluate(Evaluator<T> evaluator, SparseMatrix featureMatrix) {
        Integer2FloatKeyValue sum = new Integer2FloatKeyValue(0, 0F);
        for (int index = 0, size = featureMatrix.getRowSize(); index < size; index++) {
            MathVector vector = featureMatrix.getRowVector(index);
            // 训练映射
            T checkCollection = check(vector);
            // 推荐列表
            List<Integer2FloatKeyValue> recommendList = recommend(vector);
            // 测量列表
            Integer2FloatKeyValue measure = evaluator.evaluate(checkCollection, recommendList);
            sum.setKey(sum.getKey() + measure.getKey());
            sum.setValue(sum.getValue() + measure.getValue());
        }
        return sum;
    }

}
