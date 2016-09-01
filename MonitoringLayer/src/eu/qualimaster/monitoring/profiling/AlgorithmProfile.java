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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.observables.IObservable;

/**
 * Data class to modulate the relation of
 * <p> - one Pipeline
 * <p> - one FamilyElement
 * <p> - one Algorithm
 * <p> - the {@link IObservable} to predict and its last {@link IObservation} 
 * represented by their values ({@link String} and {@link Double}).
 * Currently only one {@link IObservable} is predicted therefore this {@link Map} always needs to have the size 1! 
 * <p> - and multiple {@link IObservable} and their {@link IObservation}s serving as parameters for the Pipeline,
 * PipelineElement and the Algorithm (e.g. the number of TASKS or EXECUTORS or
 * the current INPUT). They are represented by their values ({@link String} and {@link Double}).
 * <p> Furthermore the {@link IAlgorithmProfilePredictorAlgorithm}
 * instance used to predict the future for the outlined relation is also
 * accessible from this classes instances.
 * 
 * @author Christopher Voges
 *
 */
class AlgorithmProfile {

    private static final Logger LOGGER = LogManager.getLogger(AlgorithmProfilePredictionManager.class);
    private Map<IObservable, IAlgorithmProfilePredictorAlgorithm> predictors = new HashMap<>();
    
    private PipelineElement element;
    private Map<Object, Serializable> key;
    
    /**
     * Generates an empty {@link AlgorithmProfile}.
     * 
     * @param element the pipeline element
     * @param key the profile key
     */
    public AlgorithmProfile(PipelineElement element, Map<Object, Serializable> key) {
        this.element = element;
        this.key = key;
    }

    /**
     * Generates a string key (identifier) based on the attributes.
     * 
     * @param observable the observable to be predicted
     * 
     * @return The key representing this {@link AlgorithmProfile} instance in its current configuration.
     */
    private String generateKey(IObservable observable) {
        String pipelineName = element.getPipeline().getName();
        String elementName = element.getName();
        String algorithm = keyToString(Constants.KEY_ALGORITHM);
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<Object, Serializable> ent : key.entrySet()) {
            sorted.put(ent.getKey().toString(), ent.getValue().toString());
        }
        return "PIPELINE=" + pipelineName + ";element=" + elementName + ";algorithm=" + algorithm
                + ";predicted=" + observable.name() + ";parameters=" + sorted;
    }
    
    /**
     * Turns a key part into a string.
     * 
     * @param part the key part
     * @return the string representation
     */
    private String keyToString(Object part) {
        Serializable tmp = key.get(part);
        return null == tmp ? "" : tmp.toString();
    }
    
    /**
     * Clears this instance.
     * 
     * @param path the target path for persisting the predictor instances
     */
    void store(String path) {
        for (Map.Entry<IObservable, IAlgorithmProfilePredictorAlgorithm> ent : predictors.entrySet()) {
            store(ent.getValue(), path, generateKey(ent.getKey()));
        }
    }
    
    /**
     * Stores a given predictor.
     * 
     * @param predictor the predictor
     * @param path the target path for persisting the predictor instances
     * @param identifier the predictor identifier
     * @return <code>true</code> if successful, <code>false</code> else
     */
    boolean store(IAlgorithmProfilePredictorAlgorithm predictor, String path, String identifier) {
        boolean result = false;
        ArrayList<String> mapData = new ArrayList<>();
        int id = -1;
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(";")[0].split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        // set kind of the predictor as subfolder
        String subfolder = predictor.getIdentifier();
        folder = new File(folder, subfolder);
        
        // Create folders, if needed
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // load map-file
        File mapFile = new File(folder, "_map");
        if (mapFile.exists()) {
            mapData = loadTxtFile(mapFile, identifier);
            id = new Integer(mapData.get(0).trim());
             
        } else {
            mapData.add(String.valueOf(id));
        }
        // Check if the current identifier is already used, if so: remember the line and the id
        // Entries look like 'id|identifier'
        int entrieNumber = 0;
        boolean entryFound = false;
        while (entrieNumber < mapData.size() && !entryFound) {
            String entry = mapData.get(entrieNumber);
            // Case 1: Identifier already in map-file: use old id
            if (entry.split("|")[1].equals(identifier)) {
                id = new Integer(entry.split("\\|")[0]);
                entryFound = true;
            } else {
                entrieNumber++;
            }
            
        }
        // Case 1: No entry found: New identifier -> new id
        if (!entryFound) {
            id++;
        } 
            
        // generate file
        File instanceFile = new File(folder, id + "");
        // write instance to file
        ArrayList<String> instanceString = predictor.toStringArrayList();
        result = writeTxtFile(instanceFile, instanceString, identifier);
        
        // update map-file, if needed
        if (!entryFound) {
            mapData.set(0, String.valueOf(id));
            mapData.add(id + "|" + identifier);
            result = writeTxtFile(mapFile, mapData, identifier);
        } 
        return result;
    }
    
    /**
     * Loads the content of the given file into the returned {@link ArrayList}.
     * @param file {@link File} to load.
     * @param key the identification key for this profile
     * @return Content 
     */
    private ArrayList<String> loadTxtFile(File file, String key) {
        ArrayList<String> content = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // No comments or empty lines
                    if (!(line.startsWith("#")) && !(line.isEmpty())) {
                        content.add(line);
                    }
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            LOGGER.error("While storing the predictor of " + key, e);
        }
        
        return content;
    }
    /**
     * Stores the content to the given file.
     * 
     * @param file {@link File} to write to.
     * @param content The lines to write.
     * @param key the identification key for this profile
     * @return If the writing was successful. 
     */
    private boolean writeTxtFile(File file, ArrayList<String> content, String key) {
        boolean result = false;
        try {
            PrintStream writer = new PrintStream(file);
            for (String out : content) {
                writer.println(out);
            }
            writer.close();
            result = true;
        } catch (IOException e) {
            LOGGER.error("While storing the predictor of " + key, e);
        }
        return result;
    }

    /**
     * Obtains a predictor and creates one if permissible.
     * 
     * @param observable the observable to obtain a predictor for
     * @return the predictor
     */
    private IAlgorithmProfilePredictorAlgorithm obtainPredictor(IObservable observable) {
        IAlgorithmProfilePredictorAlgorithm predictor = predictors.get(observable);
        if (null == predictor && null != QuantizerRegistry.getQuantizer(observable)) {
            // TODO try loading
            predictor = new Kalman();
            predictors.put(observable, predictor);
        }
        return predictor;
    }

    /**
     * Predicts the next value for <code>observable</code>.
     * 
     * @param observable the observable to predict for
     * @return the predicted value, {@link Constants#NO_PREDICTION} if no prediction is possible
     */
    double predict(IObservable observable) {
        return predict(observable, 0); // TODO really 0?
    }

    /**
     * Predicts a value for the given <code>obserable</code>.
     * 
     * @param observable the observable to predict for
     * @param steps Number of steps to predict ahead.
     *      <p> steps = 0: Predict one step after the time step of the last update.
     *      <p> steps > 0: Predict X step(s) ahead of 'now'.
     * @return the predicted value, {@link Constants#NO_PREDICTION} if no prediction is possible
     */
    double predict(IObservable observable, int steps) {
        double result;
        IAlgorithmProfilePredictorAlgorithm predictor = obtainPredictor(observable);
        if (null != predictor) {
            result = predictor.predict();
        } else {
            result = Constants.NO_PREDICTION;
        }
        return result;
    }

    /**
     * Updates the profile according to the measurements in <code>family</code>.
     * 
     * @param family the family used to update the predictors
     */
    void update(PipelineNodeSystemPart family) {
        for (IObservable obs : family.getObservables()) {
            if (family.hasValue(obs)) {
                IAlgorithmProfilePredictorAlgorithm predictor = obtainPredictor(obs);
                if (null != predictor) {
                    predictor.update(family.getLastUpdate(obs) / 1000, family.getObservedValue(obs));
                }
            }
        }
    }

}
