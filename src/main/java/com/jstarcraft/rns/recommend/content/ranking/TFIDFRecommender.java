package com.jstarcraft.rns.recommend.content.ranking;

import java.util.Iterator;

import com.jstarcraft.ai.data.DataInstance;
import com.jstarcraft.ai.data.DataModule;
import com.jstarcraft.ai.data.DataSpace;
import com.jstarcraft.ai.data.attribute.MemoryQualityAttribute;
import com.jstarcraft.ai.math.algorithm.similarity.Similarity;
import com.jstarcraft.ai.math.algorithm.text.InverseDocumentFrequency;
import com.jstarcraft.ai.math.algorithm.text.NaturalInverseDocumentFrequency;
import com.jstarcraft.ai.math.algorithm.text.NaturalTermFrequency;
import com.jstarcraft.ai.math.algorithm.text.TermFrequency;
import com.jstarcraft.ai.math.structure.matrix.RowArrayMatrix;
import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.ai.math.structure.vector.VectorScalar;
import com.jstarcraft.core.common.reflection.ReflectionUtility;
import com.jstarcraft.rns.configure.Configuration;
import com.jstarcraft.rns.recommend.MatrixFactorizationRecommender;

import it.unimi.dsi.fastutil.ints.Int2FloatAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * 
 * TF IDF推荐器
 * 
 * <pre>
 * 参考LibRec团队
 * </pre>
 * 
 * @author Birdy
 *
 */
public class TFIDFRecommender extends MatrixFactorizationRecommender {

    protected String commentField;
    protected int commentDimension;

    protected RowArrayMatrix userMatrix;
    protected RowArrayMatrix itemMatrix;

    protected Similarity similarity;

    protected float scale;

    private class DocumentIterator implements Iterator<TermFrequency> {

        private Iterator<DataInstance> iterator;

        private Object[] documentValues;

        private Object2IntMap<String> worldIndexes = new Object2IntOpenHashMap<>();
        private Int2FloatSortedMap keyValues = new Int2FloatAVLTreeMap();
        private Int2FloatSortedMap factors = new Int2FloatAVLTreeMap();
        private int[] userCounts = new int[numberOfUsers];
        private int[] itemCounts = new int[numberOfItems];
        private ArrayVector[] userVectors = new ArrayVector[numberOfUsers];
        private ArrayVector[] itemVectors = new ArrayVector[numberOfItems];

        private DocumentIterator(Iterator<DataInstance> iterator, Object[] documentValues) {
            this.iterator = iterator;
            this.documentValues = documentValues;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public TermFrequency next() {
            DataInstance sample = iterator.next();
            int contentIndex = sample.getQualityFeature(commentDimension);
            String data = (String) documentValues[contentIndex];
            String[] words = data.isEmpty() ? new String[0] : data.split(":");
            int[] document = new int[words.length];
            int cursor = 0;
            for (String word : words) {
                int wordIndex = worldIndexes.getOrDefault(word, -1);
                if (wordIndex == -1) {
                    wordIndex = worldIndexes.size();
                    worldIndexes.put(word, wordIndex);
                }
                document[cursor++] = wordIndex;
            }
            keyValues.clear();
            NaturalTermFrequency termFrequency = new NaturalTermFrequency(keyValues, document);

            int userIndex = sample.getQualityFeature(userDimension);
            int itemIndex = sample.getQualityFeature(itemDimension);
            userCounts[userIndex]++;
            itemCounts[itemIndex]++;

            {
                ArrayVector userVector = userVectors[userIndex];
                if (userVector != null) {
                    for (VectorScalar term : userVector) {
                        float value = factors.getOrDefault(term.getIndex(), 0F);
                        factors.put(term.getIndex(), value + term.getValue());
                    }
                }
                for (Int2FloatMap.Entry term : keyValues.int2FloatEntrySet()) {
                    float value = factors.getOrDefault(term.getIntKey(), 0F);
                    factors.put(term.getIntKey(), value + term.getFloatValue());
                }
                int capacity = factors.size();
                int[] indexes = new int[capacity];
                float[] values = new float[capacity];
                int position = 0;
                for (Int2FloatMap.Entry term : factors.int2FloatEntrySet()) {
                    indexes[position] = term.getIntKey();
                    values[position] = term.getFloatValue();
                    position++;
                }
                userVectors[userIndex] = new ArrayVector(capacity, indexes, values);
                factors.clear();
            }
            {
                ArrayVector itemVector = itemVectors[itemIndex];
                if (itemVector != null) {
                    for (VectorScalar term : itemVector) {
                        float value = factors.getOrDefault(term.getIndex(), 0F);
                        factors.put(term.getIndex(), value + term.getValue());
                    }
                }
                for (Int2FloatMap.Entry term : keyValues.int2FloatEntrySet()) {
                    float value = factors.getOrDefault(term.getIntKey(), 0F);
                    factors.put(term.getIntKey(), value + term.getFloatValue());
                }
                int capacity = factors.size();
                int[] indexes = new int[capacity];
                float[] values = new float[capacity];
                int position = 0;
                for (Int2FloatMap.Entry term : factors.int2FloatEntrySet()) {
                    indexes[position] = term.getIntKey();
                    values[position] = term.getFloatValue();
                    position++;
                }
                itemVectors[itemIndex] = new ArrayVector(capacity, indexes, values);
                factors.clear();
            }

            return termFrequency;
        }

        public int[] getUserCounts() {
            return userCounts;
        }

        public int[] getItemCounts() {
            return itemCounts;
        }

        public ArrayVector[] getUserVectors() {
            return userVectors;
        }

        public ArrayVector[] getItemVectors() {
            return itemVectors;
        }

        public int getNumberOfWords() {
            return worldIndexes.size();
        }

    }

    @Override
    public void prepare(Configuration configuration, DataModule model, DataSpace space) {
        super.prepare(configuration, model, space);

        try {
            Class<Similarity> similarityClass = (Class<Similarity>) Class.forName(configuration.getString("recommender.similarity.class"));
            similarity = ReflectionUtility.getInstance(similarityClass);
            scale = configuration.getFloat("recommender.similarity.shrinkage");
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        commentField = configuration.getString("data.model.fields.comment");
        commentDimension = model.getQualityInner(commentField);
        MemoryQualityAttribute attribute = (MemoryQualityAttribute) space.getQualityAttribute(commentField);
        Object[] documentValues = attribute.getDatas();

        DocumentIterator iterator = new DocumentIterator(model.iterator(), documentValues);
        Int2FloatSortedMap keyValues = new Int2FloatAVLTreeMap();
        InverseDocumentFrequency inverseDocumentFrequency = new NaturalInverseDocumentFrequency(keyValues, iterator);

        int[] userCounts = iterator.getUserCounts();
        int[] itemCounts = iterator.getItemCounts();
        ArrayVector[] userVectors = iterator.getUserVectors();
        ArrayVector[] itemVectors = iterator.getItemVectors();
        ArrayVector emptyVector = new ArrayVector(0, new int[] {}, new float[] {});
        for (int userIndex = 0; userIndex < numberOfUsers; userIndex++) {
            ArrayVector userVector = userVectors[userIndex];
            if (userVector == null) {
                userVectors[userIndex] = emptyVector;
            } else {
                for (VectorScalar term : userVector) {
                    term.scaleValue(inverseDocumentFrequency.getValue(term.getIndex()) / userCounts[userIndex]);
                }
            }
        }
        for (int itemIndex = 0; itemIndex < numberOfItems; itemIndex++) {
            ArrayVector itemVector = itemVectors[itemIndex];
            if (itemVector == null) {
                itemVectors[itemIndex] = emptyVector;
            } else {
                for (VectorScalar term : itemVector) {
                    term.scaleValue(inverseDocumentFrequency.getValue(term.getIndex()) / itemCounts[itemIndex]);
                }
            }
        }
        userMatrix = RowArrayMatrix.valueOf(iterator.getNumberOfWords(), userVectors);
        itemMatrix = RowArrayMatrix.valueOf(iterator.getNumberOfWords(), itemVectors);
    }

    @Override
    protected void doPractice() {
    }

    @Override
    protected float predict(int userIndex, int itemIndex) {
        ArrayVector userVector = userMatrix.getRowVector(userIndex);
        ArrayVector itemVector = itemMatrix.getRowVector(itemIndex);
        return similarity.getCorrelation(userVector, itemVector, scale);
    }

    @Override
    public void predict(DataInstance instance) {
        int userIndex = instance.getQualityFeature(userDimension);
        int itemIndex = instance.getQualityFeature(itemDimension);
        instance.setQuantityMark(predict(userIndex, itemIndex));
    }

}
