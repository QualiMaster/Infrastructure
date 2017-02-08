package eu.qualimaster.observables;

import eu.qualimaster.common.QMInternal;

/**
 * Scalability observables (see D4.1).
 * 
 * @author Holger Eichelberger
 */
public enum Scalability implements IObservable {

    VOLUME,
    VELOCITY,
    VOLATILITY,
    VARIETY,
    ITEMS,
    PREDECESSOR_ITEMS,
    PREDICTED_ITEMS_THRESHOLD;

    @QMInternal
    @Override
    public boolean isInternal() {
        return false;
    }
    
}
