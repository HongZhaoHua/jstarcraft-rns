package com.jstarcraft.rns.recommend.collaborative.rating;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.math.structure.MathCalculator;
import com.jstarcraft.ai.math.structure.matrix.DenseMatrix;
import com.jstarcraft.ai.math.structure.vector.DenseVector;
import com.jstarcraft.ai.math.structure.vector.SparseVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;
import com.jstarcraft.rns.utility.MatrixUtility;

/**
 * 
 * MF ALS推荐器
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
public class MFALSRecommender extends MatrixFactorizationRecommender {

    @Override
    protected void doPractice() {
        DenseVector scoreVector = DenseVector.valueOf(factorSize);
        DenseMatrix inverseMatrix = DenseMatrix.valueOf(factorSize, factorSize);
        DenseMatrix transposeMatrix = DenseMatrix.valueOf(factorSize, factorSize);
        DenseMatrix copyMatrix = DenseMatrix.valueOf(factorSize, factorSize);
        // TODO 可以考虑只获取有评分的用户?
        for (int epocheIndex = 0; epocheIndex < epocheSize; epocheIndex++) {
            // fix item matrix M, solve user matrix U
            for (int userIndex = 0; userIndex < userSize; userIndex++) {
                // number of items rated by user userIdx
                SparseVector userVector = scoreMatrix.getRowVector(userIndex);
                int size = userVector.getElementSize();
                if (size == 0) {
                    continue;
                }
                // TODO 此处应该避免valueOf
                DenseMatrix rateMatrix = DenseMatrix.valueOf(size, factorSize);
                DenseVector rateVector = DenseVector.valueOf(size);
                int index = 0;
                for (VectorScalar term : userVector) {
                    // step 1:
                    int itemIndex = term.getIndex();
                    rateMatrix.getRowVector(index).copyVector(itemFactors.getRowVector(itemIndex));

                    // step 2:
                    // ratings of this userIdx
                    rateVector.setValue(index++, term.getValue());
                }

                // step 3: the updated user matrix wrt user j
                DenseMatrix matrix = transposeMatrix;
                matrix.dotProduct(rateMatrix, true, rateMatrix, false, MathCalculator.SERIAL);
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    matrix.shiftValue(factorIndex, factorIndex, userRegularization * size);
                }
                scoreVector.dotProduct(rateMatrix, true, rateVector, MathCalculator.SERIAL);
                userFactors.getRowVector(userIndex).dotProduct(MatrixUtility.inverse(matrix, copyMatrix, inverseMatrix), false, scoreVector, MathCalculator.SERIAL);
            }

            // TODO 可以考虑只获取有评分的条目?
            // fix user matrix U, solve item matrix M
            for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
                // latent factor of users that have rated item itemIdx
                // number of users rate item j
                SparseVector itemVector = scoreMatrix.getColumnVector(itemIndex);
                int size = itemVector.getElementSize();
                if (size == 0) {
                    continue;
                }

                // TODO 此处应该避免valueOf
                DenseMatrix rateMatrix = DenseMatrix.valueOf(size, factorSize);
                DenseVector rateVector = DenseVector.valueOf(size);
                int index = 0;
                for (VectorScalar term : itemVector) {
                    // step 1:
                    int userIndex = term.getIndex();
                    rateMatrix.getRowVector(index).copyVector(userFactors.getRowVector(userIndex));

                    // step 2:
                    // ratings of this item
                    rateVector.setValue(index++, term.getValue());
                }

                // step 3: the updated item matrix wrt item j
                DenseMatrix matrix = transposeMatrix;
                matrix.dotProduct(rateMatrix, true, rateMatrix, false, MathCalculator.SERIAL);
                for (int factorIndex = 0; factorIndex < factorSize; factorIndex++) {
                    matrix.shiftValue(factorIndex, factorIndex, itemRegularization * size);
                }
                scoreVector.dotProduct(rateMatrix, true, rateVector, MathCalculator.SERIAL);
                itemFactors.getRowVector(itemIndex).dotProduct(MatrixUtility.inverse(matrix, copyMatrix, inverseMatrix), false, scoreVector, MathCalculator.SERIAL);
            }
        }
    }

}
