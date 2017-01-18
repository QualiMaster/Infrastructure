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
import org.apache.commons.math3.linear.MatrixDimensionMismatchException;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.log4j.LogManager;

import eu.qualimaster.monitoring.profiling.Constants;
import eu.qualimaster.monitoring.profiling.Utils;

import static eu.qualimaster.monitoring.profiling.predictors.Utils.*;

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
     * The discrete time interval between measurements. (1 second, fixed)
     * This is also the range for a single prediction step, i.e. one second into the future.
     */
    private static final double DT = 1d;
    
    /**
     * State transition matrix (A).
     */
    private static final RealMatrix DEFAULT_A = MatrixUtils.createRealMatrix(new double[][] {
        {1, DT, 0,  0 },
        {0,  1, 0,  0 },
        {0,  0, 1, DT },
        {0,  0, 0,  1 }});

    /**
     * Control input matrix (B).
     */
    private static final RealMatrix DEFAULT_B = MatrixUtils.createRealMatrix(new double[][] {
        {0, 0, 0, 0 },
        {0, 0, 0, 0 },
        {0, 0, 1, 0 },
        {0, 0, 0, 1 }});
    
    /**
     * Measurement matrix (H). 
     */
    private static final RealMatrix DEFAULT_H = MatrixUtils.createRealMatrix(new double[][] {
        {1, 0, 0, 0 },
        {0, 0, 0, 0 },
        {0, 0, 1, 0 },
        {0, 0, 0, 0 }});
    
    /**
     * Noise is eliminated  beforehand, but it can (for
     * algorithm reasons) not be 0. Instead a double value of 0.0001d is set.
     */
    private static final double DEFAULT_MEASUREMENT_NOISE = 0.0001d;

    /**
     * Process noise covariance matrix (Q).
     */
    private static final RealMatrix DEFAULT_Q = MatrixUtils.createRealMatrix(4, 4);
    
    private static final double VAR = DEFAULT_MEASUREMENT_NOISE * DEFAULT_MEASUREMENT_NOISE;
    
    /**
     * Measurement noise covariance matrix (R).
     */
    private static final RealMatrix DEFAULT_R = MatrixUtils.createRealMatrix(new double[][] {
        {VAR,    0,   0,    0 },
        {0,   1e-3,   0,    0 },
        {0,      0, VAR,    0 },
        {0,      0,   0, 1e-3 }});

    /**
     * Error covariance matrix (P).
     */
    private static final RealMatrix DEFAULT_P = MatrixUtils.createRealMatrix(new double[][] {
            {VAR,    0,   0,    0 },
            {0,   1e-3,   0,    0 },
            {0,      0, VAR,    0 },
            {0,      0,   0, 1e-3 }});

    /**
     * Vector used to store the start value for a new timeline.
     * (initial x, its velocity, initial y, its velocity)
     */
    private static final RealVector DEFAULT_X_VECTOR = MatrixUtils.createRealVector(new double[] {0, 1, 0, 0 });

    /**
     * This vector is used to add e.g. acceleration. For us/now it is a 
     * zero-vector.
     */
    private RealVector controlVector = MatrixUtils.createRealVector(new double[] {0, 0, 0, 0});
    
    /**
     * The instance of Apaches {@link KalmanFilter}.
     */
    private KalmanFilter filter;
    
    /**
     * The point in time this Kalman-Instance was last updated as seconds since midnight, January 1, 1970 UTC.
     */
    private long lastUpdated = Long.MIN_VALUE;
    
    /**
     * The latest value used to update the Kalman-Instance.
     */
    private double lastUpdate = Constants.NO_PREDICTION;
    
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
    
    private boolean predictedSinceUpdate = false;

    /**
     * Default constructor used for a new timeline. 
     */
    public Kalman() {
        ProcessModel pm = new DefaultProcessModel(DEFAULT_A, DEFAULT_B, DEFAULT_Q, DEFAULT_X_VECTOR, DEFAULT_P);
        MeasurementModel mm = new DefaultMeasurementModel(DEFAULT_H, DEFAULT_R);
        filter = new KalmanFilter(pm, mm);
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
            // Call predict(0), if no prediction was made since the last update
            // Reason: The Kalman-Filter needs a predict-update(correct)-cycle.
            if (!predictedSinceUpdate && lastUpdate != Constants.NO_PREDICTION) {
                predict(0);
            }
            
            filter.correct(new double[] {xMeasured, 0, yMeasured, 0 });
            // When an older value is updated/corrected the attributes 'lastUpdated' and 'lastUpdate' do not change.
            if (lastUpdated < xMeasured) {
                lastUpdated = xMeasured;
                lastUpdate = yMeasured;
            }
            success = true;
            predictedSinceUpdate = false;
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
     * @return Predictions for the last time step ahead as {@link Double} or {@link Constants#NO_PREDICTION} if the 
     *     prediction failed.
     */
    public double predict(int steps) {
        double prediction = Constants.NO_PREDICTION;
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
                         * to use instead of the measurement, 'defaultMeasurement' value is used for the update.
                         */
                        update(lastUpdated + 1 , prediction == Constants.NO_PREDICTION 
                            ? lastUpdate : defaultMeasurement);
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
                predictedSinceUpdate = true;
            } catch (DimensionMismatchException e) {
                LogManager.getLogger(Kalman.class).error(e.getMessage(), e);
                prediction = Constants.NO_PREDICTION;
            }
        } /*else {
            LogManager.getLogger(Kalman.class).warn("Prediction should only be called after at least one update-call!");
        }*/
        return prediction;
    }
    
    /**
     * This method predicts the value of a time line one time step ahead of the
     * last (via update) given value.
     * 
     * @return Prediction for one time step ahead as {@link Double} or {@link Constants#NO_PREDICTION} if the 
     *     prediction failed.
     */
    public double predict() {
        return predict(1);
    }

    
    @Override
    protected Properties toProperties() {
        Properties result = new Properties();

        // currently constant - write/read for future extension
        result.put(KEY_MEASUREMENT_NOISE, String.valueOf(DEFAULT_MEASUREMENT_NOISE));
        result.put(KEY_MATRIX_A, matrixToString(DEFAULT_A));
        result.put(KEY_MATRIX_B, matrixToString(DEFAULT_B));
        result.put(KEY_MATRIX_H, matrixToString(DEFAULT_H));
        result.put(KEY_MATRIX_Q, matrixToString(DEFAULT_Q));
        result.put(KEY_MATRIX_R, matrixToString(DEFAULT_R));

        // variable
        result.put(KEY_MATRIX_P, matrixToString(getErrorCovarianceMatrix()));
        result.put(KEY_VECTOR_X, vectorToString(getStateEstimationVector()));
        result.put(KEY_VECTOR_CONTROL, vectorToString(controlVector));
        result.put(KEY_LAST_UPDATED, String.valueOf(lastUpdated));
        result.put(KEY_LAST_UPDATE, String.valueOf(lastUpdate));
        result.put(KEY_ALLOWED_GAP, String.valueOf(allowedGap));
        result.put(KEY_DEFAULT_MEASUREMENT, String.valueOf(defaultMeasurement));
        return result;
    }

    /**
     * Returns the actual error covariance matrix.
     * 
     * @return the error covariance matrix
     */
    private RealMatrix getErrorCovarianceMatrix() {
        return filter.getErrorCovarianceMatrix();
    }

    /**
     * Returns the actual state estimation vector.
     * 
     * @return the state estimation vector
     */
    private RealVector getStateEstimationVector() {
        return filter.getStateEstimationVector();
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
            tempM = stringToMatrix(tmp);
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
            tempV = stringToVector(tmp);
        }
        return null != tempV ? tempV : deflt;
    }
    
    
    @Override
    protected void setProperties(Properties data) throws IllegalArgumentException {
        // currently constant - write/read for future extension
        Utils.getDouble(data, KEY_MEASUREMENT_NOISE, DEFAULT_MEASUREMENT_NOISE); // ignore value
        RealMatrix mA = getMatrix(data, KEY_MATRIX_A, DEFAULT_A);
        RealMatrix mB = getMatrix(data, KEY_MATRIX_B, DEFAULT_B);
        RealMatrix mH = getMatrix(data, KEY_MATRIX_H, DEFAULT_H);
        RealMatrix mQ = getMatrix(data, KEY_MATRIX_Q, DEFAULT_Q);
        RealMatrix mR = getMatrix(data, KEY_MATRIX_R, DEFAULT_R);

        // variable
        RealMatrix mP = getMatrix(data, KEY_MATRIX_P, DEFAULT_P);
        RealVector xVector = getVector(data, KEY_VECTOR_X, DEFAULT_X_VECTOR);
        controlVector = getVector(data, KEY_VECTOR_CONTROL, controlVector);
        lastUpdated = Utils.getLong(data, KEY_LAST_UPDATED, lastUpdated);
        lastUpdate = Utils.getDouble(data, KEY_LAST_UPDATE, lastUpdate);
        allowedGap = Utils.getInt(data, KEY_ALLOWED_GAP, allowedGap);
        defaultMeasurement = Utils.getDouble(data, KEY_DEFAULT_MEASUREMENT, defaultMeasurement);

        try {
            ProcessModel pm = new DefaultProcessModel(mA, mB, mQ, xVector, mP); // xVector, mP
            MeasurementModel mm = new DefaultMeasurementModel(mH, mR);
            filter = new KalmanFilter(pm, mm);
        } catch (NullArgumentException | DimensionMismatchException | MatrixDimensionMismatchException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public boolean equals(IAlgorithmProfilePredictor other, double diff) {
        boolean result = false;
        if (other instanceof Kalman) {
            Kalman o = (Kalman) other;
            result = equalsMatrix(getErrorCovarianceMatrix(), o.getErrorCovarianceMatrix(), diff);
            result &= equalsVector(getStateEstimationVector(), o.getStateEstimationVector(), diff);
            result &= equalsVector(controlVector, o.controlVector, diff);
            result &= lastUpdated == o.lastUpdated;
            result &= equalsDouble(lastUpdate, o.lastUpdate, diff);
            result &= allowedGap == o.allowedGap;
            result &= equalsDouble(defaultMeasurement, o.defaultMeasurement, diff);
        }
        return result;
    }

    @Override
    public long getLastUpdated() {
        return lastUpdated;
    }
    
}
