/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.logReader;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.events.AbstractEvent;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Observables;

/**
 * Implements line parsing functions for events.
 * 
 * @author Holger Eichelberger
 */
public class EventLineParser {

    private String line;
    private PrintStream err;
    
    /**
     * Creates an event line parser.
     * 
     * @param line the line to be parsed
     * @param err the error stream (may be <b>null</b>)
     */
    public EventLineParser(String line, PrintStream err) {
        this.line = line;
        this.err = err;
    }
    
    /**
     * Parses observations.
     * 
     * @param attribute the attribute to parse for
     * @return the map of observations or <b>null</b> in case of errors
     */
    public Map<IObservable, Double> parseObservations(String attribute) {
        Map<IObservable, Double> result = null;
        if (isAttribute(attribute)) {
            consume(attribute.length() + 1);
            line = line.trim();
            if (line.startsWith("{")) {
                consume(0);
                result = new HashMap<IObservable, Double>();
                int startLength;
                do {
                    startLength = line.length();
                    int pos = line.indexOf('=');
                    if (pos > 0) {
                        String obsName = line.substring(0, pos);
                        consume(pos);
                        int end = iterDouble(0);
                        if (end > 0) {
                            String value = line.substring(0, end);
                            consume(end - 1);
                            parseObservation(obsName, value, result);
                        }
                        if (null != line && line.startsWith(",")) {
                            consume(0);
                            consumeWhitespace();
                        }
                    }
                } while (!isEndOfLine(startLength) && !line.startsWith("}"));
                if (null != line && line.startsWith("}")) {
                    consume(0);
                    consumeWhitespace();
                } else {
                    line = null;
                }
            }
        }
        if (null == line) {
            result = null;
        }
        return result;
    }

    /**
     * Parses an observation.
     * 
     * @param obsName the observable name
     * @param value the value
     * @param result the observables, modified as a side effect
     */
    private void parseObservation(String obsName, String value, Map<IObservable, Double> result) {
        IObservable obs = Observables.valueOf(obsName);
        if (null != obs) {
            try {
                double v = Double.parseDouble(value);
                result.put(obs, v);
            } catch (NumberFormatException e) {
                errorNumberFormat(e.getMessage());
                line = null;
            }
        } else {
            line = null;
        }
    }

    /**
     * Emits a number format error to the error stream.
     * 
     * @param message the number format error message
     */
    private void errorNumberFormat(String message) {
        error("Number format exception while reading line " + line + message + " Stopping!");
    }

    /**
     * Emits an error to the error stream.
     * 
     * @param text the error text
     */
    private void error(String text) {
        if (null != err) {
            err.println(text);
        }
    }
    
    /**
     * Parses the value of a string attribute until the next whitespace.
     * 
     * @param attribute the attribute name
     * @param curValue the default return value
     * @return the value or <code>curValue</code> if the value cannot be parsed
     */
    public String parseString(String attribute, String curValue) {
        return parseString(attribute, curValue, null);
    }    
    
    /**
     * Parses the value of a string attribute until the next whitespace or stopword if given.
     * 
     * @param attribute the attribute name
     * @param curValue the default return value
     * @param stopwords optional stop words, if given allow parsing more than one whitespace separated attribute value 
     *   as long as part of the value is not a stopword itself, <b>null</b> for no stopwords
     * @return the value or <code>curValue</code> if the value cannot be parsed
     */
    public String parseString(String attribute, String curValue, Set<String> stopwords) {
        String result = curValue;
        if (isAttribute(attribute)) {
            consume(attribute.length() + 1);
            line = line.trim();
            int pos;
            if (line.startsWith("\"")) {
                pos = line.indexOf('\"', 1); // simplified
            } else {
                pos = line.indexOf(' ', 0); // simplified
            }
            if (null != stopwords && pos >= 0 && pos < line.length()) {
                do {
                    int p = pos;
                    while (p < line.length() && Character.isWhitespace(line.charAt(p))) {
                        p++;
                    }
                    int start = pos;
                    p = line.indexOf(' ', p); // simplified
                    if (p > 0 && p < line.length()) {
                        if (stopwords.contains(line.substring(start, p).trim())) {
                            break;
                        }
                        pos = p;
                    } else {
                        break;
                    }
                } while (pos >= 0 && pos < line.length());
            }
            if (pos < 0) {
                result = line;
                line = null;
            } else if (pos > 0) {
                result = line.substring(0, pos);
                consume(pos);
            } else {
                line = null;
            }
        }
        return result;
    }
    
    /**
     * Returns whether {@link #line} starts with the prefix for <code>attribute</code>.
     * 
     * @param attribute the attribute to check
     * @return <code>true</code> if {@link #line} is not <b>null</b> and starts with the attribute 
     *     prefix, <code>false</code> else
     */
    private boolean isAttribute(String attribute) {
        return (null != line && line.startsWith(attribute + AbstractEvent.ATTRIBUTE_NAME_SEPARATOR));
    }
    
    /**
     * Returns whether we are at the end of a line.
     * 
     * @param startLength the start line length at the beginning of an iteration to check whether line changed 
     *   (may be negative to ignore)
     * @return <code>true</code> if end of line, <code>false</code> else
     */
    protected boolean isEndOfLine(int startLength) {
        return null == line || 0 == line.length() || startLength == line.length();
    }
    
    /**
     * Iterates over a double value.
     * 
     * @param pos the position to start at
     * @return the position at the end of the double or <code>pos</code> if there is no double
     */
    protected int iterDouble(int pos) {
        while (pos < line.length() && (Character.isDigit(line.charAt(pos)) || '.' == line.charAt(pos))) {
            pos++;
        }
        return pos;
    }
    
    /**
     * Parses an attribute for a component key.
     * 
     * @param attribute the attribute name
     * @return the component key or <b>null</b> if nothing can be parsed
     */
    public ComponentKey parseComponentKey(String attribute) {
        ComponentKey result = null;
        if (isAttribute(attribute)) {
            consume(attribute.length() + 1);
            if (line.startsWith("null ")) {
                consume(4);
            } else {
                try {
                    result = ComponentKey.valueOf(line);
                    if (null != result) {
                        String tmp = result.toString();
                        consume(tmp.length());
                    }
                } catch (NumberFormatException e) {
                    errorNumberFormat(e.getMessage());
                    line = null;
                }
            }
        }
        return result;
    }

    /**
     * Consumes {@link line} until <code>pos</code>.
     * 
     * @param pos the position to consume to
     */
    void consume(int pos) {
        line = LogReader.consume(line, pos);
    }
    
    /**
     * Consumes trailing whitespaces.
     */
    void consumeWhitespace() {
        line = LogReader.consumeWhitespace(line);
    }

    @Override
    public String toString() {
        return line;
    }

    /**
     * For testing.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        Set<String> stop = new HashSet<String>();
        stop.add("algorithm");
        stop.add("pipelineElement");
        EventLineParser p = new EventLineParser("algorithm: Random Sink pipelineElement:", System.out);
        System.out.println(p.parseString("algorithm", "", stop));

        p = new EventLineParser("algorithm: Random pipelineElement:", System.out);
        System.out.println(p.parseString("algorithm", "", stop));
        
        stop.clear();
        stop.add("observations");
        stop.add("pipelineElement");
        stop.add("key");
        stop.add("pipeline");
    }
    
}
