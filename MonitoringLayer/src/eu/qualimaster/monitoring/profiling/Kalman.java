package eu.qualimaster.monitoring.profiling;
import java.util.GregorianCalendar;

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
     * The discrete time interval between measurements.
     */
    private double dt = 1d;
    
    /**
     * A - state transition matrix TODO optimal?
     */
    private RealMatrix mA = MatrixUtils.createRealMatrix(new double[][] {
        {1, dt, 0,  0 },
        {0,  1, 0,  0 },
        {0,  0, 1, dt},
        {0,  0, 0,  1 }});

    /**
     * B - control input matrix D TODO optimal?
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
     * Q - process noise covariance matrix TODO optimal?
     */
    private RealMatrix mQ = MatrixUtils.createRealMatrix(4, 4);
    
    private double var = measurementNoise * measurementNoise;
    /**
     * R - measurement noise covariance matrix  TODO optimal?
     */
    private RealMatrix mR = MatrixUtils.createRealMatrix(new double[][] {
        {var,    0,   0,    0 },
        {0,   1e-3,   0,    0 },
        {0,      0, var,    0 },
        {0,      0,   0, 1e-3 }});

    /**
     * P - error covariance matrix  TODO optimal?
     */
    private RealMatrix mP = MatrixUtils.createRealMatrix(new double[][] {
            {var,    0,   0,    0 },
            {0,   1e-3,   0,    0 },
            {0,      0, var,    0 },
            {0,      0,   0, 1e-3 }});

    /**
     * Vector used to store the start value for a new timeline.
     */
    private RealVector x = MatrixUtils.createRealVector(new double[] {0, 1, 0, 0 });

    /**
     * The {@link ProcessModel} for the Kalman-Filter. TODO Is default good
     * enough?
     */
    private ProcessModel pm = new DefaultProcessModel(mA, mB, mQ, x, mP);

    /**
     * The {@link MeasurementModel} for the Kalman-Filter. TODO Is default good
     * enough?
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
    private final long startTime = new GregorianCalendar().getTimeInMillis();    
    
    /**
     * The point in time this Kalman-Instance was last updated as UTC milliseconds.
     */
    private long lastUpdated = 0;    
    /**
     * The latest value used to update the Kalman-Instance.
     */
    private double lastUpdate = Double.MIN_VALUE;
    /**
     * The latest prediction made for 1 second after 'lastUpdated'.
     */
    private double lastPrediction = Double.MIN_VALUE;
    
    /**
     * Default contructor used for a new timeline. TODO Custom constructors to
     * continue partly predicted timelines follows at a later date.
     */
    public Kalman() {
    }
    
    /**
     * This method updates the Kalman-Filter with the last known state/measurement of the observed value.
     * 
     * @param xMeasured Time step of measurement. For now the filter has to be 
     * used iterative, so this value has to be exactly one greater than the 
     * last time this method was called. // TODO Implement Gap-Handling
     * @param yMeasured Current measurement.
      * @return True if the update was successful.
     */
    public boolean update(double xMeasured, double yMeasured) {
        boolean success = false;
        try {
            filter.correct(new double[] {xMeasured, 0, yMeasured, 0 });
            this.lastUpdated = System.currentTimeMillis();
            this.lastUpdate = yMeasured;
            success = true;
        } catch (NullArgumentException | DimensionMismatchException | SingularMatrixException e) {
            e.printStackTrace();
        }
        return success;
    }
    /**
     * This method predicts the value of a time line one time step ahead of the
     * last (via update) given value.
     * 
     * @return Prediction for one time step ahead as {@link Double} or Double.MIN_VALUE if the prediction failed.
     */
    public double predict() {
        try {
            filter.predict(controlVector);
            lastPrediction = filter.getStateEstimation()[2];
        } catch (DimensionMismatchException e) {
            e.printStackTrace();
            lastPrediction = Double.MIN_VALUE;
        }
        return lastPrediction;
    }
}
