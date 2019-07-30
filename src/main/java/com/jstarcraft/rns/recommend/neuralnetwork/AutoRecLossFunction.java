package com.jstarcraft.rns.recommend.neuralnetwork;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import com.jstarcraft.ai.math.structure.matrix.MathMatrix;
import com.jstarcraft.ai.math.structure.matrix.Nd4jMatrix;
import com.jstarcraft.ai.model.neuralnetwork.loss.LossFunction;

/**
 * 
 * AutoRec学习器
 * 
 * <pre>
 * AutoRec: Autoencoders Meet Collaborative Filtering
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class AutoRecLossFunction implements LossFunction {

    private Nd4jMatrix maskData;

    public AutoRecLossFunction(Nd4jMatrix maskData) {
        this.maskData = maskData;
    }

    @Override
    public float computeScore(MathMatrix tests, MathMatrix trains, MathMatrix masks) {
        float score = 0F;
        if (tests instanceof Nd4jMatrix && trains instanceof Nd4jMatrix && maskData instanceof Nd4jMatrix) {
            INDArray testArray = Nd4jMatrix.class.cast(tests).getArray();
            INDArray trainArray = Nd4jMatrix.class.cast(trains).getArray();
            INDArray scoreArray = trainArray.sub(testArray);
            INDArray maskArray = Nd4jMatrix.class.cast(maskData).getArray();
            scoreArray.muli(scoreArray);
            scoreArray.muli(maskArray);
            score = scoreArray.sumNumber().floatValue();
        }
        return score;
    }

    @Override
    public void computeGradient(MathMatrix tests, MathMatrix trains, MathMatrix masks, MathMatrix gradients) {
        if (tests instanceof Nd4jMatrix && trains instanceof Nd4jMatrix && maskData instanceof Nd4jMatrix) {
            INDArray testArray = Nd4jMatrix.class.cast(tests).getArray();
            INDArray trainArray = Nd4jMatrix.class.cast(trains).getArray();
            INDArray gradientArray = Nd4jMatrix.class.cast(gradients).getArray();
            INDArray maskArray = Nd4jMatrix.class.cast(maskData).getArray();
            trainArray.sub(testArray, gradientArray);
            gradientArray.muli(2F);
            gradientArray.muli(maskArray);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        } else {
            AutoRecLossFunction that = (AutoRecLossFunction) object;
            EqualsBuilder equal = new EqualsBuilder();
            equal.append(this.maskData, that.maskData);
            return equal.isEquals();
        }
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder();
        hash.append(maskData);
        return hash.toHashCode();
    }

    @Override
    public String toString() {
        return "AutoRecLossFunction(maskData=" + maskData + ")";
    }
}
