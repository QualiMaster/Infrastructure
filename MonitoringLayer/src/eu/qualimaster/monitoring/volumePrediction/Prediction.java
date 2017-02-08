package eu.qualimaster.monitoring.volumePrediction;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.core.TSLagMaker;
import weka.classifiers.timeseries.core.TSLagMaker.Periodicity;
import weka.classifiers.timeseries.core.TSLagMaker.PeriodicityHandler;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Implementation of one of the types of volume prediction: the one based on
 * recent values.
 * 
 * @author Andrea Ceroni
 */
public class Prediction {

    /**
     * The name of the term the model refers to (can be either the name of a
     * stock or an hashtag).
     */
    private String source;

    /**
     * The window of recent volume values, used to predict the volume at the
     * next time point.
     */
    private Instances recentVolumes;

    /** The model to make predictions based on recent volume values. */
    private WekaForecaster forecaster;

    /** The number of recent time points to be considered in the model. */
    private static final int NUM_RECENT_VOLUMES = 12;

    /** Name of the volume field */
    private static final String VOLUME_FIELD = "volume";

    /** Name of the date field */
    private static final String DATE_FIELD = "date";

    /** Format of the date field */
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Constructor initializing (training) the model given the available
     * historical data
     * 
     * @param source The name of the source the model makes predictions for
     * @param dataPath The path to the data used for training the model
     */
    public Prediction(String source, File dataFile) {
        this.source = source;
        this.recentVolumes = createDataset();
        this.forecaster = new WekaForecaster();
        trainModel(dataFile);
    }

    /**
     * Predicts the volume during the next time step (1 minute) given the recent
     * values. It requires at least 12 previous points to make the prediction.
     * If not enough recent points are available, the method returns a negative
     * integer indicating the number of steps that still have to be waited
     * before being able to make predictions.
     * 
     * @param pointsToForecast the number of future data points to forecast.
     * @return The predicted volume within the next time steps.
     */
    public double[] predict(int pointsToForecast) {
        try {
            // if there are not enough values in the recent history, return a
            // negative value indicating the steps to wait
            if (this.forecaster.getTSLagMaker().getMaxLag() > this.recentVolumes
                    .size()) {
                System.out
                        .println("Not enough recent values to make predictions.");
                double[] forecast = new double[1];
                forecast[0] = this.recentVolumes.size()
                        - this.forecaster.getTSLagMaker().getMaxLag();
                return forecast;
            }

            // prime the forecaster with enough recent historical data to cover
            // up to the maximum lag
            this.forecaster.primeForecaster(this.recentVolumes);

            // forecast the desired number of data points
            // outer list is over the steps, inner list is over the targets
            List<List<NumericPrediction>> wekaForecast = this.forecaster
                    .forecast(pointsToForecast, System.out);
            //double forecast = wekaForecast.get(0).get(0).predicted();
            
            double[] forecast = new double[pointsToForecast];
            for(int i = 0; i < pointsToForecast; i++) {
                forecast[i] = wekaForecast.get(i).get(0).predicted();
                }

            return forecast;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Appends the input volume (assumed to be the new observed one) to the
     * recent volumes, which are used to make predictions. If the size of the
     * recent volumes exceeds the maximum one, the oldest volume is removed to
     * make room for the new one.
     * 
     * @param time The timestamp when the volume was observed.
     * @param observation The observed volume.
     */
    public void updateRecentVolumes(String time, Long observation) {
        try {
            // Create the instance (according to the structure of the recent
            // volumes) and add it to the recent volumes
            Instance instance = dataToInstance(time, observation,
                    this.recentVolumes);
            if (this.recentVolumes.size() >= NUM_RECENT_VOLUMES)
                this.recentVolumes.remove(0);
            this.recentVolumes.add(instance);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void trainModel(File dataFile) {
        TreeMap<String, Long> trainingData = DataUtils.readData(dataFile);
        if (!trainingData.isEmpty())
            this.forecaster = trainForecaster(trainingData);
        else
            this.forecaster = null;
    }

    private WekaForecaster trainForecaster(TreeMap<String, Long> data) {
        try {
            // data to weka instances
            Instances instances = dataToInstances(data);

            // new forecaster
            WekaForecaster forecaster = new WekaForecaster();

            // set target and date fields
            forecaster.setFieldsToForecast(VOLUME_FIELD);
            forecaster.getTSLagMaker().setTimeStampField(DATE_FIELD);

            // set the underlying classifier
            forecaster.setBaseForecaster(new LinearRegression());

            // detect the periodicity automatically (similarly to the weka gui)
            detectPeriodicity(forecaster, instances, DATE_FIELD);

            // forecaster.getTSLagMaker().setMinLag(1);
            // forecaster.getTSLagMaker().setMaxLag(12); // monthly data

            // add a month of the year indicator field
            // forecaster.getTSLagMaker().setAddMonthOfYear(true);

            // add a quarter of the year indicator field
            // forecaster.getTSLagMaker().setAddQuarterOfYear(true);

            // build the model
            System.out.println("Training forecaster");
            forecaster.buildForecaster(instances, System.out);
            System.out.println("Training done.");

            return forecaster;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Instances dataToInstances(TreeMap<String, Long> data) {
        try {
            // ensure the data is sorted by key (date) and remove outliers
            TreeMap<String, Long> sortedData = new TreeMap<>();
            for (String date : data.keySet()) {
                Long value = data.get(date);
                if (value < Integer.MAX_VALUE)
                    sortedData.put(date, data.get(date));
            }

            // create the structure of the dataset
            Instances dataset = createDataset();

            // add the instances
            for (String key : sortedData.keySet()) {
                dataset.add(dataToInstance(key, sortedData.get(key), dataset));
            }

            return dataset;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Instance dataToInstance(String time, Long value, Instances dataset)
            throws ParseException {
        Instance instance = new DenseInstance(2);
        instance.setValue(dataset.attribute(0),
                dataset.attribute(0).parseDate(time));
        instance.setValue(dataset.attribute(1), value);

        return instance;
    }

    private Instances createDataset() {
        // define the attributes
        Attribute date = new Attribute(DATE_FIELD, DATE_FORMAT);
        Attribute volume = new Attribute(VOLUME_FIELD);

        // create the dataset
        ArrayList<Attribute> attrs = new ArrayList<>();
        attrs.add(date);
        attrs.add(volume);
        Instances dataset = new Instances("dataset", attrs, 0);

        return dataset;
    }

    private void detectPeriodicity(WekaForecaster forecaster,
            Instances dataset, String dateField) {
        String selectedP = "<Unknown>";

        PeriodicityHandler detected = TSLagMaker.determinePeriodicity(dataset,
                dateField, Periodicity.UNKNOWN);
        switch (detected.getPeriodicity()) {
        case HOURLY:
            selectedP = "Hourly";
            break;
        case DAILY:
            selectedP = "Daily";
            break;
        case WEEKLY:
            selectedP = "Weekly";
            break;
        case MONTHLY:
            selectedP = "Monthly";
            break;
        case QUARTERLY:
            selectedP = "Quarterly";
            break;
        case YEARLY:
            selectedP = "Yearly";
            break;
        }

        if (forecaster != null) {
            if (selectedP.equals("Hourly")) {
                forecaster.getTSLagMaker().setPeriodicity(
                        TSLagMaker.Periodicity.HOURLY);
            } else if (selectedP.equals("Daily")) {
                forecaster.getTSLagMaker().setPeriodicity(
                        TSLagMaker.Periodicity.DAILY);
            } else if (selectedP.equals("Weekly")) {
                forecaster.getTSLagMaker().setPeriodicity(
                        TSLagMaker.Periodicity.WEEKLY);
            } else if (selectedP.equals("Monthly")) {
                forecaster.getTSLagMaker().setPeriodicity(
                        TSLagMaker.Periodicity.MONTHLY);
            } else if (selectedP.equals("Quarterly")) {
                forecaster.getTSLagMaker().setPeriodicity(
                        TSLagMaker.Periodicity.QUARTERLY);
            } else if (selectedP.equals("Yearly")) {
                forecaster.getTSLagMaker().setPeriodicity(
                        TSLagMaker.Periodicity.YEARLY);
            } else {
                forecaster.getTSLagMaker().setPeriodicity(
                        TSLagMaker.Periodicity.UNKNOWN);
            }

        }

        // only set these defaults if the user is not using custom lag lengths!
        forecaster.getTSLagMaker().setMinLag(1);
        if (selectedP.equals("Hourly")) {
            if (forecaster != null) {
                forecaster.getTSLagMaker().setMaxLag(
                        Math.min(dataset.numInstances() / 2, 24));
            }
        } else if (selectedP.equals("Daily")) {
            if (forecaster != null) {
                forecaster.getTSLagMaker().setMaxLag(
                        Math.min(dataset.numInstances() / 2, 7));
            }
        } else if (selectedP.equals("Weekly")) {
            if (forecaster != null) {
                forecaster.getTSLagMaker().setMaxLag(
                        Math.min(dataset.numInstances() / 2, 52));
            }
        } else if (selectedP.equals("Monthly")) {
            if (forecaster != null) {
                forecaster.getTSLagMaker().setMaxLag(
                        Math.min(dataset.numInstances() / 2, 12));
            }
        } else if (selectedP.equals("Quarterly")) {
            if (forecaster != null) {
                forecaster.getTSLagMaker().setMaxLag(
                        Math.min(dataset.numInstances() / 2, 4));
            }
        } else if (selectedP.equals("Yearly")) {
            if (forecaster != null) {
                forecaster.getTSLagMaker().setMaxLag(
                        Math.min(dataset.numInstances() / 2, 5));
            }
        } else {
            // default (<Unknown>)
            if (forecaster != null) {
                forecaster.getTSLagMaker().setMaxLag(
                        Math.min(dataset.numInstances() / 2, 12));
            }
        }

        // configure defaults based on the above periodicity
        if (selectedP.equals("Hourly")) {
            forecaster.getTSLagMaker().setAddAMIndicator(true);
        } else if (selectedP.equals("Daily")) {
            forecaster.getTSLagMaker().setAddDayOfWeek(true);
            forecaster.getTSLagMaker().setAddWeekendIndicator(true);
        } else if (selectedP.equals("Weekly")) {
            forecaster.getTSLagMaker().setAddMonthOfYear(true);
            forecaster.getTSLagMaker().setAddQuarterOfYear(true);
        } else if (selectedP.equals("Monthly")) {
            forecaster.getTSLagMaker().setAddMonthOfYear(true);
            forecaster.getTSLagMaker().setAddQuarterOfYear(true);
        }
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the recentVolumes
     */
    public Instances getRecentVolumes() {
        return recentVolumes;
    }

    /**
     * @param recentVolumes the recentVolumes to set
     */
    public void setRecentVolumes(Instances recentVolumes) {
        this.recentVolumes = recentVolumes;
    }

    /**
     * @return the forecaster
     */
    public WekaForecaster getForecaster() {
        return forecaster;
    }

    /**
     * @param forecaster the forecaster to set
     */
    public void setForecaster(WekaForecaster forecaster) {
        this.forecaster = forecaster;
    }
}
