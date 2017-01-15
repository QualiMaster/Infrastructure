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

/**
 * Some predictor utilites.
 * 
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
        String t = text.replace(",", "");
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

}
