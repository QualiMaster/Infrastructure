package eu.qualimaster.common.switching.warmupDataSynchronizationVariant;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.ISignalHandler;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.determination.AbstractDeterminationStrategy;
import eu.qualimaster.common.switching.determination.SlidingWindowBasedEarlyWarmupSafePoint;

/**
 * The determination strategy for the "Warm-up Switch with Data Synchronization" variant.
 * @author Cui Qin
 *
 */
public class WSDSDeterminationStrategy extends AbstractDeterminationStrategy {
    private static Logger logger = LogManager.getLogger(WSDSDeterminationStrategy.class);
    private long algStart;
    private long switchArrivalPoint;
    private long warmupDuration;
    private long windowSize;
    private long slidingStep;
    
    /**
     * Constructor for the determination strategy of the "Warm-up Switch with Data Synchronization" variant.
     * @param warmupDuration the warm-up duration
     * @param windowSize the window size
     * @param slidingStep the sliding step
     */
    public WSDSDeterminationStrategy(long warmupDuration, long windowSize, long slidingStep) {
        this.warmupDuration = warmupDuration;
        this.windowSize = windowSize;
        this.slidingStep = slidingStep;
    }
    
    /**
     * Sets the algorithm starting point.
     * @param algStart the algorithm starting point
     */
    public void setAlgorithmStartingPoint(long algStart) {
        this.algStart = algStart;
    }
    
    /**
     * Sets the switch arrival point.
     * @param switchArrivalPoint the switch arrival point
     */
    public void setSwitchArrivalPoint(long switchArrivalPoint) {
        this.switchArrivalPoint = switchArrivalPoint;
    }
       
    @Override
    public void determineSwitchPoint() {
        if (0L != algStart && 0L != switchArrivalPoint) {
            SlidingWindowBasedEarlyWarmupSafePoint safepoint = new SlidingWindowBasedEarlyWarmupSafePoint(algStart,
                   switchArrivalPoint, warmupDuration, windowSize, slidingStep);
            setSwitchPoint(safepoint.determineSafePoint());
        }
    }
    
    @Override
    public void uponSwitchPoint() {
        WSDSSignalStrategy signalStrategy = (WSDSSignalStrategy) SwitchStrategies.getInstance()
                .getStrategies().get("signal");
        ISignalHandler signalHandler = signalStrategy.getSignalHandlers()
                .get("enact" + SwitchNodeNameInfo.ORIGINALINTERMEDIARYNODE);
        if (null == signalHandler) {
            logger.warn("No signal handler is found!");
        } else {
            logger.info("Sending the next signals.");
            signalHandler.nextSignals();
        }
    }
}
