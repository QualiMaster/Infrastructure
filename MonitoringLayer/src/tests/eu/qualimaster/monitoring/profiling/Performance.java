package tests.eu.qualimaster.monitoring.profiling;

import org.junit.Test;

/**
 * Collection of Test-Cases to ensure the performance of the
 * Kalman-Implementation. This is to ensure that a prediction, be it the initial
 * one, for multiple time-steps ahead or the incremental prediction. The
 * storing- and loading-time of Kalman-Instances (each Instance represents a
 * point in the parameter space for a specific algorithm in a specific
 * environment) is tested as well. The loading-time potentially prolongs the
 * prediction time.
 * 
 * @author Christopher Voges
 *
 */
public class Performance {

    /**
     * Dummy.
     */
    @Test
    public void test() {
    }
}
