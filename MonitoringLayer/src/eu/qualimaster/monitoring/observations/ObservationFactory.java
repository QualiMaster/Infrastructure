package eu.qualimaster.monitoring.observations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.systemState.AggregationFunctionFactory;
import eu.qualimaster.monitoring.systemState.AggregationFunctionFactory.ConstantAggregationFunctionCreator;
import eu.qualimaster.monitoring.systemState.IAggregationFunction;
import eu.qualimaster.monitoring.systemState.ObservationAggregatorFactory;
import eu.qualimaster.monitoring.systemState.ObservationAggregatorFactory.ObservationAggregatorPoolManager;
import eu.qualimaster.monitoring.topology.ITopologyProvider;
import eu.qualimaster.observables.AnalysisObservables;
import eu.qualimaster.observables.CloudResourceUsage;
import eu.qualimaster.observables.FunctionalSuitability;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;
import eu.qualimaster.observables.IObservable;

/**
 * Creates observations depending on the combination of part type and observable.
 * 
 * @author Holger Eichelberger
 */
public class ObservationFactory {

    public static final long MAX_TIMEBASE_DIFF = 0; // moving aggregation for time-based aggregations
    
    static {
        registerPipelineObservationAggregator(TimeBehavior.LATENCY, IAggregationFunction.SUM, false, 
            IAggregationFunction.MAX);
        registerConstantPipelineNodeAggregator(TimeBehavior.LATENCY, IAggregationFunction.AVG);
        
        registerPipelineObservationAggregator(TimeBehavior.THROUGHPUT_ITEMS, IAggregationFunction.MIN, false, 
            IAggregationFunction.MAX);
        registerConstantPipelineNodeAggregator(TimeBehavior.THROUGHPUT_ITEMS, IAggregationFunction.SUM);
        
        registerPipelineObservationAggregator(TimeBehavior.THROUGHPUT_VOLUME, IAggregationFunction.MIN, false, 
            IAggregationFunction.MAX);
        registerConstantPipelineNodeAggregator(TimeBehavior.THROUGHPUT_VOLUME, IAggregationFunction.SUM);

        registerPipelineObservationAggregator(ResourceUsage.CAPACITY, IAggregationFunction.SUM, true, 
            IAggregationFunction.MAX);
        registerConstantPipelineNodeAggregator(ResourceUsage.CAPACITY, IAggregationFunction.AVG);

        registerPipelineObservationAggregator(ResourceUsage.USED_MEMORY, IAggregationFunction.SUM, false, 
            IAggregationFunction.SUM);
        registerConstantPipelineNodeAggregator(ResourceUsage.USED_MEMORY, IAggregationFunction.SUM);
    }
    
    /**
     * Interface for creating observations.
     * 
     * @author Holger Eichelberger
     */
    public interface IObservationCreator extends Serializable {

        /**
         * Creates an observation for the given observable and type.
         * 
         * @param observable the observable to create the observation for
         * @param type the type to create the observation for
         * @param observationProvider a provider for the observations and the pipeline topology in case that an 
         *     observation requires access to the underlying observations (derived observation) or structural 
         *     information about the pipeline to perform a correct aggregation (may be <b>null</b>)
         * @return the observation instance (must not be <b>null</b>) 
         */
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider);
        
    }
    
    /**
     * Defines a configurable single observation creator.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("serial")
    private static class SingleObservationCreator implements IObservationCreator {

        private Double init;
        
        /**
         * Creates a single observation creator with given initial value.
         * 
         * @param init the initial value, may be <b>null</b> for none
         */
        private SingleObservationCreator(Double init) {
            this.init = init;
        }
        
        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            IObservation result = new SingleObservation();
            if (null != init) {
                result.setValue(init, null); // key is not relevant for single observation
            }
            return result;
        }

    }
    
    /**
     * Creates a single value observation.
     */
    public static final IObservationCreator CREATOR_SINGLE = new SingleObservationCreator(null);

    /**
     * Creates a single value statistics observation.
     */
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_SINGLE_STATISTICS = new IObservationCreator() {
        
        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            return new DelegatingStatisticsObservation(new SingleObservation());
        }
    };
    
    /**
     * Creates a summarizing compound observation.
     */
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_SUM_COMPOUND = new IObservationCreator() {
        
        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            return new AggregatingCompoundObservation(IAggregationFunction.SUM);
        }
    };

    /**
     * Creates a compound observation with aggregation for the given observable via 
     * {@link ObservationAggregatorFactory}.
     */
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_COMPOUND = new IObservationCreator() {
        
        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            return createAggregationCompoundObservation(observable);
        }
    };

    /**
     * Creates a topology compound observation matching the given observable via {@link ObservationAggregatorFactory}
     * and the topology provider of the given observation provider.
     */
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_TOPOLOGY_COMPOUND = new IObservationCreator() {
      
        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            return new DelegatingTopologyAggregatorObservation(createAggregationCompoundObservation(observable), 
                observable, getTopologyProvider(observationProvider), true);
        }
    };
    
    /**
     * Creates a compound observation with 1 second statistics and absolute aggregation matching the given observable 
     * via {@link ObservationAggregatorFactory}.
     */
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_COMPOUND_STATISTICS_1S_ABS = new IObservationCreator() {

        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            return new DelegatingStatisticsObservation(new DelegatingTimeFramedObservation(
                createAggregationCompoundObservation(observable), 1000, MAX_TIMEBASE_DIFF)); 
        }
    };

    /**
     * Creates a compound observation with 1 second statistics and absolute aggregation matching the given observable 
     * via {@link ObservationAggregatorFactory}.
     */
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_COMPOUND_TOPOLOGY_SINK_SUM_STATISTICS_1S 
        = new IObservationCreator() {

            @Override
            public IObservation create(IObservable observable, IPartType type, 
                IObservationProvider observationProvider) {
                IObservationCreator base; 
                if (Scalability.ITEMS == observable 
                    && (PartType.PIPELINE == type 
                        || (null != observationProvider && Type.FAMILY == observationProvider.getComponentType()))) {
                    base = CREATOR_NULL; // requires update but takes value from current
                } else {
                    base = CREATOR_COMPOUND;
                }
                IObservation result = createSinkAggregation(base, observable, type, observationProvider, 
                    IAggregationFunction.SUM);
                if (CREATOR_NULL != base) { // otherwise already time framed
                    result = new DelegatingTimeFramedObservation(result, 1000, MAX_TIMEBASE_DIFF);
                }
                
                return /*new DelegatingStatisticsObservation(*/result/*)*/;
            }
        };
        
    /**
     * A creator for a combination of a referencing observation, which is then time-framed. The referencing
     * observation is reflecting the actual value of an underlying observation (given by the <code>reference</code>
     * observable on the same observation provider).
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("serial")
    public static class TimeFramedReferencingObservationCreator implements IObservationCreator {

        private int timeFrame;
        private IObservable reference;
        private boolean enableValueChange;
        
        /**
         * Creates the creator.
         * 
         * @param reference the reference to take the value from (shall be different than the observable to create
         *   the observation for)
         * @param timeFrame the time frame to aggregate, no time framing will be applied if <code>timeFrame</code> is
         *   not positive
         * @param enableValueChange enable value changes on the underlying reference or not, typically 
         *   <code>false</code> is a safe setting to avoid cycles in the topology aggregation 
         */
        public TimeFramedReferencingObservationCreator(IObservable reference, int timeFrame, 
            boolean enableValueChange) {
            this.reference = reference;
            this.timeFrame = timeFrame;
            this.enableValueChange = enableValueChange;
        }
        
        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            IObservation result = new ReferencingObservation(observationProvider, reference, enableValueChange);
            if (timeFrame > 0) {
                result = new DelegatingTimeFramedObservation(result, timeFrame, MAX_TIMEBASE_DIFF);
            }
            return result;
        }
        
    }
    
    public static final IObservationCreator ITEMS_1S 
        = new TimeFramedReferencingObservationCreator(TimeBehavior.THROUGHPUT_ITEMS, 1000, false);
    
    @SuppressWarnings("serial")
    public static final IObservationCreator PREDICTED_ITEMS = new IObservationCreator() {

        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            return new FallbackObservation(CREATOR_SINGLE, 
                new ConfigurationConstantObservation(observationProvider, observable, 0), observable, type, 
                    observationProvider);
        }
        
    };
    
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_NULL = new IObservationCreator() {

        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            return ConstantObservation.NULL_OBSERVATION;
        }
        
    };

    @SuppressWarnings("serial")
    public static final IObservationCreator PREDECESSOR_ITEMS_1S = new IObservationCreator() {
        
        @Override
        public IObservation create(IObservable observable, IPartType type, IObservationProvider observationProvider) {
            // we summarize here over items/s!
            return new DelegatingTopologyPredecessorAggregatorObservation(new SingleObservation(), 
                Scalability.ITEMS, observationProvider.getTopologyProvider(), IAggregationFunction.SUM);
        }
    };
    
    /**
     * Creates a sum-based topology aggregator for sinks only based on compound observations.
     */
    @SuppressWarnings("serial")
    public static final IObservationCreator CREATOR_COMPOUND_TOPOLOGY_SINK_SUM = new IObservationCreator() {
        
        @Override
        public IObservation create(IObservable observable, IPartType type, 
            IObservationProvider observationProvider) {
            return createSinkAggregation(CREATOR_SUM_COMPOUND, observable, type, observationProvider, 
                IAggregationFunction.SUM);
        }
    };
    
    private static final IPartType TYPE_NULL = new IPartType() {
        @Override
        public String name() { 
            return ""; 
        } 
    };
    
    private static final Map<IObservable, Map<IPartType, IObservationCreator>> CREATORS 
        = new HashMap<IObservable, Map<IPartType, IObservationCreator>>();
    private static final Map<IPartType, List<IObservable>> PARTS = new HashMap<IPartType, List<IObservable>>();

    /**
     * Prevent external creation.
     */
    private ObservationFactory() {
    }

    /**
     * Creates a sink aggregator.
     *
     * @param baseCreator the base creator for creating the underlying observation instance
     * @param observable the observable to create for
     * @param type the type of the actual system part
     * @param observationProvider the observation provider
     * @param sinkAggregation the sink aggregation function
     * @return the created aggregator
     */
    private static final DelegatingTopologySinkAggregatorObservation createSinkAggregation(
        IObservationCreator baseCreator, IObservable observable, IPartType type, 
        IObservationProvider observationProvider, IAggregationFunction sinkAggregation) {
        IObservation base = baseCreator.create(observable, type, observationProvider);
        return new DelegatingTopologySinkAggregatorObservation(base, observable, 
            getTopologyProvider(observationProvider), sinkAggregation);        
    }

    /**
     * Registers a constant aggregation function creator for parallelized processors of a single pipeline node.
     * 
     * @param observable the observable
     * @param aggregator the aggregator function for path elements (must be a reusable constant implementations 
     *     of {@link IAggregationFunction})
     */
    private static synchronized void registerConstantPipelineNodeAggregator(IObservable observable, 
        IAggregationFunction aggregator) {
        AggregationFunctionFactory.register(observable, new ConstantAggregationFunctionCreator(aggregator));
    }
    
    /**
     * Registers a observation aggregator pool manager for the given observable / aggregators combination.
     * 
     * @param observable the observable
     * @param elementAggregator the aggregator function for path elements
     * @param pathAverage whether the average value of a path or its direct value via <code>pathAverage</code> shall 
     *     be used
     * @param topologyAggregator the topology aggregator 
     */
    private static synchronized void registerPipelineObservationAggregator(IObservable observable, 
        IAggregationFunction elementAggregator, boolean pathAverage, IAggregationFunction topologyAggregator) {
        ObservationAggregatorFactory.register(new ObservationAggregatorPoolManager(observable, elementAggregator, 
            pathAverage, topologyAggregator));
    }
    
    /**
     * Returns an aggregation compound observation for <code>observable</code> with default fallback to 
     * {@link IAggregationFunction#SUM} as aggregation function (legacy).
     * 
     * @param observable the observable
     * @return the aggregation observation for <code>observable</code>.
     */
    private static final IObservation createAggregationCompoundObservation(IObservable observable) {
        IAggregationFunction aggregator = AggregationFunctionFactory.createAggregationFunction(observable);
        if (null == aggregator) {
            aggregator = IAggregationFunction.SUM;
        }
        return new AggregatingCompoundObservation(aggregator);
    }
    
    /**
     * Returns the topology provider.
     * 
     * @param observationProvider the observation provider (may be <b>null</b>)
     * @return the topology provider (may be <b>null</b>)
     */
    private static final ITopologyProvider getTopologyProvider(IObservationProvider observationProvider) {
        return null == observationProvider ? null : observationProvider.getTopologyProvider();
    }

    /**
     * Returns the default observations for a certain part type.
     * 
     * @param type the part type
     * @return the default observations
     */
    public static final List<IObservable> getObservations(IPartType type) {
        List<IObservable> result = PARTS.get(type);
        return null == result ? new ArrayList<IObservable>() : result;
    }
    
    /**
     * Creates an observation instance. {@link #CREATOR_SINGLE} leading to {@link SingleObservation} is used as a 
     * fallback if no creator is registered.
     * 
     * @param observable the observable
     * @param type the part type
     * @param observationProvider a provider for the pipeline topology in case that an observation requires structural 
     *     pipeline information to perform a correct aggregation (may be <b>null</b>)
     * @return the corresponding observation instance 
     */
    public static final IObservation createObservation(IObservable observable, IPartType type, 
        IObservationProvider observationProvider) {
        IObservation result = null;
        if (null != observable) {
            if (null == type) {
                type = TYPE_NULL;
            }
            IObservationCreator creator = getCreator(observable, type);
            if (null != creator) {
                result = creator.create(observable, type, observationProvider);
            }
        }
        return result;
    }
    
    /**
     * Returns the registered creator.
     * 
     * @param observable the observable
     * @param type the part type
     * @return the creator
     */
    public static final IObservationCreator getCreator(IObservable observable, IPartType type) {
        IObservationCreator creator = null;
        Map<IPartType, IObservationCreator> obsCreators = CREATORS.get(observable);
        if (null != obsCreators) {
            creator = obsCreators.get(type);
            if (null == creator && type != TYPE_NULL) {
                creator = obsCreators.get(TYPE_NULL);
            }
            if (null == creator) { // fallback
                creator = CREATOR_SINGLE;
            }
        }
        return creator;
    }
    
    /**
     * Registers a creator.
     * 
     * @param observable the observable to register for
     * @param type the part type (may be <b>null</b> denoting the default)
     * @param creator the creator to be registered
     */
    public static final void registerCreator(IObservable observable, IPartType type, IObservationCreator creator) {
        Map<IPartType, IObservationCreator> obsCreators = CREATORS.get(observable);
        if (null != creator && null != observable) {
            if (null == obsCreators) {
                obsCreators = new HashMap<IPartType, IObservationCreator>();
                CREATORS.put(observable, obsCreators);
            }
            if (null == type) {
                type = TYPE_NULL;
            }
            obsCreators.put(type, creator);
        }
    }
    
    /**
     * Registers the observables for the given part <code>type</code>.
     * 
     * @param type the type to register the observables for
     * @param observables the observables to be registered
     */
    public static final void registerPart(IPartType type, IObservable... observables) {
        List<IObservable> obs = PARTS.get(type);
        if (null == obs) {
            obs = new ArrayList<IObservable>();
            PARTS.put(type, obs);
        }
        for (int o = 0; o < observables.length; o++) {
            obs.add(observables[o]);
        }
    }
    
    /**
     * Default creators.
     */
    static { // observation types are just for now
        registerCreator(TimeBehavior.ENACTMENT_DELAY, null, CREATOR_SINGLE_STATISTICS);
        registerCreator(TimeBehavior.THROUGHPUT_ITEMS, null, CREATOR_COMPOUND_TOPOLOGY_SINK_SUM);
        registerCreator(TimeBehavior.THROUGHPUT_VOLUME, null, CREATOR_COMPOUND_TOPOLOGY_SINK_SUM);
        registerCreator(TimeBehavior.LATENCY, null, CREATOR_TOPOLOGY_COMPOUND);

        registerCreator(FunctionalSuitability.ACCURACY_CONFIDENCE, null, CREATOR_SINGLE);
        registerCreator(FunctionalSuitability.ACCURACY_ERROR_RATE, null, CREATOR_SINGLE);
        registerCreator(FunctionalSuitability.BELIEVABILITY, null, CREATOR_SINGLE);
        registerCreator(FunctionalSuitability.COMPLETENESS, null, CREATOR_SINGLE);
        registerCreator(FunctionalSuitability.RELEVANCY, null, CREATOR_SINGLE);
        registerCreator(FunctionalSuitability.MP_VOLATILITY, null, CREATOR_SINGLE);

        registerCreator(ResourceUsage.USED_MEMORY, null, CREATOR_TOPOLOGY_COMPOUND);
        registerCreator(ResourceUsage.LOAD, null, CREATOR_SINGLE);
        registerCreator(ResourceUsage.AVAILABLE_FREQUENCY, null, CREATOR_SINGLE);
        registerCreator(ResourceUsage.AVAILABLE_MEMORY, null, CREATOR_SINGLE);
        registerCreator(ResourceUsage.AVAILABLE_DFES, null, CREATOR_SUM_COMPOUND);
        registerCreator(ResourceUsage.AVAILABLE_MACHINES, null, CREATOR_SUM_COMPOUND);
        registerCreator(ResourceUsage.USED_DFES, null, CREATOR_SUM_COMPOUND);
        registerCreator(ResourceUsage.USED_MACHINES, null, CREATOR_SUM_COMPOUND);
        registerCreator(ResourceUsage.AVAILABLE_CPUS, null, CREATOR_SUM_COMPOUND);
        registerCreator(ResourceUsage.USED_CPUS, null, CREATOR_SUM_COMPOUND);
        //registerCreator(ResourceUsage.BANDWIDTH, null, CREATOR_SINGLE); // really just for now
        registerCreator(ResourceUsage.CAPACITY, null, CREATOR_TOPOLOGY_COMPOUND); 
        registerCreator(ResourceUsage.EXECUTORS, null, CREATOR_SUM_COMPOUND); 
        registerCreator(ResourceUsage.TASKS, null, CREATOR_SUM_COMPOUND);
        registerCreator(ResourceUsage.USED_CPUS, null, CREATOR_SUM_COMPOUND); 
        registerCreator(ResourceUsage.USED_DFES, null, CREATOR_SUM_COMPOUND);
        registerCreator(ResourceUsage.AVAILABLE, null, CREATOR_SINGLE);
        registerCreator(ResourceUsage.HOSTS, null, HostsObservation.CREATOR);
        
        registerCreator(Scalability.VARIETY, null, CREATOR_COMPOUND_STATISTICS_1S_ABS);
        registerCreator(Scalability.VELOCITY, null, CREATOR_COMPOUND_STATISTICS_1S_ABS);
        registerCreator(Scalability.VOLATILITY, null, CREATOR_COMPOUND_STATISTICS_1S_ABS);
        registerCreator(Scalability.VOLUME, null, CREATOR_COMPOUND_STATISTICS_1S_ABS);
        registerCreator(Scalability.ITEMS, null, ITEMS_1S); 
        registerCreator(Scalability.PREDECESSOR_ITEMS, null, PREDECESSOR_ITEMS_1S);
        registerCreator(Scalability.PREDICTED_ITEMS_THRESHOLD, null, PREDICTED_ITEMS);
        
        registerCreator(CloudResourceUsage.BANDWIDTH, null, CREATOR_SINGLE);
        registerCreator(CloudResourceUsage.PING, null, CREATOR_SINGLE);
        registerCreator(CloudResourceUsage.USED_HARDDISC_MEM, null, CREATOR_SINGLE);
        registerCreator(CloudResourceUsage.USED_PROCESSORS, null, CREATOR_SINGLE);
        registerCreator(CloudResourceUsage.USED_WORKING_STORAGE, null, CREATOR_SINGLE);
         
        registerCreator(AnalysisObservables.IS_VALID, null, new SingleObservationCreator(1.0)); // IS_VALID by default
        registerCreator(AnalysisObservables.IS_ENACTING, null, new SingleObservationCreator(0.0)); // dflt: not enacting
        
        registerPart(PartType.PIPELINE, 
            TimeBehavior.LATENCY, TimeBehavior.THROUGHPUT_ITEMS, TimeBehavior.THROUGHPUT_VOLUME, 
            FunctionalSuitability.ACCURACY_CONFIDENCE, FunctionalSuitability.ACCURACY_ERROR_RATE, 
            ResourceUsage.CAPACITY, ResourceUsage.EXECUTORS, ResourceUsage.TASKS, ResourceUsage.HOSTS, 
                ResourceUsage.USED_DFES, ResourceUsage.USED_CPUS,
            Scalability.VOLUME, Scalability.VELOCITY, Scalability.VOLATILITY, Scalability.VARIETY, Scalability.ITEMS,
            AnalysisObservables.IS_VALID, AnalysisObservables.IS_ENACTING);
        registerPart(PartType.PIPELINE_NODE, 
            TimeBehavior.LATENCY, TimeBehavior.ENACTMENT_DELAY, TimeBehavior.THROUGHPUT_ITEMS, 
                TimeBehavior.THROUGHPUT_VOLUME, 
            ResourceUsage.USED_MEMORY, ResourceUsage.CAPACITY, ResourceUsage.EXECUTORS, ResourceUsage.TASKS, 
                ResourceUsage.HOSTS, ResourceUsage.USED_CPUS, ResourceUsage.USED_DFES,
            FunctionalSuitability.ACCURACY_CONFIDENCE, FunctionalSuitability.COMPLETENESS,
                FunctionalSuitability.BELIEVABILITY, FunctionalSuitability.RELEVANCY,
            Scalability.VOLUME, Scalability.VELOCITY, Scalability.VOLATILITY, Scalability.VARIETY, Scalability.ITEMS, 
                Scalability.PREDECESSOR_ITEMS, Scalability.PREDICTED_ITEMS_THRESHOLD,
            AnalysisObservables.IS_VALID, AnalysisObservables.IS_ENACTING);
        registerPart(PartType.ALGORITHM, 
            TimeBehavior.LATENCY, TimeBehavior.THROUGHPUT_ITEMS, 
                TimeBehavior.THROUGHPUT_VOLUME, 
            ResourceUsage.USED_MEMORY,
            FunctionalSuitability.ACCURACY_ERROR_RATE, FunctionalSuitability.ACCURACY_CONFIDENCE, 
                FunctionalSuitability.COMPLETENESS, FunctionalSuitability.BELIEVABILITY, 
                FunctionalSuitability.RELEVANCY,
            Scalability.VOLUME, Scalability.VELOCITY, Scalability.VARIETY, Scalability.ITEMS,
            AnalysisObservables.IS_VALID, AnalysisObservables.IS_ENACTING);
        registerPart(PartType.SOURCE, 
            TimeBehavior.LATENCY, TimeBehavior.THROUGHPUT_ITEMS, TimeBehavior.THROUGHPUT_VOLUME, 
            FunctionalSuitability.ACCURACY_CONFIDENCE, FunctionalSuitability.COMPLETENESS, 
            Scalability.VOLUME, Scalability.VELOCITY, Scalability.VOLATILITY,
            AnalysisObservables.IS_VALID, AnalysisObservables.IS_ENACTING);
        registerPart(PartType.SINK, 
            TimeBehavior.LATENCY, TimeBehavior.THROUGHPUT_ITEMS, TimeBehavior.THROUGHPUT_VOLUME, 
            FunctionalSuitability.ACCURACY_ERROR_RATE, FunctionalSuitability.ACCURACY_CONFIDENCE,
            AnalysisObservables.IS_VALID, AnalysisObservables.IS_ENACTING);
        registerPart(PartType.PLATFORM, 
            ResourceUsage.AVAILABLE_MACHINES, ResourceUsage.AVAILABLE_DFES, 
                ResourceUsage.USED_MACHINES, ResourceUsage.USED_DFES, CloudResourceUsage.BANDWIDTH);
        registerPart(PartType.MACHINE, 
            CloudResourceUsage.BANDWIDTH, ResourceUsage.AVAILABLE, ResourceUsage.AVAILABLE_CPUS, ResourceUsage.LOAD, 
                ResourceUsage.AVAILABLE_FREQUENCY, ResourceUsage.AVAILABLE_MEMORY, ResourceUsage.USED_MEMORY);
        registerPart(PartType.CLUSTER, 
            ResourceUsage.AVAILABLE_CPUS, ResourceUsage.AVAILABLE_DFES, ResourceUsage.USED_CPUS, 
                ResourceUsage.USED_DFES, CloudResourceUsage.BANDWIDTH, ResourceUsage.AVAILABLE);
        registerPart(PartType.CLOUDENV,
            CloudResourceUsage.BANDWIDTH, CloudResourceUsage.PING, CloudResourceUsage.USED_HARDDISC_MEM, 
            CloudResourceUsage.USED_PROCESSORS, CloudResourceUsage.USED_WORKING_STORAGE, 
            AnalysisObservables.IS_VALID, AnalysisObservables.IS_ENACTING);
    }
    
}