package eu.qualimaster.monitoring.volumePrediction;

import java.util.HashMap;

import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;

/**
 * Main class implementing the available methods of the volume prediction.
 * 
 * @author  Andrea Ceroni
 */
public class VolumePredictor {
	
	/** Map containing the terms (either stocks or hashtags) for which a prediction model is available (along with the corresponding model) */
	private HashMap<String,PredictionModel> models;

    /**
     * A handler for upcoming data sources. Transports the historical data providers if available.
     * 
     * @author  Andrea Ceroni
     */
    private static class HistoricalDataProviderRegistrationEventHandler extends EventHandler<HistoricalDataProviderRegistrationEvent> {

        /**
         * Creates an instance.
         */
        protected HistoricalDataProviderRegistrationEventHandler() {
            super(HistoricalDataProviderRegistrationEvent.class);
        }

        @Override
        protected void handle(HistoricalDataProviderRegistrationEvent event) {
            // TODO called when a data source comes up in a pipeline. Carries the historical data provider.
            // If the source changes, an event with the same pipeline / element name will occur
        }
	    
    }

    /**
     * A handler for upcoming data sources. Transports the historical data providers if available.
     * 
     * @author  Andrea Ceroni
     */
    private static class SourceVolumeMonitoringEventHandler extends EventHandler<SourceVolumeMonitoringEvent> {

        /**
         * Creates an instance.
         */
        protected SourceVolumeMonitoringEventHandler() {
            super(SourceVolumeMonitoringEvent.class);
        }

        @Override
        protected void handle(SourceVolumeMonitoringEvent event) {
            // TODO called when aggregated source volume information is available. The frequency is by default 60000ms
            // but can be defined in the infrastructure configuration via QM-IConf. May be delayed if the source does 
            // not emit data. No data is aggregated in the source if the getAggregationKey(.) method returns null.
        }
            
    }
    
    /**
     * Called upon startup of the infrastructure.
     */
    public static void start() {
	EventManager.register(new HistoricalDataProviderRegistrationEventHandler());
        EventManager.register(new SourceVolumeMonitoringEventHandler());
    }

    /**
     * Notifies the predictor about changes in the lifecycle of pipelines.
     *  
     * @param event the lifecycle event
     */
    public static void notifyPipelineLifecycleChange(PipelineLifecycleEvent event) {
    }

    /**
     * Called upon shutdown of the infrastructure. Clean up global resources here.
     */
    public static void stop() {
    }

}
