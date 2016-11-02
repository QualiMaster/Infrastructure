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

/**
 * Some constants.
 * 
 * @author Holger Eichelberger
 */
public class Constants {

    /**
     * The value to be returned if there is no prediction (<code>Double.MIN_VALUE</code>).
     */
    public static final double NO_PREDICTION = Double.MIN_VALUE;

    /**
     * The value to be returned if there is no approximation (<code>Double.MIN_VALUE</code>).
     */
    public static final double NO_APPROXIMATION = Double.MIN_VALUE;

    public static final String KEY_CHAR = "*";
    public static final String KEY_INPUT_RATE = KEY_CHAR + "inp" + KEY_CHAR;
    static final String KEY_ALGORITHM = KEY_CHAR + "alg" + KEY_CHAR;
    
    /**
     * Turns a name with {@link #KEY_CHAR} into a file system name if needed.
     * 
     * @param string the string
     * @return the translated string
     */
    public static final String toFileName(String string) {
        return string.replace(KEY_CHAR, "0");
    }
    
}
