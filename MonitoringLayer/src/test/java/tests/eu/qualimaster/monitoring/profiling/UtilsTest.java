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

import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictionManager;
import eu.qualimaster.monitoring.profiling.Constants;
import eu.qualimaster.monitoring.profiling.DefaultStorageStrategy;
import eu.qualimaster.monitoring.profiling.Pipeline;
import eu.qualimaster.monitoring.profiling.PipelineElement;
import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy.ProfileKey;
import eu.qualimaster.monitoring.profiling.predictors.Utils;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.TimeBehavior;

import static eu.qualimaster.monitoring.profiling.predictors.Utils.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * Tests for vector equality.
     */
    @Test
    public void testEqualsVector() {
        double[] data = new double[] {0, 0.1, -0.2, 10.5, 101203450.4, 1245};
        RealVector v1 = MatrixUtils.createRealVector(data);
        data = new double[] {0.3};
        RealVector v2 = MatrixUtils.createRealVector(data);
        data = new double[] {};
        RealVector v3 = MatrixUtils.createRealVector(data);
        
        Assert.assertTrue(Utils.equalsVector(v1, v1, 0.005));
        Assert.assertFalse(Utils.equalsVector(v1, v2, 0.005));
        Assert.assertFalse(Utils.equalsVector(v1, v3, 0.005));
        Assert.assertFalse(Utils.equalsVector(v2, v3, 0.005));
    }

    /**
     * Tests for matrix equality.
     */
    @Test
    public void testEqualsMatrix() {
        double[][] data = new double[][] {
            {1, 10.2, -8.2, 0.2, 2300410.1, 1243}, 
            {0, 0.1, 0.2, -10.5, 101203450.4, -1245}};
        RealMatrix m1 = MatrixUtils.createRealMatrix(data);
        data = new double[][] {{1}, {0}};
        RealMatrix m2 = MatrixUtils.createRealMatrix(data);
        
        Assert.assertTrue(Utils.equalsMatrix(m1, m1, 0.005));
        Assert.assertFalse(Utils.equalsMatrix(m1, m2, 0.005));
        Assert.assertFalse(Utils.equalsMatrix(m2, m1, 0.005));
    }
    
    /**
     * Test for double equality.
     */
    @Test
    public void testEqualsDouble() {
        Assert.assertTrue(Utils.equalsDouble(0.05, 0.05, 0.005));
        Assert.assertTrue(Utils.equalsDouble(0.0005, 0.0004, 0.005));
        Assert.assertFalse(Utils.equalsDouble(0, 0.2, 0.005));
        Assert.assertFalse(Utils.equalsDouble(0, -0.2, 0.005));
    }
    
    /**
     * Tests string-to-key and back functionality.
     */
    @Test
    public void testStorage() {
        IStorageStrategy strategy = DefaultStorageStrategy.INSTANCE;
        Pipeline pip = AlgorithmProfilePredictionManager.obtainPipeline("pip");
        PipelineElement elt = pip.obtainElement("elt");
        IObservable obs = TimeBehavior.LATENCY;
        Map<Object, Serializable> key = new HashMap<Object, Serializable>();
        key.put(Constants.KEY_INPUT_RATE, 100);
        key.put(ResourceUsage.EXECUTORS, 5);
        String k = strategy.generateKey(elt, key, obs, true);
        ProfileKey pKey = strategy.parseKey(k);
        Assert.assertEquals(pip.getName(), pKey.getPipeline());
        Assert.assertEquals(elt.getName(), pKey.getElement());
        Assert.assertEquals(obs, pKey.getObservable());
        Assert.assertEquals(key, pKey.getParameter());
    }

}
