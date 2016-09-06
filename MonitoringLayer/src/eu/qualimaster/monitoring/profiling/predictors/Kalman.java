/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.monitoring.profiling.predictors;
import java.util.Properties;

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
import org.apache.log4j.LogManager;

import eu.qualimaster.monitoring.profiling.Utils;

/**
 * Kalman Implementation for the QualiMaster-Project using the
 * Apache-math-library to reengineer the approach used in the RainMon-Project,
 * which is written in Python.
 * The Kalman-Filter can only handle discrete-time controlled processes.
 * Therefore the correction-prediction cycle has a discrete time interval.
 * If a time step passes without an update, an update will be simulated.
 * 
 * @author Christopher Voges
 */
public class Kalman extends AbstractMatrixPredictor {

    private static final String KEY_MEASUREMENT_NOISE = "measurementNoise";
    private static final String KEY_MATRIX_A = "A";
    private static final String KEY_MATRIX_B = "B";
    private static final String KEY_MATRIX_H = "H";
    private static final String KEY_MATRIX_Q = "Q";
    private static final String KEY_MATRIX_P = "P";
    private static final String KEY_MATRIX_R = "R";
    private static final String KEY_VECTOR_X = "x";
    private static final String KEY_VECTOR_CONTROL = "controlVector";
    private static final String KEY_LAST_UPDATED = "lastUpdated";
    private static final String KEY_LAST_UPDATE = "lastUpdate";
    private static final String KEY_ALLOWED_GAP = "allowedGap";
    private static final String KEY_DEFAULT_MEASUREMENT = "defaultMeasurement";

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
    private RealVector xVector = MatrixUtils.createRealVector(new double[] {0, 1, 0, 0 });

    /**
     * The {@link ProcessModel} for the Kalman-Filter. 
     */
    private ProcessModel pm = new DefaultProcessModel(mA, mB, mQ, xVector, mP);

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
    private int allowedGap = 500;
    
    /**
     * Default measurement value.
     * If an update must be simulated and there is no predicted value to use instead of the measurement,
     * this value is used for the update.
     */
    private double defaultMeasurement = 0;

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
            LogManager.getLogger(Kalman.class).error(e.getMessage(), e);
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
                        update(lastUpdated + 1 , prediction == Double.MIN_VALUE ? lastUpdate : defaultMeasurement);
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
                LogManager.getLogger(Kalman.class).error(e.getMessage(), e);
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

    /**
     * Generates a 2-dimensional {@link RealMatrix} from a given String.
     * @param string The needed form is '{{double,double,...},...,{...}}'.
     * @return A {@link RealMatrix} if the conversion was successful, else <null>.
     */
    public static RealMatrix stringTo2DMatrix(String string) {
        RealMatrix result = null;
        try {
            // 2D-> '{{' marks the start and '}}' the end.
            int start = string.indexOf("{{") + 2;
            int end = string.indexOf("}}");
            string = string.substring(start, end);
            // Create lines
            String[] lines = string.split("\\},\\{");
            double[][] matrix = new double[lines.length][];
            // Fill lines
            for (int i = 0; i < matrix.length; i++) {
                String[] line = lines[i].split(",");
                matrix[i] = new double[line.length];
                for (int j = 0; j < matrix[i].length; j++) {
                    matrix[i][j] = Double.parseDouble(line[j]);
                }
            }
            result = MatrixUtils.createRealMatrix(matrix);
        } catch (NullArgumentException | DimensionMismatchException | NumberFormatException e) {
            LogManager.getLogger(Kalman.class).error(e.getMessage(), e);
        }
        return result;
    }
    
    /**
     * Generates a 2-dimensional {@link RealVector} from a given String.
     * @param string The needed form is '{double;double;...}'.
     * @return A {@link RealVector} if the conversion was successful, else <null>.
     */
    private static RealVector stringTo2DVector(String string) {
        RealVector result = null;
        
        try {
            int start = string.indexOf("{") + 1;
            int end = string.indexOf("}");
            string = string.substring(start, end);
            String[] line = string.split(";");
            double[] vector = new double[line.length];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = Double.parseDouble(line[i]);
            }
            
            result = MatrixUtils.createRealVector(vector);
        } catch (NumberFormatException | NullPointerException e) {
            LogManager.getLogger(Kalman.class).error(e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * Update models and the Kalman-Filter after a change in the parameters / matrices.
     */
    private void reinitialize() {
        pm = new DefaultProcessModel(mA, mB, mQ, xVector, mP);
        mm = new DefaultMeasurementModel(mH, mR);
        filter = new KalmanFilter(pm, mm);
    }
    
    @Override
    protected Properties toProperties() {
        Properties result = new Properties();
        result.put(KEY_MEASUREMENT_NOISE, String.valueOf(measurementNoise));
        result.put(KEY_MATRIX_A, mA.toString());
        result.put(KEY_MATRIX_B, mB.toString());
        result.put(KEY_MATRIX_H, mH.toString());
        result.put(KEY_MATRIX_Q, mQ.toString());
        result.put(KEY_MATRIX_R, mR.toString());
        result.put(KEY_MATRIX_P, mP.toString());
        result.put(KEY_VECTOR_X, xVector.toString());
        result.put(KEY_VECTOR_CONTROL, controlVector.toString());
        result.put(KEY_LAST_UPDATED, String.valueOf(lastUpdated));
        result.put(KEY_LAST_UPDATE, String.valueOf(lastUpdate));
        result.put(KEY_ALLOWED_GAP, String.valueOf(allowedGap));
        result.put(KEY_DEFAULT_MEASUREMENT, String.valueOf(defaultMeasurement));
        return result;
    }

    /**
     * Returns a matrix from a properties file.
     * 
     * @param prop the properties file
     * @param key the key
     * @param deflt the default value
     * @return the read matdix or <code>deflt</code>
     */
    private static RealMatrix getMatrix(Properties prop, String key, RealMatrix deflt) {
        RealMatrix tempM = null;
        String tmp = prop.getProperty(key);
        if (null != tmp) {
            tempM = stringTo2DMatrix(tmp);
        }
        return null != tempM ? tempM : deflt;
    }

    /**
     * Returns a vector from a properties file.
     * 
     * @param prop the properties file
     * @param key the key
     * @param deflt the default value
     * @return the read vector or <code>deflt</code>
     */
    private static RealVector getVector(Properties prop, String key, RealVector deflt) {
        RealVector tempV = null;
        String tmp = prop.getProperty(key);
        if (null != tmp) {
            tempV = stringTo2DVector(tmp);
        }
        return null != tempV ? tempV : deflt;
    }
    
    
    @Override
    protected void setProperties(Properties data) {
        measurementNoise = Utils.getDouble(data, KEY_MEASUREMENT_NOISE, measurementNoise);
        mA = getMatrix(data, KEY_MATRIX_A, mA);
        mB = getMatrix(data, KEY_MATRIX_B, mB);
        mH = getMatrix(data, KEY_MATRIX_H, mH);
        mQ = getMatrix(data, KEY_MATRIX_Q, mQ);
        mP = getMatrix(data, KEY_MATRIX_P, mP);
        mR = getMatrix(data, KEY_MATRIX_R, mR);
        xVector = getVector(data, KEY_VECTOR_X, xVector);
        controlVector = getVector(data, KEY_VECTOR_CONTROL, controlVector);
        lastUpdated = Utils.getLong(data, KEY_LAST_UPDATED, lastUpdated);
        lastUpdate = Utils.getDouble(data, KEY_LAST_UPDATE, lastUpdate);
        allowedGap = Utils.getInt(data, KEY_ALLOWED_GAP, allowedGap);
        defaultMeasurement = Utils.getDouble(data, KEY_DEFAULT_MEASUREMENT, defaultMeasurement);
        reinitialize();
    }
    
    @Override
    public boolean equals(Object obj) {
        // for now we focus only on the values that are actually stored - to be checked
        // double equality follows the implementation of Double.equals
        boolean result = false;
        if (obj instanceof Kalman) {
            Kalman k = (Kalman) obj;
            result = equals(measurementNoise, k.measurementNoise);
            result &= mA.equals(k.mA);
            result &= mB.equals(k.mB);
            result &= mH.equals(k.mH);
            result &= mQ.equals(k.mQ);
            result &= mP.equals(k.mP);
            result &= mR.equals(k.mR);
            result &= xVector.equals(k.xVector);
            result &= controlVector.equals(k.controlVector);
            result &= equals(lastUpdated, k.lastUpdated);
            result &= equals(lastUpdate, k.lastUpdate);
            result &= allowedGap == k.allowedGap;
            result &= equals(defaultMeasurement, k.defaultMeasurement);
        }
        return result;
    }
    
    /**
     * Compares two doubles using the same approach as <code>Double.equals</code>. 
     * 
     * @param d1 the first double value
     * @param d2 the second double value
     * @return <code>true</code> if <code>d1==d2</code>, <code>false</code> else
     */ 
    private static boolean equals(double d1, double d2) {
        return Double.doubleToLongBits(d1) == Double.doubleToLongBits(d2);
    }
    
    /**
     * The hashcode method according to Double (Java 1.8).
     * 
     * @param value the double value
     * @return the hash code
     */
    private static int hashCode(double value) {
        long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));        
    }
    
    @Override
    public int hashCode() {
        int result = hashCode(measurementNoise);
        result ^= mA.hashCode();
        result ^= mB.hashCode();
        result ^= mH.hashCode();
        result ^= mQ.hashCode();
        result ^= mP.hashCode();
        result ^= mR.hashCode();
        result ^= xVector.hashCode();
        result ^= controlVector.hashCode();
        result ^= hashCode(lastUpdated);
        result ^= hashCode(lastUpdate);
        result ^= allowedGap;
        result ^= hashCode(defaultMeasurement);
        return result;
    }
    
}
