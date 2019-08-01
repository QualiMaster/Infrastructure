package tests.eu.qualimaster.common.switching.warmupDataSynchronizationVariant;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.switching.warmupDataSynchronizationVariant.WSDSDeterminationStrategy;

/**
 * Test the determination strategy of the "WSDS".
 * @author Cui Qin
 *
 */
public class TestWSDSDeterminationStrategy {
    
    /**
     * Tests.
     */
    @Test
    public void test() {
        WSDSDeterminationStrategy detStrategy = new WSDSDeterminationStrategy(5000, 1000, 500);
        detStrategy.setAlgorithmStartingPoint(Long.valueOf("1559834602121"));
        detStrategy.setSwitchArrivalPoint(Long.valueOf("1559834658461"));
        detStrategy.determineSwitchPoint();
        long expected = Long.valueOf("1559834664121"); //one experimental result
        Assert.assertEquals(expected, detStrategy.getSwitchPoint());
    }

}
