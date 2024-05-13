package eu.qualimaster.monitoring.observations;

import eu.qualimaster.monitoring.systemState.IAggregationFunction;

/**
 * Implements an aggregating compound observation, mapping all null keys to a single value and building
 * the result by applying a binary aggregation function.
 * 
 * @author Holger Eichelberger
 */
public class AggregatingCompoundObservation extends AbstractCompoundObservation {

    private static final long serialVersionUID = -8662078251197668341L;
    private IAggregationFunction aggregator;
    
    /**
     * Creates an aggregating compound observation.
     * 
     * @param aggregator the aggregator function
     */
    public AggregatingCompoundObservation(IAggregationFunction aggregator) {
        super();
        this.aggregator = aggregator;
    }
    
    /**
     * Creates a summarizing compound observation from a given source.
     * 
     * @param source the source to copy from
     */
    protected AggregatingCompoundObservation(AggregatingCompoundObservation source) {
        super(source);
        this.aggregator = source.aggregator;
    }
    
    /**
     * Returns the value.
     * 
     * @return the value
     */
    private double getValue0() {
        double result = aggregate(aggregator);
        if (aggregator.doAverage()) {
            int count = getComponentCount();
            if (count > 0) {
                result = result / count;
            }
        }
        return result;
    }
    
    @Override
    public double getValue() {
        return getValue0();
    }

    @Override
    public double getLocalValue() {
        return getValue0();
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        return new AggregatingCompoundObservation(this);
    }

    @Override
    protected String toStringValue() {
        return aggregator.getName() + " " + getValue() + ";";
    }

}
