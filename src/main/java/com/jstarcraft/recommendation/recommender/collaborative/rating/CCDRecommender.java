package com.jstarcraft.recommendation.recommender.collaborative.rating;

import java.util.Date;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.recommendation.configurator.Configuration;
import com.jstarcraft.recommendation.recommender.MatrixFactorizationRecommender;

/**
 * 
 * CCD推荐器
 * 
 * <pre>
 * Large-Scale Parallel Collaborative Filtering for the Netflix Prize
 * http://www.hpl.hp.com/personal/Robert_Schreiber/papers/2008%20AAIM%20Netflix/
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class CCDRecommender extends MatrixFactorizationRecommender {

	@Override
	public void prepare(Configuration configuration, DataModule model, DataSpace space) {
		super.prepare(configuration, model, space);
		userFactors = DenseMatrix.valueOf(numberOfUsers, numberOfFactors);
		userFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
		itemFactors = DenseMatrix.valueOf(numberOfItems, numberOfFactors);
		itemFactors.iterateElement(MathCalculator.SERIAL, (scalar) -> {
			scalar.setValue(distribution.sample().floatValue());
		});
	}

	@Override
	protected void doPractice() {
		for (int iterationStep = 1; iterationStep <= numberOfEpoches; iterationStep++) {
			for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
				SparseVector userVector = trainMatrix.getRowVector(userIndex);
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float userFactor = 0F;
					float numerator = 0F;
					float denominator = 0F;
					for (VectorScalar term : userVector) {
						int itemIndex = term.getIndex();
						numerator += (term.getValue() + userFactors.getValue(userIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex)) * itemFactors.getValue(itemIndex, factorIndex);
						denominator += itemFactors.getValue(itemIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex);
					}
					userFactor = numerator / (denominator + userRegularization);
					for (VectorScalar term : userVector) {
						int itemIndex = term.getIndex();
						term.setValue(term.getValue() - (userFactor - userFactors.getValue(userIndex, factorIndex)) * itemFactors.getValue(itemIndex, factorIndex));
					}
					userFactors.setValue(userIndex, factorIndex, userFactor);
				}
			}
			for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
				SparseVector itemVector = trainMatrix.getColumnVector(itemIndex);
				for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
					float itemFactor = 0F;
					float numerator = 0F;
					float denominator = 0F;
					for (VectorScalar term : itemVector) {
						int userIndex = term.getIndex();
						numerator += (term.getValue() + userFactors.getValue(userIndex, factorIndex) * itemFactors.getValue(itemIndex, factorIndex)) * userFactors.getValue(userIndex, factorIndex);
						denominator += userFactors.getValue(userIndex, factorIndex) * userFactors.getValue(userIndex, factorIndex);
					}
					itemFactor = numerator / (denominator + itemRegularization);
					for (VectorScalar term : itemVector) {
						int userIndex = term.getIndex();
						term.setValue(term.getValue() - (itemFactor - itemFactors.getValue(itemIndex, factorIndex)) * userFactors.getValue(userIndex, factorIndex));
					}
					itemFactors.setValue(itemIndex, factorIndex, itemFactor);
				}
			}
			logger.info(StringUtility.format("{} runs at iter {}/{} {}", this.getClass().getSimpleName(), iterationStep, numberOfEpoches, new Date()));
		}
	}

	@Override
	public float predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
		DefaultScalar scalar = DefaultScalar.getInstance();
		float score = scalar.dotProduct(userFactors.getRowVector(userIndex), itemFactors.getRowVector(itemIndex)).getValue();
		if (score == 0F) {
			score = meanOfScore;
		} else if (score > maximumOfScore) {
			score = maximumOfScore;
		} else if (score < minimumOfScore) {
			score = minimumOfScore;
		}
		return score;
	}

}
