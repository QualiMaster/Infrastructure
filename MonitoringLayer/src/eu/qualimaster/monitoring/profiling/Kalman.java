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
package eu.qualimaster.monitoring.profiling;
import java.util.ArrayList;

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
public class Kalman implements IAlgorithmProfilePredictorAlgorithm {

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
    private double defaultMeasurenment = 0;
    /**
     * Default contructor used for a new timeline. 
     * TODO Custom constructors to continue partly predicted timelines 
     */
    public Kalman() {
    }
    /**
     * .
     * @param parameters .
     */
    public Kalman(ArrayList<String> parameters) {
        stringsToAttributes(parameters);
        reinitialize();
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

    /** 
     * Generates a String representation of a {@link Kalman} instance.
     * @return {@link ArrayList} of {@link String} representing a {@link Kalman} instance.
     */
    public ArrayList<String> toStringArrayList() {
        ArrayList<String> result = new ArrayList<>();
        result.add("measurementNoise=" + measurementNoise);
        result.add("A=" + mA);
        result.add("B=" + mB);
        result.add("H=" + mH);
        result.add("Q=" + mQ);
        result.add("R=" + mR);
        result.add("P=" + mP);
        result.add("x=" + xVector);
        result.add("controlVector=" + controlVector);
        result.add("lastUpdated=" + lastUpdated);
        result.add("lastUpdate=" + lastUpdate);
        result.add("allowedGap=" + allowedGap);
        result.add("defaultMeasurenment=" + defaultMeasurenment);
        return result;
    }
    /**
     * Generates a 2-dimensional {@link RealMatrix} from a given String.
     * @param string The needed form is '{{double,double,...},...,{...}}'.
     * @return A {@link RealMatrix} if the conversion was successful, else <null>.
     */
    public RealMatrix stringTo2DMatrix(String string) {
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
            e.printStackTrace();
        }
        return result;
    }
    /**
     * Generates a 2-dimensional {@link RealVector} from a given String.
     * @param string The needed form is '{double;double;...}'.
     * @return A {@link RealVector} if the conversion was successful, else <null>.
     */
    private RealVector stringTo2DVector(String string) {
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
            e.printStackTrace();
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
    /**
     * Update the attributes using given string representations.
     * @param parameters Content to override the attributes with.
     */
    private void stringsToAttributes(ArrayList<String> parameters) {
        for (String string : parameters) {
            String entry = string.split("=")[0];
            String content = string.split("=")[1];
            RealMatrix tempM = null;
            RealVector tempV = null;
            try {
                switch (entry) {
                case "measurementNoise":
                    measurementNoise = Double.parseDouble(content);
                    break;
                case "A":
                    tempM = stringTo2DMatrix(content);
                    mA = null != tempM ? tempM : mA;
                    break;
                case "B":
                    tempM = stringTo2DMatrix(content);
                    mB = null != tempM ? tempM : mB;
                    break;
                case "H":
                    tempM = stringTo2DMatrix(content);
                    mH = null != tempM ? tempM : mH;
                    break;
                case "Q":
                    tempM = stringTo2DMatrix(content);
                    mQ = null != tempM ? tempM : mQ;
                    break;
                case "P":
                    tempM = stringTo2DMatrix(content);
                    mP = null != tempM ? tempM : mP;
                    break;
                case "R":
                    tempM = stringTo2DMatrix(content);
                    mR = null != tempM ? tempM : mR;
                    break;
                case "x":
                    tempV = stringTo2DVector(content);
                    xVector = null != tempV ? tempV : xVector;
                    break;
                case "controlVector":
                    tempV = stringTo2DVector(content);
                    controlVector = null != tempV ? tempV : controlVector;
                    break;
                case "lastUpdated":
                    lastUpdated = Long.parseLong(content);
                    break;
                case "lastUpdate":
                    lastUpdate = Double.parseDouble(content);
                    break;
                case "allowedGap":
                    allowedGap = Integer.parseInt(content);
                    break;
                case "defaultMeasurenment":
                    defaultMeasurenment = Double.parseDouble(content);
                    break;
                default:
                    break;
                }
            } catch (NullPointerException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public String getIdentifier() {
        return "PREDICTOR=kalman";
    }

}
