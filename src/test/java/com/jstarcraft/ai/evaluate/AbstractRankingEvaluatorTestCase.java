package com.jstarcraft.ai.evaluate;

import java.util.ArrayList;
import java.util.List;

import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;
import com.jstarcraft.core.utility.RandomUtility;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public abstract class AbstractRankingEvaluatorTestCase extends AbstractEvaluatorTestCase<IntCollection> {

    @Override
    protected IntCollection check(MathVector vector) {
        IntSet itemSet = new IntOpenHashSet();
        for (VectorScalar scalar : vector) {
            if (RandomUtility.randomFloat(1F) < 0.5F) {
                itemSet.add(scalar.getIndex());
            }
        }
        return itemSet;
    }

    @Override
    protected List<Integer2FloatKeyValue> recommend(MathVector vector) {
        List<Integer2FloatKeyValue> recommendList = new ArrayList<>(vector.getElementSize());
        for (VectorScalar scalar : vector) {
            if (RandomUtility.randomFloat(1F) < 0.5F) {
                recommendList.add(new Integer2FloatKeyValue(scalar.getIndex(), scalar.getValue()));
            }
        }
        return recommendList;
    }

}
