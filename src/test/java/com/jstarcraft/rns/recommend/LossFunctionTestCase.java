package com.jstarcraft.rns.recommend;

import java.util.LinkedList;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.activations.impl.ActivationSigmoid;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.ILossFunction;

import com.jstarcraft.ai.environment.EnvironmentContext;
import com.jstarcraft.ai.environment.EnvironmentFactory;
import com.jstarcraft.ai.math.MathUtility;
import com.jstarcraft.ai.math.structure.matrix.MathMatrix;
import com.jstarcraft.ai.math.structure.matrix.Nd4jMatrix;
import com.jstarcraft.ai.model.neuralnetwork.activation.ActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.activation.SigmoidActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.loss.LossFunction;
import com.jstarcraft.core.utility.KeyValue;

public abstract class LossFunctionTestCase {

    protected static Nd4jMatrix getMatrix(INDArray array) {
        return new Nd4jMatrix(array);
    }

    protected static boolean equalMatrix(MathMatrix matrix, INDArray array) {
        for (int row = 0; row < matrix.getRowSize(); row++) {
            for (int column = 0; column < matrix.getColumnSize(); column++) {
                if (Math.abs(matrix.getValue(row, column) - array.getFloat(row, column)) > MathUtility.EPSILON) {
                    return false;
                }
            }
        }
        return true;
    }

    protected abstract ILossFunction getOldFunction(INDArray masks);

    protected abstract LossFunction getNewFunction(INDArray masks, ActivationFunction function);

    @Test
    public void testScore() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            LinkedList<KeyValue<IActivation, ActivationFunction>> activetionList = new LinkedList<>();
            activetionList.add(new KeyValue<>(new ActivationSigmoid(), new SigmoidActivationFunction()));
//            activetionList.add(new KeyValue<>(new ActivationSoftmax(), new SoftMaxActivationFunction()));
            for (KeyValue<IActivation, ActivationFunction> keyValue : activetionList) {
                INDArray array = Nd4j.linspace(-2.5D, 2.0D, 10).reshape(5, 2);
                INDArray marks = Nd4j.create(new double[] { 0D, 1D, 0D, 1D, 0D, 1D, 0D, 1D, 0D, 1D }).reshape(5, 2);
                ILossFunction oldFunction = getOldFunction(marks);
                double value = oldFunction.computeScore(marks, array.dup(), keyValue.getKey(), null, false);

                Nd4jMatrix input = getMatrix(array.dup());
                Nd4jMatrix output = new Nd4jMatrix(Nd4j.zeros(input.getRowSize(), input.getColumnSize()));
                ActivationFunction function = keyValue.getValue();
                function.forward(input, output);
                LossFunction newFunction = getNewFunction(marks, function);
                newFunction.doCache(getMatrix(marks), output);
                double score = newFunction.computeScore(getMatrix(marks), output, null);

                System.out.println(value);
                System.out.println(score);

                if (Math.abs(value - score) > MathUtility.EPSILON) {
                    Assert.fail();
                }
            }
        });
        task.get();
    }

    @Test
    public void testGradient() throws Exception {
        EnvironmentContext context = EnvironmentFactory.getContext();
        Future<?> task = context.doTask(() -> {
            LinkedList<KeyValue<IActivation, ActivationFunction>> activetionList = new LinkedList<>();
            activetionList.add(new KeyValue<>(new ActivationSigmoid(), new SigmoidActivationFunction()));
//            activetionList.add(new KeyValue<>(new ActivationSoftmax(), new SoftMaxActivationFunction()));
            for (KeyValue<IActivation, ActivationFunction> keyValue : activetionList) {
                INDArray array = Nd4j.linspace(-2.5D, 2.0D, 10).reshape(5, 2);
                INDArray marks = Nd4j.create(new double[] { 0D, 1D, 0D, 1D, 0D, 1D, 0D, 1D, 0D, 1D }).reshape(5, 2);
                ILossFunction oldFunction = getOldFunction(marks);
                INDArray value = oldFunction.computeGradient(marks, array.dup(), keyValue.getKey(), null);

                Nd4jMatrix input = getMatrix(array.dup());
                Nd4jMatrix output = new Nd4jMatrix(Nd4j.zeros(input.getRowSize(), input.getColumnSize()));
                ActivationFunction function = keyValue.getValue();
                function.forward(input, output);
                Nd4jMatrix gradient = new Nd4jMatrix(Nd4j.zeros(input.getRowSize(), input.getColumnSize()));
                LossFunction newFunction = getNewFunction(marks, function);
                newFunction.doCache(getMatrix(marks), output);
                newFunction.computeGradient(getMatrix(marks), output, null, gradient);
                function.backward(input, gradient, output);
                System.out.println(value);
                System.out.println(output);
                Assert.assertTrue(equalMatrix(output, value));
            }
        });
        task.get();
    }

}
