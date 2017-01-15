/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.eu.qualimaster.monitoring.profiling;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.Assert;
import org.junit.Test;

import static eu.qualimaster.monitoring.profiling.predictors.Utils.*;

/**
 * Tests the utility methods.
 * 
 * @author Holger Eichelberger
 */
public class UtilsTest {

    /**
     * Tests the parse double method converting back from math3 textual formatting.
     */
    @Test
    public void testParseDouble() {
        Assert.assertEquals(parseDouble("0"), 0.0, 0.005);
        Assert.assertEquals(parseDouble("0.1"), 0.1, 0.005);
        Assert.assertEquals(parseDouble("103,050.4"), 103050.4, 0.005);
        Assert.assertEquals(parseDouble("103.050.4"), 103050.4, 0.005);
        try {
            parseDouble("aaa");
            Assert.fail("No exception");
        } catch (NumberFormatException e) {
        }
    }

    /**
     * Tests writing and parsing matrices.
     */
    @Test
    public void writeParseMatrix() {
        double[][] data = new double[][] {
            {1, 10.2, -8.2, 0.2, 2300410.1, 1243}, 
            {0, 0.1, 0.2, -10.5, 101203450.4, -1245}};
        assertMatrix(data);

        data = new double[][] {{1}, {0}};
        assertMatrix(data);
     
        // empty matrix not allowed!
    }

    /**
     * Asserts turning <code>data</code> into a vector, a string and beck again.
     * 
     * @param data the data
     */
    private void assertMatrix(double[][] data) {
        RealMatrix mat = MatrixUtils.createRealMatrix(data);
        String s = matrixToString(mat);
        System.out.println("Matrix: " + s); 
        RealMatrix m = stringToMatrix(s);
        Assert.assertEquals(mat, m);
    }

    /**
     * Tests writing and parsing vectors.
     */
    @Test
    public void testWriteParseVector() {
        double[] data = new double[] {0, 0.1, -0.2, 10.5, 101203450.4, 1245};
        assertVector(data);

        data = new double[] {0.3};
        assertVector(data);

        data = new double[] {};
        assertVector(data);
    }
    
    /**
     * Asserts turning <code>data</code> into a vector, a string and beck again.
     * 
     * @param data the data
     */
    private void assertVector(double[] data) {
        RealVector vec = MatrixUtils.createRealVector(data);
        String s = vectorToString(vec);
        System.out.println("Vector: " + s); 
        RealVector v = stringToVector(s);
        Assert.assertEquals(vec, v);
    }

}
