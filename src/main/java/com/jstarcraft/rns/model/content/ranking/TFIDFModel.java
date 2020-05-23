package com.jstarcraft.rns.model.content.ranking;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.math.algorithm.text.AbstractTermFrequency;
import com.jstarcraft.ai.math.algorithm.text.InverseDocumentFrequency;
import com.jstarcraft.ai.math.algorithm.text.NaturalInverseDocumentFrequency;
import com.jstarcraft.ai.math.algorithm.text.TermFrequency;
import com.jstarcraft.ai.math.structure.DefaultScalar;
import com.jstarcraft.ai.math.structure.matrix.HashMatrix;
import com.jstarcraft.ai.math.structure.matrix.SparseMatrix;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.HashVector;
import com.jstarcraft.ai.math.structure.vector.MathVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.configuration.Configurator;
import com.jstarcraft.core.utility.Integer2FloatKeyValue;
import com.jstarcraft.core.utility.Neighborhood;
import com.jstarcraft.rns.model.MatrixFactorizationModel;

import it.unimi.dsi.fastutil.ints.Int2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;
import it.unimi.dsi.fastutil.longs.Long2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * 
 * TF-IDF推荐器
 * 
 * @author Birdy
 *
 */
public class TFIDFModel extends MatrixFactorizationModel {

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

    protected String commentField;
    protected int commentDimension;

    protected ArrayVector[] userVectors;
    protected SparseMatrix itemVectors;

//	protected MathCorrelation correlation;

    private class VectorTermFrequency extends AbstractTermFrequency {

        public VectorTermFrequency(MathVector vector) {
            super(new Int2FloatAVLTreeMap(), vector.getElementSize());

            for (VectorScalar scalar : vector) {
                keyValues.put(scalar.getIndex(), scalar.getValue());
            }
        }

    }

    private class DocumentIterator implements Iterator<TermFrequency> {

        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < itemVectors.getRowSize();
        }

        @Override
        public TermFrequency next() {
            MathVector vector = itemVectors.getRowVector(index++);
            VectorTermFrequency termFrequency = new VectorTermFrequency(vector);
            return termFrequency;
        }

    }

    @Override
    public void prepare(Configurator configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);
        int numberOfFeatures = 4096;

        // 特征矩阵
        HashMatrix featureMatrix = new HashMatrix(true, itemSize, numberOfFeatures, new Long2FloatRBTreeMap());
        DataModule featureModel = space.getModule("article");
        String articleField = configuration.getString("data.model.fields.article");
        String featureField = configuration.getString("data.model.fields.feature");
        String degreeField = configuration.getString("data.model.fields.degree");
        int articleDimension = featureModel.getQualityInner(articleField);
        int featureDimension = featureModel.getQualityInner(featureField);
        int degreeDimension = featureModel.getQuantityInner(degreeField);
        for (DataInstance instance : featureModel) {
            int itemIndex = instance.getQualityFeature(articleDimension);
            int featureIndex = instance.getQualityFeature(featureDimension);
            float featureValue = instance.getQuantityFeature(degreeDimension);
            featureMatrix.setValue(itemIndex, featureIndex, featureValue);
        }

        // 物品矩阵
        itemVectors = SparseMatrix.valueOf(itemSize, numberOfFeatures, featureMatrix);
        DocumentIterator iterator = new DocumentIterator();
        Int2FloatSortedMap keyValues = new Int2FloatAVLTreeMap();
        InverseDocumentFrequency inverseDocumentFrequency = new NaturalInverseDocumentFrequency(keyValues, iterator);
        /** k控制着词频饱和度,值越小饱和度变化越快,值越大饱和度变化越慢 */
        float k = 1.2F;
        /** b控制着词频归一化所起的作用,0.0会完全禁用归一化,1.0会完全启用归一化 */
        float b = 0.75F;
        float avgdl = 0F;
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            MathVector itemVector = itemVectors.getRowVector(itemIndex);
            avgdl += itemVector.getElementSize();
        }
        avgdl /= itemSize;
        for (int itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            MathVector itemVector = itemVectors.getRowVector(itemIndex);
            float l = itemVector.getElementSize() / avgdl;
            for (VectorScalar scalar : itemVector) {
                float tf = scalar.getValue();
                float idf = inverseDocumentFrequency.getValue(scalar.getIndex());
                // use BM25
//                scalar.setValue((idf * (k + 1F) * tf) / (k * (1F - b + b * l) + tf));
                // use TF-IDF
                scalar.setValue((idf * tf));
            }
            // 归一化
            itemVector.scaleValues(1F / itemVector.getNorm(2F));
        }

        // 用户矩阵
        userVectors = new ArrayVector[userSize];
        for (int userIndex = 0; userIndex < userSize; userIndex++) {
            MathVector rowVector = scoreMatrix.getRowVector(userIndex);
            HashVector userVector = new HashVector(0L, numberOfFeatures, new Long2FloatAVLTreeMap());
            for (VectorScalar scalar : rowVector) {
                int itemIndex = scalar.getIndex();
                MathVector itemVector = itemVectors.getRowVector(itemIndex);
                for (int position = 0; position < itemVector.getElementSize(); position++) {
                    float value = userVector.getValue(itemVector.getIndex(position));
                    userVector.setValue(itemVector.getIndex(position), Float.isNaN(value) ? itemVector.getValue(position) : value + itemVector.getValue(position));
                }
            }
            userVector.scaleValues(1F / rowVector.getElementSize());
            Neighborhood<Integer2FloatKeyValue> knn = new Neighborhood<Integer2FloatKeyValue>(50, comparator);
            for (int position = 0; position < userVector.getElementSize(); position++) {
                knn.updateNeighbor(new Integer2FloatKeyValue(userVector.getIndex(position), userVector.getValue(position)));
            }
            userVector = new HashVector(0L, numberOfFeatures, new Long2FloatAVLTreeMap());
            Collection<Integer2FloatKeyValue> neighbors = knn.getNeighbors();
            for (Integer2FloatKeyValue neighbor : neighbors) {
                userVector.setValue(neighbor.getKey(), neighbor.getValue());
            }
            userVectors[userIndex] = new ArrayVector(userVector);
        }
    }

    @Override
    protected void doPractice() {
    }

    @Override
    protected float predict(int userIndex, int itemIndex) {
        MathVector userVector = userVectors[userIndex];
        MathVector itemVector = itemVectors.getRowVector(itemIndex);
        return DefaultScalar.getInstance().dotProduct(userVector, itemVector).getValue();
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(predict(userIndex, itemIndex));
    }

}
