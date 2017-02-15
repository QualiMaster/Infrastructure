package eu.qualimaster.monitoring.volumePrediction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.adaptation.events.SourceVolumeAdaptationEvent;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;
import eu.qualimaster.dataManagement.sources.TwitterHistoricalDataProvider;
import eu.qualimaster.dataManagement.storage.hbase.HBaseStorageSupport;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.MonitoringConfiguration;

/**
 * Main class implementing the available methods of the volume prediction.
 * 
 * @author Andrea Ceroni
 */
public class VolumePredictor {

    /** The pipeline the predictor refers to. */
    private String pipeline;

    /** The source the predictor refers to. */
    private String source;

    /**
     * Set of terms (either stocks or hashtags) monitored by the volume
     * prediction.
     */
    private HashSet<String> monitoredTerms;

    /**
     * Set of terms (either stocks or hashtags) for which historical data might
     * be looked up as blind prediction.
     */
    private HashSet<String> blindTerms;

    /**
     * Recent observed volumes of the monitored terms, used to decide whether to
     * raise alarms or not
     */
    private HashMap<String, ArrayList<Long>> recentVolumes;

    /**
     * Map containing the term (either stocks or hashtags) for which a
     * prediction model is available (along with the corresponding model)
     */
    private HashMap<String, Prediction> models;

    /**
     * Map containing the term (either stocks or hashtags) for which a "blind"
     * prediction model is available (along with the corresponding model)
     */
    private HashMap<String, BlindPrediction> blindModels;

    /** The status of the component (whether it is running or not) */
    private boolean running;

    /** The provider of historical data */
    private IHistoricalDataProvider historyProvider;

    /** File to temporarily store historical data */
    private File historicalDataFile;

    /** Mapping between ids and names of source terms */
    private Map<String, String> idsToNamesMap;

    /** Flag indicating whether the class is being used in test mode or not */
    private boolean test;

    /** Alarm event at the current time point (null if there was no event) */
    AdaptationEvent adaptationEvent;
    
    /** Last alarm raised */
    private SourceVolumeAdaptationEvent lastAlarm;

    /**
     * The number of months (in milliseconds) of data to consider when training
     * the model
     */
    private static final long NUM_MONTHS = 4l * (1000l * 60l * 60l * 24l * 30l);

    /** The format for storing dates */
    private static final String DATE_FORMAT = "MM/dd/yyyy','HH:mm:ss";

    /** The format for storing dates */
    private static final String DATE_FORMAT_WEKA = "yyyy-MM-dd'T'HH:mm:ss";

    /** The size of the recent history (number of time points) */
    private static final int RECENT_HISTORY_SIZE = 10;
    
    /** The number of data points to forecast */
    private static final int POINTS_TO_FORECAST = 5;

    /**
     * The number of recent data points for checking small but regular increases
     */
    private static final int REGULAR_INCREASE_SIZE = RECENT_HISTORY_SIZE / 3;

    /** Url used to retrieve historical data in test mode */
    private static final String TEST_URL = "test";

    private static final String TEST_HISTORICAL_FOLDER = "/historicalData/";
    private static final String TEST_WARMUP_FOLDER = "/warmupData/";
    private static final String TEST_STREAMING_FOLDER = "/streamingData/";

    /**
     * Constructor of the predictor, no models are trained yet.
     */
    public VolumePredictor(String pipeline, String source,
            IHistoricalDataProvider dataProvider,
            Map<String, String> idsToNamesMap, boolean test) {
        this.pipeline = pipeline;
        this.source = source;
        this.monitoredTerms = null;
        this.blindTerms = null;
        this.recentVolumes = null;
        this.models = null;
        this.blindModels = null;
        this.running = false;
        this.historyProvider = dataProvider;
        this.historicalDataFile = null;
        this.idsToNamesMap = formatMap(idsToNamesMap);
        this.test = test;
        this.lastAlarm = null;
    }

    /**
     * Constructor of the predictor, no models are trained yet.
     */
    public VolumePredictor(String pipeline, String source,
            IHistoricalDataProvider dataProvider,
            Map<String, String> idsToNamesMap) {
        this(pipeline, source, dataProvider, idsToNamesMap, false);
    }

    /**
     * Initializes the volume predictor with a set of terms to be monitored and
     * a set of terms to be looked up for blind prediction, assuming that the
     * data provider has been already set.
     * 
     * @param monitoredSources the initial set of terms to be monitored and for
     *        which a predictor must be trained. It can be null or empty, in
     *        this case the predictor will not be able to make any prediction
     *        and will need to be updated at some point.
     * @param blindSources the initial set of terms whose historical volume can
     *        looked up. It can be null or empty.
     * @param filePath the path of a temporary file used to store and read data.
     */
    public void initialize(HashSet<String> monitoredTerms,
            HashSet<String> blindTerms, String filePath) {
        // check if the historical data provider has been set
        if (this.historyProvider == null) {
            System.out
                    .println("ERROR: no historical data provider has been set.");
            return;
        }

        if (monitoredTerms != null)
            this.monitoredTerms = new HashSet<>(monitoredTerms);
        else
            this.monitoredTerms = new HashSet<>();
        if (blindTerms != null)
            this.blindTerms = new HashSet<>(blindTerms);
        else
            this.blindTerms = new HashSet<>();
        this.recentVolumes = new HashMap<>();
        for (String term : this.monitoredTerms)
            this.recentVolumes.put(term, new ArrayList<Long>());
        this.running = false;
        this.historicalDataFile = new File(filePath);
        this.models = new HashMap<>();
        this.blindModels = new HashMap<>();
        initializeModels(this.models, this.blindModels);
    }

    /**
     * Initializes the volume predictor using the default terms (monitored and
     * blind) contained within the historical data handler, assuming that the
     * data provider has been already set.
     * 
     * @param monitoredSources the initial set of terms to be monitored and for
     *        which a predictor must be trained. It can be null or empty, in
     *        this case the predictor will not be able to make any prediction
     *        and will need to be updated at some point.
     * @param blindSources the initial set of terms whose historical volume can
     *        looked up. It can be null or empty.
     * @param filePath the path of a temporary file used to store and read data.
     */
    public void initialize(String filePath) {
        System.out.println("TMP FILE FOR HISTORICAL DATA: " + filePath);
        System.out.println("TEST = " + this.test);
        
        // check if the historical data provider has been set
        if (this.historyProvider == null) {
            System.out
                    .println("ERROR: no historical data provider has been set.");
           System.out.println("Ignoring Volume Prediction...");
            return;
        }

        HashSet<String> monitoredTerms = this.historyProvider
                .getDefaultMonitoredTerms();
        HashSet<String> blindTerms = this.historyProvider
                .getDefaultBlindTerms();
        initialize(monitoredTerms, blindTerms, filePath);
        
        System.out.println("Volume Prediction model for source " + this.source + " initialized.");
    }

    /**
     * Makes a blind prediction (based on historical data) of the volume of a
     * term that is not being monitored.
     * 
     * @param term the not monitored term whose volume has to be predicted.
     * @return the predicted volume of the term; -1 if a model with historical
     *         data for the input term is not available.
     */
    public double predictBlindly(String termId) {
        // get the name of the source term from its id
        String termName;
        if (!this.idsToNamesMap.isEmpty())
            termName = this.idsToNamesMap.get(termId);
        else
            termName = termId;

        BlindPrediction model = this.blindModels.get(termName);
        if (model != null)
            return model.predictBlindly();
        else {
            // add the term to the set of blind models with a null model, so
            // that a model for this new term will be trained during the next
            // update.
            this.blindModels.put(termName, null);
            this.blindTerms.add(termName);
            return -1;
        }
    }

    /**
     * Processes the current set of observed volumes: prediction, evaluation,
     * storage.
     * 
     * @param observations the term-volume map containing the observed volumes
     *        for each term
     */
    public void handlePredictionStep(Map<String, Integer> observations) {
//        System.out.println("Handling prediction step...");
        String toPrint = "";
        String timestamp = getTimestamp();
        toPrint += "measured timestamp = " + timestamp + "\t";
        HashMap<String, Double> alarms = new HashMap<>();
        HashMap<String, Double> normalizedAlarms = new HashMap<>();
        HashMap<String, Double> durations = new HashMap<>();
        HashMap<String, Long> volumesForEvent = new HashMap<>();
        HashMap<String, Double> predictionsForEvent = new HashMap<>();
        HashMap<String, Double> thresholdsForEvent = new HashMap<>(); 
        ArrayList<String> unknownTerms = new ArrayList<>();
        for (String termId : observations.keySet()) {
//            System.out.println("Term id = " + termId);
            // get the name of the source term from its id
            String termName;
            if (!this.idsToNamesMap.isEmpty())
                termName = this.idsToNamesMap.get(termId);
            else
                termName = termId;
            
//            System.out.println("Term name = " + termName);

            long currVolume = (long) observations.get(termId);
            
            // ignore first monitoring (= 1)
            if(currVolume == 1){
                System.out.println("First monitoring, it will be ignored.");
                continue;
            }
            
            toPrint += "current volume = " + currVolume + "\t";
            Prediction model = null == this.models ? null : this.models.get(termName);

            // add the current observation to the recent volumes for the current
            // term
            addRecentVolume(termName, currVolume);

            if (model != null && model.getForecaster() != null) {
                // for test cases, derive the current date by incrementing the
                // date of the last observation by the desired granularity
                if (this.test) {
                    if(model.getRecentVolumes().size() > 0){
                        long lastTime = (long) model.getRecentVolumes()
                                .instance(model.getRecentVolumes().size() - 1)
                                .value(0);
                        timestamp = getTimestamp(lastTime + 30000);
                    }
                }
                toPrint += "model timestamp = " + timestamp + "\t";

                // update the recent values within the model
                model.updateRecentVolumes(timestamp, currVolume);

                // predict the volume within the next time step
                double[] predictions = model.predict(POINTS_TO_FORECAST);
                toPrint += "predicted volume = " + (int) predictions[0] + "\t";

                // check whether the predicted volume is critical and, if so,
                // include the term when raising the alarm
                double[] alarm = evaluatePrediction(termName, predictions);
                if (alarm[0] != -1) {
                    alarms.put(termId, alarm[0]);
                    normalizedAlarms.put(termId, alarm[0] / currVolume);
                    durations.put(termId, alarm[1]);
                    volumesForEvent.put(termId, currVolume);
                    predictionsForEvent.put(termId, predictions[0]);
                    thresholdsForEvent.put(termId, alarm[2]);
                }
                toPrint += "alarms = " + alarm[0] + "\t" + alarm[1] + "\t" + "|" + "\t";
                System.out.println(toPrint);
            } else {
                System.out.println("No predictors available for term " + termName);
                if (!this.monitoredTerms.contains(termName))
                    unknownTerms.add(termName);
            }

            // store the current observation in the historical data of the
            // current term (only for twitter)
            storeInHistoricalData(termName, timestamp, currVolume);
            
            if(this.lastAlarm != null && currVolume < this.lastAlarm.getVolumes().get(termId)){
                System.out.println("Critical period for term " + termId + " is over");
                this.lastAlarm = removeTermFromAlarm(this.lastAlarm, termId);
            }
        }
        System.out.print("\n");

        // raise an alarm to the adaptation layer containing all the critical
        // terms and their volumes
        if (!alarms.isEmpty())
            raiseAlarms(alarms, normalizedAlarms, durations,
                        volumesForEvent, predictionsForEvent, thresholdsForEvent);
        else
            this.adaptationEvent = null;
        
        // check if the critical period is over for ALL the terms. If so, signal this
        // to the adaptation layer by sending an empty SourceVolumeAdaptationEvent
        if(this.lastAlarm != null && this.lastAlarm.getFindings().isEmpty()){
            System.out.println("Critical period is over for all terms, sending signal to Adaptation Layer");
            this.lastAlarm = null;
            EventManager.send(new SourceVolumeAdaptationEvent(this.pipeline, this.source));
        }

        // initialize one predictor for each unknown term that was observed in
        // the source
        for (String term : unknownTerms)
            addMonitoredTerm(term);
    }

    private String getTimestamp() {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT_WEKA);
        Date date = new Date();
        return format.format(date);
    }

    private String getTimestamp(Long time) {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT_WEKA);
        Date date = new Date(time);
        return format.format(date);
    }

    public void stopPrediction() {
        this.running = false;
    }

    /**
     * Updates the prediction models of each monitored and blind term.
     */
    public void updatePrediction() {
        // create the new models in separate objects not to interfere with any
        // prediction that might be running
        HashMap<String, Prediction> newModels = new HashMap<>();
        HashMap<String, BlindPrediction> newBlindModels = new HashMap<>();
        initializeModels(newModels, newBlindModels);

        // change the models
        this.models.clear();
        this.models.putAll(newModels);
        this.blindModels.clear();
        this.blindModels.putAll(newBlindModels);
    }

    private double[] evaluatePrediction(String term, double[] predictions) {
        // handles 2 cases:
        // - signal high peaks (using recent history to compute avg should
        // detect these)
        // - signal slightly but continuously increasing volumes: dangerous
        // because the recent history is updated
        // with always increasing values so the new values, although higher, do
        // not result in alarms

        // an alarm is made of a magnitude and a duration probability
        double[] alarm = new double[3];
        
        // compute average and std deviation within the recent history
        ArrayList<Long> recentVolumesForTerm = this.recentVolumes.get(term);
        double[] stats = computeStatistics(recentVolumesForTerm);

        // compute alarm threshold from avg and std and compare it with the
        // predicted volume
        double threshold = stats[0] + 2 * stats[1];
        // System.out.print((int)stats[0] + "\t");
        // System.out.print((int)stats[1] + "\t");
        System.out.println("threshold = " + (int) threshold + "\t");
        if (predictions[0] > threshold) {
            Long current = recentVolumesForTerm
                    .get(recentVolumesForTerm.size() - 1);
            if (predictions[0] > current){
                alarm[0] = (double) (predictions[0] - current);
                alarm[1] = estimateDuration(predictions, threshold, current);
                alarm[2] = threshold;
                return alarm;
            }
            
            else
                return new double[]{-1,-1};
            // return (prediction - threshold);
        }

        // check the trend of the recent volumes and signal if it is always
        // increasing
        // if(checkIncrease(recentVolumesForTerm, REGULAR_INCREASE_SIZE)) return
        // computeIncrease(recentVolumesForTerm);

        else
            return new double[]{-1,-1};
    }
    
    private double estimateDuration(double[] predictions, double threshold, double current){
        
        // TODO try considering: time of the day of the increase, relative/absolute increase (similar to the decrease computation),
        //      number of points in the increase that have high values.
        
        
        double probability = 0;
        
        // contribution of exceeding threshold and current values at the next time points.
        // this part contributes with 0.5 to the duration probability.
        double piece = (1.0 / predictions.length) / 4;
        for(double prediction : predictions){
            if(prediction > current) probability += piece;
            if(prediction > threshold) probability += piece;
        }
        
        // contribution of the difference between the first and last future point.
        // this part contributes with the other 0.5 to the duration probability.
        double trend = Math.abs((predictions[4] - predictions[0]) / predictions[0]);
        probability += 0.5 - trend;
        probability = Math.max(0, probability);
        
        return probability;
    }

    private double[] computeStatistics(ArrayList<Long> data) {
        double[] stats = new double[2];
        double avg, std;
        double sum = 0;
        double sumsq = 0;

        if (data == null || data.isEmpty())
            return stats;

        // compute avg and standard deviation
        for (int i = 0; i < data.size(); i++)
            sum += data.get(i);
        avg = (double) sum / data.size();
        for (int i = 0; i < data.size(); i++)
            sumsq = sumsq + ((data.get(i) - avg) * (data.get(i) - avg));
        std = Math.sqrt((double) sumsq / data.size());

        // store the statistics
        stats[0] = avg;
        stats[1] = std;

        return stats;
    }

    private boolean checkIncrease(ArrayList<Long> history, int size) {
        if (size > history.size() - 1)
            size = history.size() - 1;
        int i = history.size() - 1;
        int count = 0;
        while (count < size) {
            if (history.get(i) < history.get(i - 1))
                return false;
            i--;
            count++;
        }
        return true;
    }

    private int computeIncrease(ArrayList<Long> values) {
        int firstIndex = values.size() - REGULAR_INCREASE_SIZE;
        int lastIndex = values.size() - 1;
        return (int) (values.get(lastIndex) - values.get(firstIndex));
    }

    private void raiseAlarms(HashMap<String, Double> alarms,
            HashMap<String, Double> normalizedAlarms,
            HashMap<String, Double> durations,
            HashMap<String, Long> volumes,
            HashMap<String, Double> predictions,
            HashMap<String, Double> thresholds) {
        
        // use the event class defined in the infrastructure to send alarms to
        // the adaptation layer
        SourceVolumeAdaptationEvent svae = new SourceVolumeAdaptationEvent(
                this.pipeline, this.source, alarms, normalizedAlarms, durations,
                volumes, predictions, thresholds);
        this.adaptationEvent = svae;
        
        // before raising alarms, check if a previous alarm exists and
        // if the new alarms are less critical than it (if so, then do not
        // send any alarm).
        svae = removeNotCriticalAlarms(svae);
        if(svae.getFindings().isEmpty()){
            System.out.println("No new critical alarms found, nothing to signal");
            return;
        }
        
        if(this.lastAlarm == null)
            this.lastAlarm = svae;
        else
            mergeLastAlarms(svae);
            
        EventManager.send(svae);
    }
    
    private SourceVolumeAdaptationEvent removeNotCriticalAlarms(
            SourceVolumeAdaptationEvent event){
        if(this.lastAlarm == null)
            return event;
        ArrayList<String> termsToRemove = new ArrayList<>();
        for(String term : event.getNormalizedFindings().keySet()){
            Map<String, Double> lastNormAlarms = this.lastAlarm.getNormalizedFindings();
            if(lastNormAlarms.containsKey(term) && 
            event.getNormalizedFindings().get(term) <= lastNormAlarms.get(term))
                termsToRemove.add(term);
        }
        
        for(String term : termsToRemove){
            event = removeTermFromAlarm(event, term);
        }
        
        return event;
    }
    
    private SourceVolumeAdaptationEvent removeTermFromAlarm(SourceVolumeAdaptationEvent event, String term){
        event.getFindings().remove(term);
        event.getNormalizedFindings().remove(term);
        event.getDurations().remove(term);
        event.getVolumes().remove(term);
        event.getPredictions().remove(term);
        event.getThresholds().remove(term);
        
        return event;
    }
    
    private void mergeLastAlarms(SourceVolumeAdaptationEvent event){
        this.lastAlarm.getFindings().putAll(event.getFindings());
        this.lastAlarm.getNormalizedFindings().putAll(event.getNormalizedFindings());
        this.lastAlarm.getDurations().putAll(event.getDurations());
        this.lastAlarm.getVolumes().putAll(event.getVolumes());
        this.lastAlarm.getPredictions().putAll(event.getPredictions());
        this.lastAlarm.getThresholds().putAll(event.getThresholds());
    }

    private void addRecentVolume(String term, Long volume) {
        if (this.recentVolumes.containsKey(term)) {
            if (this.recentVolumes.get(term).size() >= RECENT_HISTORY_SIZE)
                this.recentVolumes.get(term).remove(0);
            this.recentVolumes.get(term).add(volume);
        }
    }

    private void initializeModels(HashMap<String, Prediction> models,
            HashMap<String, BlindPrediction> blindModels) {
        // make the union of monitored and blind terms to avoid getting
        // historical data twice (in case a term appears in both the sets)
        HashSet<String> allTerms = new HashSet<>();
        allTerms.addAll(this.monitoredTerms);
        allTerms.addAll(this.blindTerms);
        this.historyProvider.setTest(this.test);

        for (String term : allTerms) {
            System.out.println("Term: " + term);
            getHistoricalData(term, NUM_MONTHS, this.historicalDataFile);

            // monitored models
            if (this.monitoredTerms.contains(term)) {
                Prediction model = new Prediction(term, this.historicalDataFile);
                if (model.getForecaster() == null)
                    model = null;
                models.put(term, model);
            }
            // blind models
            if (this.blindTerms.contains(term)) {
                BlindPrediction model = new BlindPrediction(term,
                        this.historicalDataFile);
                if (model.getHistoricalVolumes() == null)
                    model = null;
                blindModels.put(term, model);
            }
        }
    }

    private void getHistoricalData(String term, long months, File outputFile) {
        try {
            // if(this.test)
            // this.historyProvider.obtainHistoricalData(NUM_MONTHS, term,
            // this.historicalDataFile, TEST_URL);
            if (this.test)
                this.historyProvider.obtainHistoricalData(NUM_MONTHS, term,
                        this.historicalDataFile,
                        MonitoringConfiguration.getVolumeModelLocation()
                                + TEST_HISTORICAL_FOLDER);
            else
                this.historyProvider.obtainHistoricalData(NUM_MONTHS, term,
                        this.historicalDataFile);
        } catch (IOException e) {
            // handle the absence of historical data:
            // clear the content of the historicalDataFile, because it refers to
            // the read of historical data for a previous term
            System.out.println("ERROR: historical data not available for term:"
                    + term);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(
                        this.historicalDataFile));
                writer.write("");
                writer.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Warms the predictor up with recent data (only for testing purposes).
     * 
     * @param dataFolder the folder containing warm up data, one file for each
     *        term monitored by the predictor is expected.
     */
    public void warmUp(String dataFolder) {
        // warm the predictor up only in test mode
        if (!this.test) {
            System.out
                    .println("Predictor not in test mode, warm up is not allowed.");
            return;
        }
        
        System.out.println("Warming up predictor for source" + this.getSourceName());
        
        File directory = new File(dataFolder);
        for (String term : this.monitoredTerms) {
            System.out.println("Warming up model for term " + term);
            File file = null;
            for (File f : directory.listFiles()) {
                if (f.getName().contains(term)) {
                    file = f;
                    break;
                }
            }

            if (file == null) {
                System.out.println("No warm up data available for term: "
                        + term);
                continue;
            }

            TreeMap<String, Long> data = DataUtils.readData(file);
            for (String timestamp : data.keySet()) {
                // feed the recent history within the prediction model (for
                // making predictions)
                this.models.get(term).updateRecentVolumes(timestamp,
                        data.get(timestamp));

                // feed the recent history within the predictor (for computing
                // the volume threshold)
                addRecentVolume(term, data.get(timestamp));
                
                System.out.println("Warmed up with data: [" + timestamp + ", " + data.get(timestamp) + "]");
            }
        }
        
        System.out.println("Predictor warmed up" + this.getSourceName());
    }

    private void storeInHistoricalData(String term, String timestamp, Long value) {
        // store a value in the historical data only for twitter
        if (this.historyProvider.getClass().equals(
                TwitterHistoricalDataProvider.class))
            storeTwitterVolume(timestamp, term, value);
    }

    private void storeTwitterVolume(String timestamp, String term, Long volume) {
        // Get the HBase table containing data for the input term and write into
        // it
        // TODO which strategy should be used?
        HBaseStorageSupport table = (HBaseStorageSupport) DataManager.VOLUME_PREDICTION_STORAGE_MANAGER
                .getTable("", term, null);
        table.connect();
        table.doWrite(timestamp, volume);
        table.disconnect();
    }

    /**
     * Removes a term from the monitored ones
     * 
     * @param term The term to be removed
     */
    public void removeMonitoredTerm(String term) {
        this.monitoredTerms.remove(term);
        this.models.remove(term);
        this.recentVolumes.remove(term);
    }

    /**
     * Adds a term to be monitored and initializes its prediction model. The
     * model is initialized even if a model for the term already exists (because
     * it might be outdated).
     * 
     * @param source The term to be monitored
     */
    public void addMonitoredTerm(String term) {
        getHistoricalData(term, NUM_MONTHS, this.historicalDataFile);

        Prediction model = new Prediction(term, this.historicalDataFile);
        if (model.getForecaster() == null)
            model = null;
        this.models.put(term, model);
        this.monitoredTerms.add(term);
        this.recentVolumes.put(term, new ArrayList<Long>());
    }

    /**
     * Removes a term from the blind ones
     * 
     * @param term The term to be removed
     */
    public void removeBlindTerm(String term) {
        this.blindTerms.remove(term);
        this.blindModels.remove(term);
    }

    /**
     * Adds a blind term and initializes its prediction model. The model is
     * initialized even if a model for the term already exists (because it might
     * be outdated).
     * 
     * @param source The term to be monitored
     */
    public void addBlindTerm(String term) {
        getHistoricalData(term, NUM_MONTHS, this.historicalDataFile);

        BlindPrediction model = new BlindPrediction(term,
                this.historicalDataFile);
        if (model.getHistoricalVolumes() == null)
            model = null;
        this.blindModels.put(term, model);
        this.blindTerms.add(term);
    }

    // public void updateTermThreshold(String term, long threshold){
    // if(this.monitoredTerms.containsKey(term)) this.monitoredTerms.put(term,
    // threshold);
    // else System.out.println("ERROR: the required term is not monitored.");
    // }

    private Map<String, String> formatMap(Map<String, String> inputMap) {
        Map<String, String> newMap = new HashMap<>();
        
        if(inputMap == null) return newMap;
        
        for (String id : inputMap.keySet()) {
            String name = inputMap.get(id);
            System.out.println("Name before formatting = " + name);
            //String[] fields = name.split("�");
            String[] fields = name.split("-");
            if (fields.length > 1)
                name = fields[0] + "-" + fields[1];
            else
                name = fields[0];
            System.out.println("Name after formatting = " + name);
            newMap.put(id, name);
        }
        return newMap;
    }

    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return the historyProvider
     */
    public IHistoricalDataProvider getHistoryProvider() {
        return historyProvider;
    }

    /**
     * @param historyProvider the historyProvider to set
     */
    public void setHistoryProvider(IHistoricalDataProvider historyProvider) {
        this.historyProvider = historyProvider;
    }

    /**
     * @return the monitoredTerms
     */
    public HashSet<String> getMonitoredTerms() {
        return monitoredTerms;
    }

    /**
     * @param monitoredTerms the monitoredTerms to set
     */
    public void setMonitoredTerms(HashSet<String> monitoredTerms) {
        this.monitoredTerms = monitoredTerms;
    }

    /**
     * @return the blindTerms
     */
    public HashSet<String> getBlindTerms() {
        return blindTerms;
    }

    /**
     * @param blindTerms the blindTerms to set
     */
    public void setBlindTerms(HashSet<String> blindTerms) {
        this.blindTerms = blindTerms;
    }

    /**
     * @return the source
     */
    public String getSourceName() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSourceName(String source) {
        this.source = source;
    }

    /**
     * @return the pipeline
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * @param pipeline the pipeline to set
     */
    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * @return the recentVolumes
     */
    public HashMap<String, ArrayList<Long>> getRecentVolumes() {
        return recentVolumes;
    }

    /**
     * @return the test
     */
    public boolean isTest() {
        return test;
    }

    /**
     * @param test the test to set
     */
    public void setTest(boolean test) {
        this.test = test;
    }

    /**
     * @return the adaptation event
     */
    public AdaptationEvent getAdaptationEvent() {
        return this.adaptationEvent;
    }

    /**
     * @return the idsToNamesMap
     */
    public Map<String, String> getIdsToNamesMap() {
        return this.idsToNamesMap;
    }
}
