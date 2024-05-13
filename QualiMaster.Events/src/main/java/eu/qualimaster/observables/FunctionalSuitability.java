package eu.qualimaster.observables;

import eu.qualimaster.common.QMInternal;

/**
 * Functional suitability observables (see D4.1, D4.2).
 * 
 * @author Holger Eichelberger
 */
public enum FunctionalSuitability implements IObservable {
    
    ACCURACY_CONFIDENCE,
    ACCURACY_ERROR_RATE,
    BELIEVABILITY,
    RELEVANCY,
    COMPLETENESS,
    NOVELTY,
    DIVERSITY,
    SERENDIPITY,
    MP_VOLATILITY;

    @QMInternal
    @Override
    public boolean isInternal() {
        return false;
    }

}
