package eu.qualimaster.common.switching.acknowledgement;


/**
 * Provide an abstract class for the acknowledgement strategy.
 * @author Cui Qin
 *
 */
public abstract class AbstractAcknowledgementStrategy implements IAcknowledgementStrategy {
    private static final String STRATEGYTYPE = "acknowledgement";
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
}
