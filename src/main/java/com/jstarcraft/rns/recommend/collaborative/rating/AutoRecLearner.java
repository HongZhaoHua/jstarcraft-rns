package com.jstarcraft.rns.recommend.collaborative.rating;

import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.primitives.Pair;

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
public class AutoRecLearner implements ILossFunction {

	private INDArray maskData;

	AutoRecLearner(INDArray maskData) {
		this.maskData = maskData;
	}

	private INDArray scoreArray(INDArray labels, INDArray preOutput, IActivation activationFn, INDArray mask) {
		INDArray scoreArr;
		INDArray output = activationFn.getActivation(preOutput.dup(), true);
		INDArray yMinusyHat = Transforms.abs(labels.sub(output));
		scoreArr = yMinusyHat.mul(yMinusyHat);
		scoreArr = scoreArr.mul(maskData);

		if (mask != null) {
			scoreArr.muliColumnVector(mask);
		}
		return scoreArr;
	}

	@Override
	public double computeScore(INDArray labels, INDArray preOutput, IActivation activationFn, INDArray mask, boolean average) {
		INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
		double score = scoreArr.sumNumber().doubleValue();

		if (average) {
			score /= scoreArr.size(0);
		}

		return score;
	}

	@Override
	public INDArray computeScoreArray(INDArray labels, INDArray preOutput, IActivation activationFn, INDArray mask) {
		INDArray scoreArr = scoreArray(labels, preOutput, activationFn, mask);
		return scoreArr.sum(1);
	}

	@Override
	public INDArray computeGradient(INDArray labels, INDArray preOutput, IActivation activationFn, INDArray mask) {
		INDArray output = activationFn.getActivation(preOutput.dup(), true);
		INDArray yMinusyHat = labels.sub(output);
		INDArray dldyhat = yMinusyHat.mul(-2);

		INDArray gradients = activationFn.backprop(preOutput.dup(), dldyhat).getFirst();
		gradients = gradients.mul(maskData);
		// multiply with masks, always
		if (mask != null) {
			gradients.muliColumnVector(mask);
		}

		return gradients;
	}

	@Override
	public Pair<Double, INDArray> computeGradientAndScore(INDArray labels, INDArray preOutput, IActivation activationFn, INDArray mask, boolean average) {
		return new Pair<>(computeScore(labels, preOutput, activationFn, mask, average), computeGradient(labels, preOutput, activationFn, mask));
	}

	@Override
	public String toString() {
		return super.toString() + "AutoRecLossFunction";
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return toString();
	}
}
