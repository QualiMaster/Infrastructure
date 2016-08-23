package eu.qualimaster.monitoring.profiling;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;

/**
 * Kalman Implementation for the QualiMaster-Project using the
 * apache-math-library to reengineer the approach used in the RainMon-Project,
 * which is written in Python.
 * The Kalman-Filter can only handle descrete-time controlled processes.
 * Therefore the correction-prediction cycle has a discrete time interval.
 * If a time step passes without an update, an update will be simulated.
 * 
 * @author Christopher Voges
 */
public class Kalman implements AlgorithmProfilePredictorAlgorithm {

    /**
     * Noise is eliminated  beforehand, but it can (for
     * algorithm reasons) not be 0. Instead a double value of 0.0001d is set.
     */
    private double measurementNoise = 0.0001d;
    
    /** 
     * The discrete time interval between measurements. (1 second, fixed)
     * This is also the range for a single prediction step, i.e. one second into the future.
     */
    private final double dt = 1d;
    
    /**
     * A - state transition matrix.
     */
    private RealMatrix mA = MatrixUtils.createRealMatrix(new double[][] {
        {1, dt, 0,  0 },
        {0,  1, 0,  0 },
        {0,  0, 1, dt },
        {0,  0, 0,  1 }});

    /**
     * B - control input matrix D.
     */
    private RealMatrix mB = MatrixUtils.createRealMatrix(new double[][] {
        {0, 0, 0, 0 },
        {0, 0, 0, 0 },
        {0, 0, 1, 0 },
        {0, 0, 0, 1 }});
    
    /**
     * H - measurement matrix. 
     */
    private RealMatrix mH = MatrixUtils.createRealMatrix(new double[][] {
        {1, 0, 0, 0 },
        {0, 0, 0, 0 },
        {0, 0, 1, 0 },
        {0, 0, 0, 0 }});

    /**
     * Q - process noise covariance matrix.
     */
    private RealMatrix mQ = MatrixUtils.createRealMatrix(4, 4);
    
    private double var = measurementNoise * measurementNoise;
    
    /**
     * R - measurement noise covariance matrix.
     */
    private RealMatrix mR = MatrixUtils.createRealMatrix(new double[][] {
        {var,    0,   0,    0 },
        {0,   1e-3,   0,    0 },
        {0,      0, var,    0 },
        {0,      0,   0, 1e-3 }});

    /**
     * P - error covariance matrix.
     */
    private RealMatrix mP = MatrixUtils.createRealMatrix(new double[][] {
            {var,    0,   0,    0 },
            {0,   1e-3,   0,    0 },
            {0,      0, var,    0 },
            {0,      0,   0, 1e-3 }});

    /**
     * Vector used to store the start value for a new timeline.
     * (initial x, its velocity, initial y, its velocity)
     */
    private RealVector x = MatrixUtils.createRealVector(new double[] {0, 1, 0, 0 });

    /**
     * The {@link ProcessModel} for the Kalman-Filter. 
     */
    private ProcessModel pm = new DefaultProcessModel(mA, mB, mQ, x, mP);

    /**
     * The {@link MeasurementModel} for the Kalman-Filter. 
     */
    private MeasurementModel mm = new DefaultMeasurementModel(mH, mR);

    /**
     * The instance of Apaches {@link KalmanFilter}.
     */
    private KalmanFilter filter = new KalmanFilter(pm, mm);

    /**
     * This vector is used to add e.g. acceleration. For us/now it is a 
     * zero-vector.
     */
    private RealVector controlVector = MatrixUtils.createRealVector(new double[] {0, 0, 0, 0});
    
    /**
     * The point in time this Kalman-Instance was first created as UTC milliseconds.
     */
    private final long startTime = System.currentTimeMillis();    
    
    /**
     * The point in time this Kalman-Instance was last updated as seconds since midnight, January 1, 1970 UTC.
     */
    private long lastUpdated = Long.MIN_VALUE;    
    /**
     * The latest value used to update the Kalman-Instance.
     */
    private double lastUpdate = Double.MIN_VALUE;
    /**
     * Allowed gap between update and prediction in milliseconds.
     */
    private final int allowedGap = 500;
    /**
     * Default measurement value.
     * If an update must be simulated and there is no predicted value to use instead of the measurement,
     * this value is used for the update.
     */
    private double defaultMeasurenment = 0;
    /**
     * Default contructor used for a new timeline. 
     * TODO Custom constructors to continue partly predicted timelines 
     */
    public Kalman() {
    }
    
    /**
     * This method updates the Kalman-Filter with the current state/measurement of the observed value.
     * Where current means that the measurement will be mapped to the second since midnight, January 1, 1970 UTC. 
     * 
     * @param measured Currently measured state of the observed value. 
     * @return True if the update was successful.
     */
    public boolean update(double measured) {
        return update(System.currentTimeMillis() / 1000, measured);
    }
    
    /**
     * This method updates the Kalman-Filter with the last known state/measurement of the observed value.
     * 
     * @param xMeasured time step of measurement.  
     * It is measured in full, i.e. as {@link Long}, seconds since midnight, January 1, 1970 UTC.
     * @param yMeasured Current measurement.
     * @return True if the update was successful.
     */
    public boolean update(long xMeasured, double yMeasured) {
        boolean success = false;
        try {
            // TODO if first update, reinitialize filter with x = (xMeasured, 1, yMeasured, 0)
            
            filter.correct(new double[] {xMeasured, 0, yMeasured, 0 });
            // When an older value is updated/corrected the attributes 'lastUpdated' and 'lastUpdate' do not change.
            if (lastUpdated < xMeasured) {
                lastUpdated = xMeasured;
                lastUpdate = yMeasured;
            }
            success = true;
        } catch (NullArgumentException | DimensionMismatchException | SingularMatrixException e) {
            e.printStackTrace();
        }
        return success;
    }
    /**
     * This method predicts the value of a time line one or multiple time step(s) ahead of the
     * last (via update) given value.
     * @param steps Number of times steps to predict.
     * 
     * @return Predictions for the last time step ahead as {@link Double} or Double.MIN_VALUE if the prediction failed.
     */
    public double predict(int steps) {
        double prediction = Double.MIN_VALUE;
        if (lastUpdated != Long.MIN_VALUE) {
            try {
                if (steps > 0) {
                    // Gap-Handling
                    /* 
                     * As long as the time stamp of the last update and the time step to predict 
                     * are more than an allowed gap apart from each other ...
                     */ 
                    long oldLastUpdated = lastUpdated;
                    double oldLastUpdate = lastUpdate;
                    boolean gap = false;
                    while (((System.currentTimeMillis() + (steps - 1) * 1000) - (lastUpdated * 1000)) > allowedGap) {
                        /* 
                         * ... simulate updates using the last prediction.
                         * If an update must be simulated and there is no predicted value 
                         * to use instead of the measurement, 'defaultMeasurenment' value is used for the update.
                         */
                        update(lastUpdated + 1 , prediction == Double.MIN_VALUE ? lastUpdate : defaultMeasurenment);
                        prediction = predict(0);
                        gap = true;
                    }
                    // Reset values overwritten by gap handling to make predict-updates non-persistent.
                    if (gap) {
                        lastUpdated = oldLastUpdated;
                        lastUpdate = oldLastUpdate;
                    }
                }
                filter.predict(controlVector);
                prediction = filter.getStateEstimation()[2];
            } catch (DimensionMismatchException e) {
                e.printStackTrace();
                prediction = Double.MIN_VALUE;
            }
        } else {
            System.err.println("Warning: Prediction should only be called after at least one update-call!");
        }
        return prediction;
    }
    
    /**
     * This method predicts the value of a time line one time step ahead of the
     * last (via update) given value.
     * 
     * @return Prediction for one time step ahead as {@link Double} or Double.MIN_VALUE if the prediction failed.
     */
    public double predict() {
        return predict(1);
    }
}
