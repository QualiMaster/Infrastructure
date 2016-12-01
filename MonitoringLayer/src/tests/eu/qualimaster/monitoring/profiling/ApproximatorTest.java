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
     * Tests the Math 3 harmonic approximator.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testHarmonicApproximator() throws IOException {
        List<Point> data = new ArrayList<Point>();
        add(data, 1, 100);
        add(data, 5, 200);
        add(data, 10, 300);
        add(data, 15, 400);
        add(data, 20, 300);
        add(data, 25, 200);
        add(data, 30, 100);
        
        Double res = test(HarmonicApacheMathApproximator.INSTANCE_10, data, 13);
        System.out.println("Harmonic " + res);
    }
    
    /**
     * Tests the Math 3 polynomial approximator.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testPolynomialApproximator() {
        List<Point> data = new ArrayList<Point>();
        add(data, 1, 100);
        add(data, 5, 200);
        add(data, 10, 300);
        add(data, 15, 400);
        
        Double res = test(PolynomialApacheMathApproximator.INSTANCE_3, data, 3);
        System.out.println("Polynomial " + res);
    }
    
    /**
     * Tests writing, reading, updating and querying an approximator.
     * 
     * @param creator the approximator creator
     * @param data the data used for updating
     * @param param the param used for querying (may be <b>null</b> for no querying)
     * @return the apporoximated value (<b>null</b> if <code>param</code> was <b>null</b>)
     */
    private Double test(IApproximatorCreator creator, List<Point> data, Integer param) {
        Double result = null;
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

        if (null != param) {
            double v1 = approx1.approximate(param);
            double v2 = approx2.approximate(param);
            Assert.assertEquals(v1, v2, 0.05);
            result = v1;
        }
        
        FileUtils.deleteQuietly(tmp);
        return result;
    }

}
