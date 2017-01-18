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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.monitoring.profiling.quantizers.Quantizer;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Observables;

/**
 * Implements the default storage strategy using hierarchical paths based on the profile key.
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
public class DefaultStorageStrategy implements IStorageStrategy {

    public static final IStorageStrategy INSTANCE = new DefaultStorageStrategy();
    private static final String APPROX_FOLDER = "approximators";
    
    /**
     * Prevents external instantiation.
     */
    private DefaultStorageStrategy() {
    }
    
    @Override
    public File getPredictorPath(PipelineElement element, String path, Map<Object, Serializable> key, 
        IObservable observable) {
        String identifier = generateKey(element, key, observable, false);
        return getPredictorPath(identifier, path, element.getProfileCreator());
    }

    /**
     * Returns the path including the predictor folder.
     * 
     * @param identifier the internal profile identifier
     * @param path the base path
     * @param creator the creator carrying the predictor storage sub folder (ignored if <b>null</b>)
     * @return the path to the predictor folder
     */
    private File getPredictorPath(String identifier, String path, IAlgorithmProfileCreator creator) {
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        File result = folder;
        if (null != creator) {
            result = new File(result, creator.getStorageSubFolder());
        }
        return result;
    }

    @Override
    public File getApproximatorsPath(PipelineElement element, String path, Map<Object, Serializable> key) {
        String identifier = generateKey(element, key, null, false);
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        return new File(folder, APPROX_FOLDER);
    }

    @Override
    public String generateKey(PipelineElement element, Map<Object, Serializable> key, IObservable observable, 
        boolean includeParameters) {
        boolean profiling = element.isInProfilingMode();
        String pipelineName = element.getPipeline().getName();
        String elementName = element.getName();
        return generateKey(pipelineName, elementName, key, observable, profiling, includeParameters);
    }

    @Override
    public File getPredictorPath(String pipeline, String element, String algorithm, String path, IObservable observable,
        IAlgorithmProfileCreator creator) {
        Map<Object, Serializable> key = new HashMap<Object, Serializable>();
        key.put(Constants.KEY_ALGORITHM, algorithm);
        String identifier = generateKey(pipeline, element, key, observable, null == pipeline || null == element, false);
        return getPredictorPath(identifier, path, null == observable ? null : creator);
    }

    // checkstyle: stop parameter number check

    /**
     * Generates a string key (identifier).
     * 
     * @param pipelineName the pipeline name 
     * @param elementName the pipeline element name
     * @param key the profile key (may be <b>null</b> if the algorithm part/following parameters are not needed)
     * @param observable the observable to be predicted (may be <b>null</b> if the observable/following parameters
     *    are not needed)
     * @param profiling are we in profile mode
     * @param includeParameters include the parameters into the key
     * 
     * @return The key representing this {@link SeparateObservableAlgorithmProfile} instance in its 
     *     current configuration.
     */
    private String generateKey(String pipelineName, String elementName, Map<Object, Serializable> key, 
        IObservable observable, boolean profiling, boolean includeParameters) {
        String result;
        if (profiling) {
            result = "";
        } else {
            result = "pipeline=" + pipelineName + ":element=" + elementName + ":";
        }
        if (null != key) {
            String algorithm = keyToString(key, Constants.KEY_ALGORITHM);
            result += "algorithm=" + algorithm;
        }
        if (null != observable) {
            result += ":predicted=" + observable.name();
        }
        if (includeParameters) {
            TreeMap<String, String> sorted = new TreeMap<>();
            for (Map.Entry<Object, Serializable> ent : key.entrySet()) {
                String k = keyToString(ent.getKey());
                if (!Constants.KEY_ALGORITHM.equals(k)) {
                    sorted.put(k, valueToString(ent.getValue()));
                }
            }
            result += ";parameters=" + sorted;
        }
        return result;
    }

    // checkstyle: resume parameter number check

    @Override
    public ProfileKey parseKey(String key) {
        ProfileKeyParser parser = new ProfileKeyParser();
        return parser.parse(key);
    }
    
    /**
     * Implements a profile key parser.
     * 
     * @author Holger Eichelberger
     */
    private static class ProfileKeyParser {
        
        private String key;

        /**
         * Parses text back into a profile key.
         * 
         * @param text the text
         * @return the profile key
         */
        private ProfileKey parse(String text) {
            this.key = text;
            int pos = key.lastIndexOf(";parameters=");
            Map<Object, Serializable> parameters = null;
            if (pos > 0) {
                parameters = stringToParameters(key.substring(pos + 12));
                key = key.substring(0, pos);
            }
            String pipeline = parseNextEntry("pipeline=");
            String element = parseNextEntry("element=");
            String algorithm = parseNextEntry("algorithm=");
            String observed = parseNextEntry("predicted=");
            return new ProfileKey(pipeline, element, algorithm, Observables.valueOf(observed), parameters);
        }
        
        /**
         * Parses the next entry.
         * 
         * @param prefix the next entry
         * @return the entry value (<b>null</b> if parsing is not possible)
         */
        private String parseNextEntry(String prefix) {
            String result = null;
            if (key.startsWith(prefix)) {
                int pos = key.indexOf("=");
                int end = key.indexOf(":");
                if (end < 0) {
                    end = key.length();
                }
                if (pos > 0 && end > 0) {
                    result = key.substring(pos + 1, end);
                }
                if (end + 1 > key.length()) {
                    key = "";
                } else {
                    key = key.substring(end + 1);
                }
            }
            return result;
        }
        
    }
    
    /**
     * Turns a parameters into a string.
     * 
     * @param key the parameters
     * @return the string representation
     */
    public static String parametersToString(Map<Object, Serializable> key) {
        String result = "{";
        Iterator<Map.Entry<Object, Serializable>> iter = key.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Object, Serializable> ent = iter.next();
            result += keyValueToString(ent.getKey(), ent.getValue());
            if (iter.hasNext()) {
                result += ",";
            }
        }
        return result + "}";
    }
    
    /**
     * Turns a key part into a string.
     *  
     * @param key the key
     * @return the string
     */
    public static String keyToString(Object key) {
        String k = key.toString();
        if (needsQuotation(k)) {
            k = quote(k);
        }
        return k;
    }

    /**
     * Turns a value part into a string.
     *  
     * @param value the value
     * @return the string
     */
    public static String valueToString(Object value) {
        String v = value.toString();
        if (value instanceof String || needsQuotation(v)) {
            v = quote(v);
        }
        return v;
    }
    
    /**
     * Turns a key-value pair into a string.
     *  
     * @param key the key
     * @param value the value
     * @return the string
     */
    public static String keyValueToString(Object key, Serializable value) {
        return keyToString(key) + "=" + valueToString(value);
    }
        
    /**
     * Turns a string into parameters.
     * 
     * @param text the text
     * @return the parameters
     */
    public static Map<Object, Serializable> stringToParameters(String text) {
        Map<Object, Serializable> result = new HashMap<Object, Serializable>();
        String k = text.trim();
        if (k.startsWith("{") && k.endsWith("}")) {
            k = k.substring(1, k.length() - 1);
            int pos = 0;
            while (pos < k.length()) {
                pos = consumeWhitespaces(k, pos);
                int keyStart = pos;
                pos = consumeString(k, pos, '=');
                int keyEnd = pos;
                if (pos < k.length() && '=' == k.charAt(pos)) {
                    pos = consumeWhitespaces(k, pos + 1);
                    int valueStart = pos;
                    pos = consumeString(k, pos, ',');
                    int valueEnd = pos;
                    if (valueEnd > 0) {
                        String keyString = unquote(k.substring(keyStart, keyEnd).trim());
                        String valueString = unquote(k.substring(valueStart, valueEnd).trim());
                        addKeyValue(keyString, valueString, result);
                    }
                    if (pos < k.length() && ',' == k.charAt(pos)) {
                        pos++;
                    }
                }
            }
            // read until =
            // if ""
        }
        return result;
    }

    /**
     * Adds a key-value pair to <code>result</code> trying to parse back the serializables.
     * 
     * @param key the key
     * @param value the value
     * @param result the result to be modified as a side effect
     */
    private static void addKeyValue(String key, String value, Map<Object, Serializable> result) {
        Object oKey = key;
        IObservable obs = Observables.valueOf(key);
        if (null != obs) {
            oKey = obs;
        }
        Serializable oVal = null;
        if (null != obs) {
            Quantizer<?> quantizer = ProfilingRegistry.getQuantizer(obs, true);
            oVal = quantizer.parse(value);
        } else {
            // fallback heuristics
            try {
                oVal = Integer.parseInt(value);
            } catch (NumberFormatException e) {
            }
            if (null == oVal) {
                try {
                    oVal = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                }            
            }
        }
        if (null == oVal) {
            oVal = value;
        }
        result.put(oKey, oVal);
    }

    /**
     * Returns whether a text needs quotation.
     * 
     * @param text the text
     * @return <code>true</code> for quotation, <code>false</code> else
     */
    private static boolean needsQuotation(String text) {
        return text.indexOf('=') >= 0 || text.indexOf('\"') >= 0 || text.indexOf(',') >= 0;
    }
    
    /**
     * Quotes a string.
     * 
     * @param text the text
     * @return the quoted text
     */
    private static String quote(String text) {
        return "\"" + text.replaceAll("\"", "#~") + "\"";
    }

    /**
     * Unquotes a string.
     * 
     * @param text the text
     * @return the unquoted text
     */
    private static String unquote(String text) {
        String result = text;
        if (text.startsWith("\"") && text.endsWith("\"")) {
            result = result.substring(0, result.length() - 1);
            result.replaceAll("#~", "\"");
        }
        return result;
    }

    /**
     * Consumes text from pos until separator.
     * 
     * @param text the text
     * @param pos the current position in text
     * @param separator the separator until to consume
     * @return the new position
     */
    private static int consumeString(String text, int pos, char separator) {
        if (pos < text.length()) {
            if ('"' == text.charAt(pos)) {
                pos++;
                while (pos < text.length() && text.charAt(pos) != '"') {
                    pos++;
                }
                if ('"' == text.charAt(pos)) {
                    pos++;
                }
            } 
            while (pos < text.length() && text.charAt(pos) != separator) {
                pos++;
            }
        }
        return pos;
    }

    /**
     * Consumes text from pos until end of next whitespaces.
     * 
     * @param text the text
     * @param pos the current position in text
     * @return the new position
     */
    private static int consumeWhitespaces(String text, int pos) {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    /**
     * Turns a key part into a string.
     * 
     * @param key the key
     * @param part the key part
     * @return the string representation
     */
    private static String keyToString(Map<Object, Serializable> key, Object part) {
        Serializable tmp = key.get(part);
        return null == tmp ? "" : tmp.toString();
    }

    @Override
    public boolean isApproximatorsFolder(File file) {
        return APPROX_FOLDER.equals(file.getName());
    }

    @Override
    public String getMapFileName() {
        return MapFile.NAME;
    }

    @Override
    public String getApproximatorFileName(Object parameterName, IObservable observable, String suffix) {
        String name = parameterName + "-" + observable.name();
        if (null != suffix) {
            if (!suffix.startsWith(".")) {
                name += ".";
            }
            name += suffix;
        }
        return Constants.toFileName(name);
    }

    @Override
    public ApproximatorInfo parseApproximatorFileName(String fileName) {
        ApproximatorInfo result = null;
        String name = fileName;
        int pos = name.lastIndexOf('.');
        if (pos > 0) {
            name = name.substring(0, pos);
        }
        // name constants for observables shall not contain a - (naming conventions)
        pos = name.lastIndexOf("-");
        if (pos > 0 && pos < name.length()) { // somewhere in-between
            IObservable obs = Observables.valueOf(name.substring(pos + 1));
            if (null != obs) {
                result = new ApproximatorInfo(name.substring(0, pos), obs);
            }
        }
        return result;
    }

    @Override
    public String stripToProfilingIdentifier(String identifier) {
        String result = identifier;
        if (null != identifier) {
            result = stripLeadingEntry(result, "pipeline=");
            result = stripLeadingEntry(result, "element=");
        }
        return result;
    }
    
    /**
     * Strips until the first separator.
     * 
     * @param identifier the identifier to strip
     * @return the stripped identifier or <code>identifier</code>
     */
    private static String stripUntilFirstSeparator(String identifier) {
        String result = identifier;
        int pos = result.indexOf(":");
        if (pos > 0 && pos < result.length()) {
            result = result.substring(pos + 1);
        }
        return result;
    }

    /**
     * Strips the entire leading entry.
     * 
     * @param identifier the identifier to strip
     * @param entryKey the entry key to strip
     * @return the stripped identifier or <code>identifier</code>
     */
    private static String stripLeadingEntry(String identifier, String entryKey) {
        String result = identifier;
        if (result.startsWith(entryKey)) {
            result = stripUntilFirstSeparator(identifier);
        }
        return result;
    }
    
}
