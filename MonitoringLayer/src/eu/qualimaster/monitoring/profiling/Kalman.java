package eu.qualimaster.monitoring.profiling;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Kalman Implementation for the QualiMaster-Project using the
 * apache-math-library to reengineer the approach used in the RainMon-Project,
 * which is written in Python.
 * 
 * @author Christopher Voges
 */
public class Kalman {

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
     * Default contructor used for a new timeline. TODO Custom constructors to
     * continue partly predicted timelines follows at a later date.
     */
    public Kalman() {
    }
    
    /**
     * This method predicts the value of a timeline one timestep ahead of the
     * given value.
     * 
     * @param xMeasured Timestep of measurement. For now the filter has to be 
     * used iterative, so this value has to be exactly one greater than the 
     * last time this method was called.
     * @param yMeasured Current measurement.
     * @return Prediction for one timestep ahead as {@link Double}.
     */
    public double predict(double xMeasured, double yMeasured) {
        filter.correct(new double[] {xMeasured, 0, yMeasured, 0 });
        filter.predict(controlVector);
        return filter.getStateEstimation()[2];
    }
}
