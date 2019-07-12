package com.jstarcraft.ai.evaluate;

import java.util.ArrayList;
import java.util.List;

import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.ai.utility.Integer2FloatKeyValue;
import com.jstarcraft.core.utility.RandomUtility;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.floats.FloatList;

public abstract class AbstractRatingEvaluatorTestCase extends AbstractEvaluatorTestCase<FloatCollection> {

    @Override
    protected FloatCollection check(MathVector vector) {
        FloatList scoreList = new FloatArrayList(vector.getElementSize());
        for (VectorScalar scalar : vector) {
            scoreList.add(scalar.getValue());
        }
        return scoreList;
    }

    @Override
    protected List<Integer2FloatKeyValue> recommend(MathVector vector) {
        List<Integer2FloatKeyValue> recommendList = new ArrayList<>(vector.getElementSize());
        for (VectorScalar scalar : vector) {
            if (RandomUtility.randomFloat(1F) < 0.5F) {
                recommendList.add(new Integer2FloatKeyValue(scalar.getIndex(), scalar.getValue()));
            } else {
                recommendList.add(new Integer2FloatKeyValue(scalar.getIndex(), scalar.getValue() * 0.5F));
            }
        }
        return recommendList;
    }

}
