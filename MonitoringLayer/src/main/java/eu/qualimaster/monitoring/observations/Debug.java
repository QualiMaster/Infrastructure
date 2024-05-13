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
package eu.qualimaster.monitoring.observations;

/**
 * Helper class for debugging topology-based aggregations through proper indentations.
 * 
 * @author Holger Eichelberger
 */
public class Debug {

    private static boolean debug = false;
    private static String indent = "";
    
    /**
     * Enables or disables debugging.
     * 
     * @param doDebug the debug flag indicating debugging or non-debugging mode
     */
    public static void setDebugFlag(boolean doDebug) {
        debug = doDebug;
    }
    
    /**
     * Increasing the indentation for the following {@link #println(String) debug prints}.
     */
    public static void increaseIndent() {
        if (debug) {
            indent = indent + " ";
        }
    }
    
    /**
     * Decreasing the indentation for the following {@link #println(String) debug prints}.
     */
    public static void decreaseIndent() {
        if (debug && indent.length() > 0) {
            indent = indent.substring(0, indent.length() - 1);
        }
    }
    
    /**
     * Prints debugging text.
     * 
     * @param text the text to be printed
     */
    public static void println(String text) {
        if (debug) {
            System.out.println(indent + text);
        }
    }
}
