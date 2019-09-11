package com.jstarcraft.rns.utility;

import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.ai.math.structure.vector.ArrayVector;
import com.jstarcraft.rns.utility.SampleUtility;

public class SampleUtilityTestCase {

    @Test
    public void testBinarySearch() {
        int[] indexes = new int[] { 5, 10, 15 };
        float[] values = new float[] { 5F, 10F, 15F };

        for (float index = 0F; index < 5F; index += 0.5F) {
            Assert.assertEquals(0, SampleUtility.binarySearch(values, 0, values.length - 1, index));
        }

        for (float index = 5F; index < 10F; index += 0.5F) {
            Assert.assertEquals(1, SampleUtility.binarySearch(values, 0, values.length - 1, index));
        }

        for (float index = 10; index < 15F; index += 0.5F) {
            Assert.assertEquals(2, SampleUtility.binarySearch(values, 0, values.length - 1, index));
        }

        ArrayVector vector = new ArrayVector(3, indexes, values);
        for (float index = 0F; index < 5F; index += 0.5F) {
            Assert.assertEquals(0, SampleUtility.binarySearch(vector, 0, values.length - 1, index));
        }

        for (float index = 5F; index < 10F; index += 0.5F) {
            Assert.assertEquals(1, SampleUtility.binarySearch(vector, 0, values.length - 1, index));
        }

        for (float index = 10; index < 15F; index += 0.5F) {
            Assert.assertEquals(2, SampleUtility.binarySearch(vector, 0, values.length - 1, index));
        }
    }

}
