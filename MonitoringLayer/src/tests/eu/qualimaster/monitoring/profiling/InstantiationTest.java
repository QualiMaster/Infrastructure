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

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.profiling.Kalman;

/**
 * Collection of test-cases for the creation, storing and loading of Kalman-Filter instances.
 * @author Christopher Voges
 *
 */
public class InstantiationTest {
    /**
     * Test for the time needed to create a new default Kalman-Instance.
     * Fails if more than 100ms are needed for the first (cold) instantiation
     * or more than 1ms for following (warm) instantiations.
     */
    @Test
     public void testNewKalmanInstance() {
        long startTime = System.nanoTime();
        @SuppressWarnings("unused")
        Kalman filter = new Kalman();
        long runTime = System.nanoTime() - startTime;
        System.out.println(runTime);
        Assert.assertTrue(runTime <= (100 * 1000000)); // timing test may fail on different system
        for (int i = 0; i < 100; i++) {
            filter = null;
            startTime = System.nanoTime();
            filter = new Kalman();
            runTime = System.nanoTime() - startTime;
            Assert.assertTrue(runTime <= (1 * 1000000)); // timing test may fail on different system
        }
    }

    /**
     * Test storing a Kalman-Instance to the file-system.
     */
    @Test
    public void testEqualsKalmanInstance() {
        Kalman k1 = new Kalman();
        Kalman k2 = new Kalman();
        Assert.assertEquals(k1, k2);
        Assert.assertEquals(k1.hashCode(), k2.hashCode());
        
        // just to be sure that it recognizes the update as a new value
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        k1.update(50); // shall update at least lastUpdate/lastUpdated
        Assert.assertNotEquals(k1, k2);
        // no statement about hashCodes possible!
    }
    
    /**
     * Test storing a Kalman-Instance to the file-system.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testStoreKalmanInstance() throws IOException {
        File f = new File(FileUtils.getTempDirectory(), "kalman.tmp");
        f.delete();
        Kalman k1 = new Kalman();
        k1.store(f, "abba");
        
        Kalman k2 = new Kalman();
        k2.load(f, "abba");
        
        Assert.assertEquals(k1, k2);
        f.delete();
    } // loading without storing makes only sense if we have a prepared kalman instance and can compare its attributes

}
