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

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.profiling.IAlgorithmProfilePredictor;
import eu.qualimaster.monitoring.profiling.Kalman;


/**
 * Collection of Test-Cases to ensure the performance of the
 * Kalman-Implementation. This is to ensure that a prediction, be it the initial
 * one, for multiple time-steps ahead or the incremental prediction. The
 * storing- and loading-time of Kalman-Instances (each Instance represents a
 * point in the parameter space for a specific algorithm in a specific
 * environment) is tested in 'InstantiationTest'. 
 * 
 * @author Christopher Voges
 *
 */
public class PerformanceTest {

    /**
     * Conducting a performance benchmark for multiple different input-sets.
     * It is measured how long the update/predict-cycle needs to run through the given data.
     * Afterwards this time is compared to a given maximum. If more is needed the test fails.
     * The test fails, when a run exceeds the given time.
     */
    @Test
    public void testAgainstHistory() {
        ArrayList<String> testData = TestTools.loadData("performanceTestData");
        if (testData.size() > 0) {
            for (String string : testData) {
                // Only numbers and the separation-signs ('.', ',' and ';') are allowed
                if (string.matches("[0-9;\\.,]+")) {
                    String[] data = string.replaceAll(",", ".").split(";");
                    double[] entries = new double[data.length];
                    for (int i = 0; i < data.length; i++) {
                        try {
                            entries[i] = new Double(data[i]);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    // Begin time-measurement
                    long startTime = System.nanoTime();
                    
                    // Create Kalman-Instance
                    IAlgorithmProfilePredictor filter = new Kalman();

                    // Run Update-Predict-Cycle
                    for (int i = 1; i < entries.length; i++) {
                        filter.update(i, entries[i]);
                        filter.predict(0);
                    }
                    // Check if runTime <= maximalCriteria
                    long runTime = System.nanoTime() - startTime;
                    Assert.assertTrue("The following Test-Data took too long (" 
                            + ((double) (runTime / 1000000)) + "ms) to execute: " 
                            + string, 
                            runTime <= (entries[0] * 1000000));
                } else {
                    System.out.println("Following line was skipped for containing illegal characters:");
                    System.out.println(string);
                }
            }
        }
    }
    /**
     * Test the update-time for a fragmented time-line.
     */
    @Test
    public void testUpdateWithGap() {
      //TODO
    }
    /**
     * Test the update-time for a single value.
     */
    @Test
    public void testUpdate() {
      //TODO
    }
    /**
     * Test the prediction-time for one time-step ahead.
     */
    @Test
    public void testPredict() {
      //TODO
    }
    /**
     * Test the prediction-time for multiple time-steps ahead.
     */
    @Test
    public void testPredictMultiple() {
      //TODO
    }
}
