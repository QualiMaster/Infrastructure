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
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;
import eu.qualimaster.monitoring.profiling.predictors.IAlgorithmProfilePredictor;
import eu.qualimaster.monitoring.profiling.validators.IValidator;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.observables.IObservable;

/**
 * Handles multiple predictors for various observables (separately, not integrated via the predictor).
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
class SeparateObservableAlgorithmProfile implements IAlgorithmProfile {

    private static final Logger LOGGER = LogManager.getLogger(AlgorithmProfilePredictionManager.class);
    private Map<IObservable, IAlgorithmProfilePredictor> predictors = new HashMap<>();
    
    private PipelineElement element;
    private Map<Object, Serializable> key;
    
    /**
     * Generates an empty {@link SeparateObservableAlgorithmProfile}.
     * 
     * @param element the pipeline element
     * @param key the profile key
     */
    public SeparateObservableAlgorithmProfile(PipelineElement element, Map<Object, Serializable> key) {
        this.element = element;
        this.key = key;
    }

    /**
     * Generates a string key (identifier) based on the attributes.
     * 
     * @param observable the observable to be predicted
     * 
     * @return The key representing this {@link SeparateObservableAlgorithmProfile} instance in its 
     *     current configuration.
     */
    private String generateKey(IObservable observable) {
        return generateKey(element, key, observable);
    }
    
    /**
     * Generates a string key (identifier).
     * 
     * @param element the pipeline element
     * @param key the profile key
     * @param observable the observable to be predicted
     * 
     * @return The key representing this {@link SeparateObservableAlgorithmProfile} instance in its 
     *     current configuration.
     */
    private static String generateKey(PipelineElement element, Map<Object, Serializable> key, IObservable observable) {
        return getStorageStrategy(element).generateKey(element, key, observable, true);
    }

    /**
     * Returns the storage strategy of the given pipeline <code>element</code>.
     * 
     * @param element the pipeline element
     * @return the storage strategy
     */
    private static IStorageStrategy getStorageStrategy(PipelineElement element) {
        return element.getProfileCreator().getStorageStrategy();
    }
    
    /**
     * Returns all known parameter values.
     * 
     * @param element the element to return the values for
     * @param key the profile key
     * @param observable the observable
     * @param parameter the parameter name
     * @return the parameter values (here as strings regardless of type)
     * @throws IOException in case that the map file of the profile cannot be loaded
     */
    static List<String> getKnownParameterValues(PipelineElement element, Map<Object, Serializable> key, 
        IObservable observable, String parameter) throws IOException {
        File folder = getStorageStrategy(element).getPredictorPath(element, element.getPath(), key, observable);
        MapFile mapFile = new MapFile(folder);
        mapFile.load();
        return readKnownParameterValues(mapFile, parameter);
    }

    /**
     * Returns the known parameter values from a map file. [public for testing]
     * 
     * @param mapFile the map file
     * @param parameter the parameter
     * @return the known values as strings (regardless of type)
     */
    public static List<String> readKnownParameterValues(MapFile mapFile, String parameter) {
        List<String> result = new ArrayList<>();
        for (String k : mapFile.keys()) {
            int pos = k.indexOf(";parameters=");
            if (pos > 0) {
                String paramId = parameter + "=";
                pos = k.indexOf(parameter + "=", pos + 1);
                if (pos > 0) {
                    pos += paramId.length();
                    int end = k.indexOf(",", pos);
                    if (end < 0) {
                        end = k.indexOf("}", pos);
                    }
                    if (end > 0) {
                        result.add(k.substring(pos, end));
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public void store() {
        for (Map.Entry<IObservable, IAlgorithmProfilePredictor> ent : predictors.entrySet()) {
            try {
                IObservable observable = ent.getKey();
                // this is not really efficient
                store(ent.getValue(), getFolder(observable), generateKey(observable));
            } catch (IOException e) {
                LOGGER.error("While writing profile: " + e.getMessage());
            }
        }
    }
    
    @Override
    public File getFolder(IObservable observable) {
        return getStorageStrategy(element).getPredictorPath(element, element.getPath(), key, observable);
    }

    /**
     * Returns the folder for a predictor.
     * 
     * @param path the base path
     * @param identifier the profile identifier
     * @return the folder
     */
    /*private File getFolder(String path, String identifier) {
        return getFolder(element, path, identifier);
    }*/
    
    /**
     * Returns the folder for a predictor.
     * 
     * @param element the pipeline element
     * @param path the base path
     * @param identifier the profile identifier
     * @return the folder
     */
    /*private static File getFolder(PipelineElement element, String path, String identifier) {
        File folder = new File(path);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(";")[0].split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        // set kind of the predictor as subfolder
        String subfolder = element.getProfileCreator().getStorageSubFolder();
        return new File(folder, subfolder);
    }*/
    
    /**
     * Stores a given predictor.
     * 
     * @param predictor the predictor
     * @param folder the target folder for persisting the predictor instances
     * @param identifier the predictor identifier
     * @throws IOException if saving the predictor fails
     */
    private void store(IAlgorithmProfilePredictor predictor, File folder, String identifier) throws IOException {
        // Create folders, if needed
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // load map-file
        MapFile mapFile = new MapFile(folder);
        mapFile.load();
        
        boolean newEntry = false;
        int id = mapFile.get(identifier);
        if (id < 0) {
            id = mapFile.size() + 1;
            newEntry = true;
        }

        File instanceFile = MapFile.getFile(folder, id);
        predictor.store(instanceFile, identifier);
        
        // update map-file, if needed
        if (newEntry) {
            mapFile.put(identifier, id);
            mapFile.store();
        } 
    }
    
    /**
     * Loads a predictor back if possible.
     * 
     * @param predictor the predictor to load into
     * @param folder the folder for persisting the predictor instances
     * @param identifier the predictor identifier
     * @throws IOException if saving the predictor fails
     */
    private void load(IAlgorithmProfilePredictor predictor, File folder, String identifier) throws IOException {
        MapFile mapFile = new MapFile(folder);
        mapFile.load();
        File instanceFile = mapFile.getFile(identifier);
        predictor.load(instanceFile, identifier);
    }

    /**
     * Obtains a predictor and creates one if permissible.
     * 
     * @param observable the observable to obtain a predictor for
     * @return the predictor
     */
    private IAlgorithmProfilePredictor obtainPredictor(IObservable observable) {
        IAlgorithmProfilePredictor predictor = predictors.get(observable);
        if (null == predictor && null != ProfilingRegistry.getQuantizer(observable, false)) {
            predictor = element.getProfileCreator().createPredictor();
            try {
                load(predictor, getFolder(observable), generateKey(observable));
            } catch (IOException e) {
                LOGGER.error("While reading predictor: " + e.getMessage());
            }
            predictors.put(observable, predictor);
        }
        return predictor;
    }

    @Override
    public double predict(IObservable observable) {
        return predict(observable, 0);
    }

    @Override
    public double predict(IObservable observable, int steps) {
        double result;
        IAlgorithmProfilePredictor predictor = obtainPredictor(observable);
        if (null != predictor) {
            result = predictor.predict(steps);
            IValidator validator = ProfilingRegistry.getValidator(observable);
            if (null != validator) {
                result = validator.validate(result);
            }
        } else {
            result = Constants.NO_PREDICTION;
        }
        return result;
    }

    @Override
    public void update(PipelineNodeSystemPart family) {
        for (IObservable obs : family.getObservables()) {
            if (family.hasValue(obs)) {
                IAlgorithmProfilePredictor predictor = obtainPredictor(obs);
                if (null != predictor) {
                    predictor.update(family.getLastUpdate(obs) / 1000, family.getObservedValue(obs));
                }
            }
        }
    }

}
