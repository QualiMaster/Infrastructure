package tests.eu.qualimaster.monitoring.profiling;

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
        Assert.assertTrue(runTime <= (100 * 1000000));
        for (int i = 0; i < 100; i++) {
            filter = null;
            startTime = System.nanoTime();
            filter = new Kalman();
            runTime = System.nanoTime() - startTime;
            Assert.assertTrue(runTime <= (1 * 1000000));
        }
    }
    
    /**
     * Test storing a Kalman-Instance to the file-system.
     */
    @Test
    public void testStoreKalmanInstance() {
      //TODO
    }
    /**
     * Test loading a Kalman-Instance from the file-system.
     */
    @Test
    public void testLoadKalmanInstance() {
        //TODO
    }
    /**
     * Test analogy based instantiation of a Kalman-Filter.
     */
    @Test
    public void testAnalogyBasesKalmanInstance() {
        //TODO
    }
}
