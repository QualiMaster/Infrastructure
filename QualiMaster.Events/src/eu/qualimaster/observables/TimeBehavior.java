package eu.qualimaster.observables;

import eu.qualimaster.common.QMInternal;

/**
 * Time-behavior observables (see D4.1).
 * 
 * @author Holger Eichelberger
 */
public enum TimeBehavior implements IObservable {
    
    LATENCY,
    THROUGHPUT_ITEMS,
    THROUGHPUT_VOLUME,
    ENACTMENT_DELAY;

    @QMInternal
    @Override
    public boolean isInternal() {
        return false;
    }
    
}
