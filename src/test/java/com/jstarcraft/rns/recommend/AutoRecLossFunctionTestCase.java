package com.jstarcraft.rns.recommend;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.ILossFunction;

import com.jstarcraft.ai.math.structure.matrix.Nd4jMatrix;
import com.jstarcraft.ai.model.neuralnetwork.activation.ActivationFunction;
import com.jstarcraft.ai.model.neuralnetwork.loss.LossFunction;
import com.jstarcraft.rns.recommend.collaborative.rating.AutoRecLearner;
import com.jstarcraft.rns.recommend.neuralnetwork.AutoRecLossFunction;

public class AutoRecLossFunctionTestCase extends LossFunctionTestCase {

	@Override
	protected ILossFunction getOldFunction(INDArray masks) {
		return new AutoRecLearner(masks);
	}

	@Override
	protected LossFunction getNewFunction(INDArray masks, ActivationFunction function) {
		return new AutoRecLossFunction(new Nd4jMatrix(masks));
	}

}
