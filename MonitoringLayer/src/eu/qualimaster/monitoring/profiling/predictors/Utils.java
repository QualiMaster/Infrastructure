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

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.log4j.LogManager;

/**
 * Some predictor utilites.
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
public class Utils {

    /**
     * Parses a double value from text in math3 notation.
     * 
     * @param text the text to parse
     * @return the double value
     * @throws NumberFormatException if parsing fails
     */
    public static double parseDouble(String text) throws NumberFormatException {
        String t = text.trim().replace(",", "");
        int pos = t.lastIndexOf('.');
        if (pos > 0) {
            t = t.substring(0, pos).replace(".", "") + t.substring(pos); // include ".", linux/english formatting
        }
        try {
            return Double.parseDouble(t); // handle , for 1000s
        } catch (NumberFormatException e) {
            throw new NumberFormatException("parsing " + t + ": " + e.getMessage());
        }
    }

    /**
     * Generates a 2-dimensional {@link RealMatrix} from a given String.
     * @param string The needed form is '{{double,double,...},...,{...}}'.
     * @return A {@link RealMatrix} if the conversion was successful, else <null>.
     */
    public static RealMatrix stringToMatrix(String string) {
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
                String[] line = lines[i].split(";");
                matrix[i] = new double[line.length];
                for (int j = 0; j < matrix[i].length; j++) {
                    matrix[i][j] = parseDouble(line[j]);
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
    public static RealVector stringToVector(String string) {
        RealVector result = null;
        try {
            int start = string.indexOf("{") + 1;
            int end = string.indexOf("}");
            string = string.substring(start, end);
            String[] line = string.split(";");
            boolean isEmpty = false;
            if (1 == line.length) {
                isEmpty = line[0].trim().isEmpty();
            }
            double[] vector;
            if (isEmpty) {
                vector = new double[0];
            } else {
                vector = new double[line.length];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = parseDouble(line[i]);
                }
            }
            result = MatrixUtils.createRealVector(vector);
        } catch (NumberFormatException | NullPointerException e) {
            LogManager.getLogger(Kalman.class).error(e.getMessage(), e);
        }
        return result;
    }
    
    /**
     * Turns a vector into a string.
     * 
     * @param vector the vector
     * @return the string representation
     */
    public static String vectorToString(RealVector vector) {
        String result = "{";
        int dim = vector.getDimension();
        for (int i = 0; i < dim; i++) {
            result += toString(vector.getEntry(i));
            if (i + 1 < dim) {
                result += ";";
            }
        }
        return result + "}";
    }
    
    /**
     * Turns a matrix into a string.
     * 
     * @param matrix the matrix
     * @return the string
     */
    public static String matrixToString(RealMatrix matrix) {
        String result = "{";
        int cols = matrix.getColumnDimension();
        int rows = matrix.getRowDimension();
        for (int r = 0; r < rows; r++) {
            result += "{";
            for (int c = 0; c < cols; c++) {
                result += toString(matrix.getEntry(r, c));
                if (c + 1 < cols) {
                    result += ";";
                }
            }
            result += "}";
            if (r + 1 < rows) {
                result += ",";
            }
        }
        return result + "}";
    }
    
    /**
     * Turns a double into a string.
     * 
     * @param value the value to be turned into a string
     * @return the string value
     */
    public static String toString(double value) {
        return String.valueOf(value);
    }

    /**
     * Returns whether two vectors are considered equal with respect to a certain tolerance.
     * 
     * @param v1 the first vector
     * @param v2 the second vector
     * @param diff the tolerance
     * @return <code>true</code> if considered equals, <code>false</code> else
     */
    public static boolean equalsVector(RealVector v1, RealVector v2, double diff) {
        boolean equals = v1.getDimension() == v2.getDimension();
        if (equals) {
            int dim = v1.getDimension();
            for (int i = 0; equals && i < dim; i++) {
                equals &= Math.abs(v1.getEntry(i) - v2.getEntry(i)) < diff;
            }
        }
        return equals;
    }
    
    /**
     * Returns whether two matrices are considered equal with respect to a certain tolerance.
     * 
     * @param m1 the first matrix
     * @param m2 the second matrix
     * @param diff the tolerance
     * @return <code>true</code> if considered equals, <code>false</code> else
     */
    public static boolean equalsMatrix(RealMatrix m1, RealMatrix m2, double diff) {
        boolean equals = m1.getRowDimension() == m2.getRowDimension();
        equals &= m1.getColumnDimension() == m2.getColumnDimension();
        if (equals) {
            int cols = m1.getColumnDimension();
            int rows = m1.getRowDimension();
            for (int r = 0; equals && r < rows; r++) {
                for (int c = 0; equals && c < cols; c++) {
                    equals &= Math.abs(m1.getEntry(r, c) - m2.getEntry(r, c)) < diff;
                }
            }
        }
        return equals;
    }
    
    /**
     * Returns whether two doubles are considered equal with respect to a certain tolerance.
     * 
     * @param d1 the first double
     * @param d2 the second double
     * @param diff the tolerance
     * @return <code>true</code> if considered equals, <code>false</code> else
     */
    public static boolean equalsDouble(double d1, double d2, double diff) {
        return Math.abs(d1 - d2) < diff;
    }

}
