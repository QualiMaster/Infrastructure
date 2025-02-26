package eu.qualimaster.monitoring.volumePrediction;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import eu.qualimaster.dataManagement.events.HistoricalDataProviderRegistrationEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;
import eu.qualimaster.monitoring.events.SourceVolumePredictionRequest;
import eu.qualimaster.monitoring.events.SourceVolumePredictionResponse;
import eu.qualimaster.monitoring.utils.IScheduler;

/**
 * Entrance point for the volume prediction, it handles the events and provides
 * the methods to use the functionalities of the volume prediction. It contains
 * a source-predictor map to efficiently resolve which predictor to use based on
 * the source.
 * 
 * @author Andrea Ceroni
 */
public class VolumePredictionManager {

    private static final String DEFAULT_FILE_NAME = "historical_data.txt";

    private static final HistoricalDataProviderRegistrationEventHandler HISTORICAL_DATA_REGISTRATION_EVENT_HANDLER = new HistoricalDataProviderRegistrationEventHandler();

    private static final SourceVolumePredictionRequestHandler SOURCE_VOLUME_PREDICTION_REQUEST_HANDLER = new SourceVolumePredictionRequestHandler();

    /**
     * Set of available volume predictors, one for each different source.
     */
    private static HashMap<String, VolumePredictor> volumePredictors = new HashMap<>();

    /**
     * Indicates whether the prediction is running in "test" mode or not.
     */
    private static boolean test = true;

    private static Map<String, Map<String, Double>> testBlindPredictions;

    /** Indicates the status of the component */
    private static String status = "idle";
    
    /** Used to print error messages only once for the same pipeline. */
    private static HashSet<String> pipelinesWithErrors = new HashSet<>();

    /**
     * Initializes the volume predictor for a given source (either Spring or
     * Twitter), assuming that the data provider has been already set via the
     * proper event. This must be called before feeding the predictor with
     * volume data, with enough advance to let the predictors (one for each
     * input term) be trained.
     * 
     * @param s the source the predictor will refer to.
     * @param monitoredTerms the initial set of terms to be monitored and for
     *        which a predictor must be trained. It can be null or empty, in
     *        this case the predictor will not be able to make any prediction
     *        and will need to be updated at some point.
     * @param blindTerms the initial set of terms whose historical volume can
     *        looked up. It can be null or empty.
     * @param path the path of a temporary file used within the predictor to
     *        store and read data.
     */
    public static void initialize(String source,
            HashSet<String> monitoredTerms, HashSet<String> blindTerms,
            String path) {
        VolumePredictor predictor = volumePredictors.get(source);
        if (predictor != null)
            predictor.initialize(monitoredTerms, blindTerms, path);
        else{
            if (!pipelinesWithErrors.contains(source)){
                System.out
                .println(" "
                        + source);
                pipelinesWithErrors.add(source);
            }
        }   
    }

    /**
     * Performs a blind prediction (i.e. only based on historical data, if
     * available) for a given term in a given source
     * 
     * @param source the source containing the term
     * @param term the term whose volume has to be predicted
     * @return the predicted volume of the term; -1 if a model with historical
     *         data for the input term is not available.
     */
    public static double predictBlindly(String source, String term) {
        VolumePredictor predictor = volumePredictors.get(source);
        if (predictor != null)
            return predictor.predictBlindly(term);
        else{
            if (!pipelinesWithErrors.contains(source)){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + source);
                pipelinesWithErrors.add(source);
            }
            return -1;
        }
    }

    /**
     * Updates the models for all the terms in all the sources.
     */
    public static void updatePredictors() {
        for (String source : volumePredictors.keySet()) {
            updatePredictorsForSource(source);
        }
    }

    /**
     * Updates the predictors for all the terms in a single source
     * 
     * @param source
     */
    public static void updatePredictorsForSource(String source) {
        VolumePredictor predictor = volumePredictors.get(source);
        if (predictor != null)
            predictor.updatePrediction();
        else{
            if (!pipelinesWithErrors.contains(source)){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + source);
                pipelinesWithErrors.add(source);
            }
        } 
    }

    /**
     * Trains a model for a new term to be monitored and adds it to the
     * predictor (useful for having a model ready before the term is added to
     * the source).
     * 
     * @param source the source that the term belongs to
     * @param term the new monitored term to be added
     */
    public static void addMonitoredTerm(String source, String term) {
        VolumePredictor predictor = volumePredictors.get(source);
        if (predictor != null)
            predictor.addMonitoredTerm(term);
        else{
            if (!pipelinesWithErrors.contains(source)){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + source);
                pipelinesWithErrors.add(source);
            }
        }     
    }

    /**
     * Removes a monitored term (and the corresponding model) from the
     * predictor.
     * 
     * @param source the source that the term belongs to
     * @param term the monitored term to be removed
     */
    public static void removeMonitoredTerm(String source, String term) {
        VolumePredictor predictor = volumePredictors.get(source);
        if (predictor != null)
            predictor.removeMonitoredTerm(term);
        else{
            if (!pipelinesWithErrors.contains(source)){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + source);
                pipelinesWithErrors.add(source);
            }
        }
    }

    /**
     * Creates a blind model for a new term and adds it to the predictor.
     * 
     * @param source the source that the term belongs to
     * @param term the blind term to be added
     */
    public static void addBlindTerm(String source, String term) {
        VolumePredictor predictor = volumePredictors.get(source);
        if (predictor != null)
            predictor.addBlindTerm(term);
        else{
            if (!pipelinesWithErrors.contains(source)){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + source);
                pipelinesWithErrors.add(source);
            }
        }
    }

    /**
     * Removes a blind term (and the corresponding bling model) from the
     * predictor
     * 
     * @param source the source that the term belongs to
     * @param term the blind term to be removed
     */
    public static void removeBlindTerm(String source, String term) {
        VolumePredictor predictor = volumePredictors.get(source);
        if (predictor != null)
            predictor.removeBlindTerm(term);
        else{
            if (!pipelinesWithErrors.contains(source)){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + source);
                pipelinesWithErrors.add(source);
            }
        }
    }

    /**
     * Updates the volume threshold for a given monitored term.
     * 
     * @param source the source that the term belongs to
     * @param term the monitored term whose threshold has to be changed
     * @param threshold the new volume threshold for the monitored term
     */
    // public static void updateTermThreshold(String source, String term){
    // VolumePredictor predictor = volumePredictors.get(source);
    // if(predictor != null) predictor.updateTermThreshold(term);
    // else
    // System.out.println("ERROR: no volume predictor available for the input source"
    // + source);
    // }

    /**
     * A handler for upcoming data sources. Transports the historical data
     * providers if available.
     * 
     * @author Andrea Ceroni
     */
    private static class HistoricalDataProviderRegistrationEventHandler extends
            EventHandler<HistoricalDataProviderRegistrationEvent> {

        /**
         * Creates an instance.
         */
        protected HistoricalDataProviderRegistrationEventHandler() {
            super(HistoricalDataProviderRegistrationEvent.class);
        }

        @Override
        protected void handle(HistoricalDataProviderRegistrationEvent event) {
            // called when a data source comes up in a pipeline. Carries the
            // historical data provider.
            // If the source changes, an event with the same pipeline / element
            // name will occur
            System.out
                    .println("HistoricalDataProviderRegistrationEvent received...");
            
            printForDebug(event);
            
            status = "initializing";
            VolumePredictor predictor = new VolumePredictor(
                    event.getPipeline(), event.getSource(),
                    event.getProvider(), event.getIdsNamesMap(), test);
            predictor.initialize(MonitoringConfiguration
                    .getVolumeModelLocation()
                    + "/"
                    + event.getSource()
                    + "_"
                    + DEFAULT_FILE_NAME);
            volumePredictors.put(event.getSource(), predictor);
            status = "ready";
            
//            System.out.println();
//            System.out.println("Available predictors:");
//            for(String key : volumePredictors.keySet()){
//                VolumePredictor p = volumePredictors.get(key);
//                System.out.println("source = " + p.getSourceName());
//                for(String idKey : p.getIdsToNamesMap().keySet()){
//                    System.out.println("term id = " + idKey + ", termName = " + p.getIdsToNamesMap().get(idKey));
//                }
//                String toPrint = "Monitored terms: ";
//                for(String model : p.getMonitoredTerms())
//                    toPrint += model + ", ";
//                System.out.println(toPrint);
//                System.out.println();
//            }
//            System.out.println();
            
            //warmup
            warmUp(MonitoringConfiguration.getVolumeModelLocation() + "/warmupData/");
        }
    }

    /**
     * A handler for source volume prediction requests. Leads to a
     * {@link SourceVolumePredictionResponse}.
     * 
     * @author Andrea Ceroni
     * @author Holger Eichelberger
     */
    private static class SourceVolumePredictionRequestHandler extends
            EventHandler<SourceVolumePredictionRequest> {

        /**
         * Creates an instance.
         */
        protected SourceVolumePredictionRequestHandler() {
            super(SourceVolumePredictionRequest.class);
        }

        @Override
        protected void handle(SourceVolumePredictionRequest event) {
            Map<String, Double> predictions = new HashMap<String, Double>();
            String source = event.getSource();
            for (int k = 0; k < event.getKeywordCount(); k++) {
                String keyword = event.getKeyword(k);
                Map<String, Double> blindTestdata = null == testBlindPredictions ? null
                        : testBlindPredictions.get(source);
                if (null != blindTestdata) {
                    predictions.put(keyword, blindTestdata.get(keyword));
                } else {
                    predictions.put(keyword,
                            predictBlindly(event.getSource(), keyword));
                }
            }
            EventManager.send(new SourceVolumePredictionResponse(event,
                    predictions));
        }

    }

    /**
     * Is called when the monitoring manager receives a
     * {@link SourceVolumeMonitoringEvent}. Although a full event bus handler
     * would also do the job, this shall be less resource consumptive as the
     * event is anyway received in the Monitoring Layer.
     * 
     * @param event the event
     */
    public static void notifySourceVolumeMonitoringEvent(
            SourceVolumeMonitoringEvent event) {
        // called when aggregated source volume information is available. The
        // frequency is by default 60000ms
        // but can be defined in the infrastructure configuration via QM-IConf.
        // May be delayed if the source does
        // not emit data. No data is aggregated in the source if the
        // getAggregationKey(.) method returns null.

        // TODO handle exceptions like: source map does not contain an input
        // term; the model for a source is not available (null);

        // use the right predictor (based on the source) to handle the
        // prediction for the incoming terms
        
//        System.out.println("Received monitoring for prediction:");
//        System.out.println("source = " + event.getPipelineElement());
//        for(String key : event.getObservations().keySet())
//            System.out.println("key = " + key + ", value = " + event.getObservations().get(key));
        
//        System.out.println();
//        System.out.println("Available predictors:");
//        for(String key : volumePredictors.keySet()){
//            VolumePredictor p = volumePredictors.get(key);
//            System.out.println("source = " + p.getSourceName());
//            for(String idKey : p.getIdsToNamesMap().keySet()){
//                System.out.println("term id = " + idKey + ", termName = " + p.getIdsToNamesMap().get(idKey));
//            }
//            String toPrint = "Monitored terms: ";
//            for(String model : p.getMonitoredTerms())
//                toPrint += model + ", ";
//            System.out.println(toPrint);
//            System.out.println();
//        }
//        System.out.println();
        
        status = "monitoring";
        VolumePredictor predictor = volumePredictors.get(event
                .getPipelineElement());
        if (predictor != null){
//            System.out.println("Predictor exists for source " + event.getPipelineElement());
            predictor.handlePredictionStep(event.getObservations());
        }
        else{
            if (!pipelinesWithErrors.contains(event.getPipelineElement())){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + event.getPipelineElement());
                pipelinesWithErrors.add(event.getPipelineElement());
            }
        }
        status = "ready";
    }

    /**
     * Called upon startup of the infrastructure.
     * 
     * @param scheduler a scheduler instance for regular tasks
     */
    public static void start(IScheduler scheduler) {
        EventManager.register(HISTORICAL_DATA_REGISTRATION_EVENT_HANDLER);
        EventManager.register(SOURCE_VOLUME_PREDICTION_REQUEST_HANDLER);

        // this is rather initial - each day at 2:00
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR, 2);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        scheduler.schedule(new ModelUpdateTask(), cal.getTime(),
                24 * 60 * 60 * 1000);
    }

    /**
     * Notifies the predictor about changes in the lifecycle of pipelines.
     * 
     * @param event the lifecycle event
     */
    public static void notifyPipelineLifecycleChange(
            PipelineLifecycleEvent event) {
    }

    /**
     * Called upon shutdown of the infrastructure. Clean up global resources
     * here.
     */
    public static void stop() {
        EventManager.unregister(HISTORICAL_DATA_REGISTRATION_EVENT_HANDLER);
        EventManager.unregister(SOURCE_VOLUME_PREDICTION_REQUEST_HANDLER);
        pipelinesWithErrors.clear();
    }

    /**
     * Simulates the handle of a HistoricalDataProviderRegistrationEvent (for
     * testing purposes)
     * 
     * @param event the event carrying the historical data handler
     * @param folder the folder where to write temporary data
     */

    public static void handleHistoricalDataProviderRegistrationEventTest(
            HistoricalDataProviderRegistrationEvent event, String folder) {
        VolumePredictor predictor = new VolumePredictor(event.getPipeline(),
                event.getSource(), event.getProvider(), event.getIdsNamesMap(),
                test);
        predictor.initialize(folder + event.getSource() + "_"
                + DEFAULT_FILE_NAME);
        volumePredictors.put(event.getSource(), predictor);
    }

    public static void warmUp(String folder) {
        for (VolumePredictor vp : volumePredictors.values()) {
            vp.warmUp(folder);
        }
    }

    public static void handleNotifySourceVolumeMonitoringEventTest(
            SourceVolumeMonitoringEvent event) {
        VolumePredictor predictor = volumePredictors.get(event
                .getPipelineElement());
        if (predictor != null)
            predictor.handlePredictionStep(event.getObservations());
        else{
            if (!pipelinesWithErrors.contains(event.getPipelineElement())){
                System.out
                .println("ERROR: no volume predictor available for the input source "
                        + event.getPipelineElement());
                pipelinesWithErrors.add(event.getPipelineElement());
            }
        }
    }

    /**
     * @return the test
     */
    public static boolean isTest() {
        return test;
    }

    /**
     * @param test the test to set
     */
    public static void setTest(boolean t) {
        test = t;
    }

    /**
     * Gets the predictor corresponding to a given source
     * 
     * @param source the source
     * @return
     */
    public static VolumePredictor getPredictor(String source) {
        return volumePredictors.get(source);
    }

    /**
     * @return the status
     */
    public static String getStatus() {
        return status;
    }

    /**
     * Sets blind prediction values for testing.
     * 
     * @param predictions the predictions, i.e., a source-keyword-value mapping
     */
    public static void setBlindTestPredictions(
            Map<String, Map<String, Double>> predictions) {
        testBlindPredictions = predictions;
    }
    
    private static void printForDebug(HistoricalDataProviderRegistrationEvent event){
        System.out.println("TEST MODE = " + test);
        System.out.println("PIPELINE: " + event.getPipeline());
        System.out.println("SOURCE: " + event.getSource());
        if(event.getIdsNamesMap() != null){
            System.out.println("IDS-NAME MAP:");
            Map<String, String> map = event.getIdsNamesMap();
            for(String key : map.keySet())
                System.out.println("\tkey = " + key + ", value = " + map.get(key));
        }
        else System.out.println("IDS-NAME MAP = null");
        if(event.getProvider() != null){
            System.out.print("DEFAULT BLIND TERMS: ");
            for(String s : event.getProvider().getDefaultBlindTerms())
                System.out.print(s + ", ");
            System.out.println();
            System.out.print("DEFAULT MONITORING TERMS: ");
            for(String s : event.getProvider().getDefaultMonitoredTerms())
                System.out.print(s + ", ");
            System.out.println();
        }
        else System.out.println("HISTORICAL DATA PROVIDER = null");
        
        System.out.println("MAIN FOLDER = " + MonitoringConfiguration.getVolumeModelLocation());
    }
}
