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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.profiling.approximation.HarmonicApacheMathApproximator;
import eu.qualimaster.monitoring.profiling.approximation.IApproximator;
import eu.qualimaster.monitoring.profiling.approximation.IApproximatorCreator;
import eu.qualimaster.monitoring.profiling.approximation.PolynomialApacheMathApproximator;
import eu.qualimaster.monitoring.profiling.approximation.SplineInterpolationLinearExtrapolationApproximator;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Tests the approximators.
 * 
 * @author Holger Eichelberger
 */
public class ApproximatorTest {

    /**
     * Represents a (parameter,observable) data point.
     * 
     * @author Holger Eichelberger
     */
    private static class Point {
        private int param;
        private double value;
        private boolean measured;
        
        /**
         * Creates a non-measured data point.
         * 
         * @param param parameter value
         * @param value observable value
         */
        private Point(int param, double value) {
            this(param, value, false);
        }

        /**
         * Creates a measured data point.
         * 
         * @param param parameter value
         * @param value observable value
         * @param measured whether the data point was measured
         */
        private Point(int param, double value, boolean measured) {
            this.param = param;
            this.value = value;
            this.measured = measured;
        }

        /**
         * Updates the given approximator with this data point.
         * 
         * @param approx the approximator to update
         */
        private void update(IApproximator approx) {
            approx.update(param, value, measured);
        }
        
    }

    /**
     * Describes a parameter value and an expected approximation range.
     * 
     * @author Holger Eichelberger
     */
    private static class ExpectedApproximation {
        private double minExpected;
        private int param;
        private double maxExpected;

        /**
         * Creates a parameter value without expected range.
         * 
         * @param param the parameter value
         */
        private ExpectedApproximation(int param) {
            this(Double.MIN_VALUE, param, Double.MAX_VALUE);
        }

        /**
         * Creates a parameter value with given expected range.
         * 
         * @param minExpected the minimum expected value
         * @param param the parameter value
         * @param maxExpected the maximum expected value
         */
        private ExpectedApproximation(double minExpected, int param, double maxExpected) {
            this.minExpected = Math.min(minExpected, maxExpected);
            this.param = param;
            this.maxExpected = Math.max(minExpected, maxExpected);
        }
        
        /**
         * Asserts an approximation for the parameter value and the expected range given by this instance.
         * 
         * @param approx the approximator
         * @return the approximated value
         */
        private double assertApproximation(IApproximator approx) {
            double a = approx.approximate(param);
            Assert.assertTrue("Not " + minExpected + "<=" + a + "<=" + maxExpected + " for " + param, 
                 minExpected <= a && a <= maxExpected);
            return a;
        }
    }

    /**
     * Adds (param,value) to points as non-measured information.
     * 
     * @param points the points to be modified
     * @param param the parameter value
     * @param value the observable value
     */
    private static void add(List<Point> points, int param, double value) {
        add(points, param, value, false);
    }
    
    /**
     * Adds (param,value) to points.
     * 
     * @param points the points to be modified
     * @param param the parameter value
     * @param value the observable value
     * @param measured measured or not
     */
    private static void add(List<Point> points, int param, double value, boolean measured) {
        points.add(new Point(param, value, measured));
    }

    /**
     * Creates a linear dataset.
     * 
     * @return the linear dataset
     */
    private static List<Point> createLinearDataset() {
        List<Point> data = new ArrayList<Point>();
        add(data, 1, 100);
        add(data, 5, 200);
        add(data, 10, 300);
        add(data, 15, 400);
        return data;
    }
    
    /**
     * Creates a converging dataset.
     * 
     * @return a converging dataset
     */
    private static List<Point> createConvergingDataset() {
        List<Point> data = new ArrayList<Point>();
        add(data, 1, 400);
        add(data, 5, 700);
        add(data, 10, 900);
        add(data, 15, 1000);
        add(data, 20, 1050);
        add(data, 25, 1060);
        add(data, 30, 1062);
        return data;
    }

    /**
     * Creates an increasing and decreasing dataset.
     * 
     * @return the dataset
     */
    private static List<Point> createIncreasingDecreasingDataset() {
        List<Point> data = new ArrayList<Point>();
        add(data, 1, 400);
        add(data, 5, 700);
        add(data, 10, 900);
        add(data, 15, 1000);
        add(data, 20, 900);
        add(data, 25, 700);
        add(data, 30, 500);
        return data;
    }

    
    /**
     * Tests the Math 3 harmonic approximator.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testHarmonicApproximator() throws IOException {
        List<Point> data = createConvergingDataset();
        test(HarmonicApacheMathApproximator.INSTANCE_10, data, null);
    }
    
    /**
     * Tests the Math 3 polynomial approximator.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testPolynomialApproximator() {
        List<Point> data = createLinearDataset();
        test(PolynomialApacheMathApproximator.INSTANCE_3, data, null);
    }

    /**
     * Tests the Math 3 Spline approximator.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSplineApproximator() {
        List<Point> data = createConvergingDataset();
        List<ExpectedApproximation> exp = new ArrayList<ExpectedApproximation>();
        // inside
        exp.add(new ExpectedApproximation(400, 3, 700));
        exp.add(new ExpectedApproximation(1000, 17, 1050));
        // outside
        exp.add(new ExpectedApproximation(0, 0, 400));
        exp.add(new ExpectedApproximation(1062, 40, 1066));
        test(SplineInterpolationLinearExtrapolationApproximator.INSTANCE, data, exp);

        data = createLinearDataset();
        exp.clear();
        // inside
        exp.add(new ExpectedApproximation(100, 3, 200));
        exp.add(new ExpectedApproximation(300, 11, 400));
        // outside
        exp.add(new ExpectedApproximation(0, 0, 100));
        exp.add(new ExpectedApproximation(400, 17, 500));
        test(SplineInterpolationLinearExtrapolationApproximator.INSTANCE, data, exp);
        
        data = createIncreasingDecreasingDataset();
        exp.clear();
        // inside
        exp.add(new ExpectedApproximation(400, 3, 700));
        exp.add(new ExpectedApproximation(700, 23, 900));
        // outside
        exp.add(new ExpectedApproximation(0, 0, 400));
        exp.add(new ExpectedApproximation(0, 35, 500));
        test(SplineInterpolationLinearExtrapolationApproximator.INSTANCE, data, exp);
    }

    /**
     * Tests writing, reading, updating and querying an approximator.
     * 
     * @param creator the approximator creator
     * @param data the data used for updating
     * @param expectations the expected values for given input values (may be <b>null</b> for no expectations)
     */
    private void test(IApproximatorCreator creator, List<Point> data, List<ExpectedApproximation> expectations) {
        File tmp = new File(FileUtils.getTempDirectory(), "approx");
        tmp.mkdirs();
        
        IApproximator approx1 = creator.createApproximator(tmp, "key", TimeBehavior.LATENCY);
        Assert.assertNotNull(approx1);
        for (int d = 0; d < data.size(); d++) {
            data.get(d).update(approx1);
        }
        File f = approx1.store(tmp);
        Assert.assertNotNull(f);
        Assert.assertTrue(f.exists());
        
        IApproximator approx2 = creator.createApproximator(tmp, "key", TimeBehavior.LATENCY);
        Assert.assertNotNull(approx2);
        Assert.assertTrue(approx2.containsSameData(approx1));

        if (null != expectations) {
            for (ExpectedApproximation a : expectations) {
                double v1 = a.assertApproximation(approx1);
                double v2 = a.assertApproximation(approx2);
                Assert.assertEquals(v1, v2, 0.05);
            }
        }
        
        FileUtils.deleteQuietly(tmp);
    }

}
