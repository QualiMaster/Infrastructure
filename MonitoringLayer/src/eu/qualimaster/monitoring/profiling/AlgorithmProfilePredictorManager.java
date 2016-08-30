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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Class to handle update/correct- an predict-requests.
 * For this it also handles the creation of AlgorithmProfilePredictorAlgorithm instances 
 * incl. an analogy based creation of new/unknown configurations.
 * This includes a mapping of identifiers to (a) corresponding instance(s).
 * Furthermore the instances can be 
 * <p>- loaded (from file system to the java-vm),
 * <p>- stored (to file system),
 * <p>- removed (from the java-vm) or 
 * <p>- deleted (from the java-vm and the file system).
 * 
 * @author Christopher Voges
 *
 */

public class AlgorithmProfilePredictorManager {
    /**
     * Location (relative path from root-folder) of all known {@link AlgorithmProfilePredictorAlgorithm} instances. 
     */
    private String pathToRootFolder = "predictors";
    /**
     * Storage for loaded or newly created {@link AlgorithmProfilePredictorAlgorithm} instances.
     */
    private Map<String, AlgorithmProfile> storage = new HashMap<>();
    
    /**
     * Sets the root folder for following save- and load-calls.
     * @param pathToRootFolder Relative path for the storage of {@link AlgorithmProfilePredictor} instances.
     */
    public void setPathToRootFolder(String pathToRootFolder) {
        this.pathToRootFolder = pathToRootFolder;
    }
    // TODO NOW
//    public void load() {    }
//    public AlgorithmProfilePredictorAlgorithm getPredictor(/* TODO identifiers */) { return null;}
    /**
     * Removes all {@link AlgorithmProfile} instances without storing them.
     */
    public void removeAll() {
        storage = new HashMap<>();
    }
    
    /**
     * Removes the given {@link AlgorithmProfile} instance without storing it.
     * @param toRemove The {@link AlgorithmProfile} instance to remove.
     */
    public void remove(AlgorithmProfile toRemove) {
        storage.remove(toRemove);
    }
    
    /**
     * Deletes the given {@link AlgorithmProfile} from the hard drive.
     * @param toRemove The {@link AlgorithmProfile} to delete.
     */
    public void delete(AlgorithmProfile toRemove) {
        // TODO
    }
    
    /**
     * Saves all {@link AlgorithmProfile} in storage to the hard drive.
     * @throws IOException Exception if the write process is not possible / succesfully completed.
     */
    public void storeAll() throws IOException {
        for (Map.Entry<String, AlgorithmProfile> entry : storage.entrySet()) {
            store(entry.getValue());
        }
        
    }
    /**
     * Saves the given {@link AlgorithmProfile} instance to the hard drive.
     * @param profile The {@link AlgorithmProfile} to store.
     * @return true if the storing was successful
     * @throws IOException Exception if the write process is not possible / successfully completed.
     */
    public boolean store(AlgorithmProfile profile) throws IOException {
        AlgorithmProfilePredictorAlgorithm predictor = profile.getPredictor();
        String identifier = profile.getStringKey();
        boolean result = false;
        ArrayList<String> mapData = new ArrayList<>();
        int id = -1;
        File folder = new File(pathToRootFolder);
        // Get subfolder from nesting information 
        String[] nesting = identifier.split(";")[0].split(":");
        for (String string : nesting) {
            folder = new File(folder, string);
        }
        // set kind of the predictor as subfolder
        String subfolder = "other";
        if (predictor instanceof Kalman) {
            subfolder = "PREDICTOR=kalman";
        }
        folder = new File(folder, subfolder);
        
        // Create folders, if needed
        if (!folder.exists()) {
            folder.mkdirs();
        }
        // load map-file
        File mapFile = new File(folder, "_map");
        if (mapFile.exists()) {
            mapData = loadTxtFile(mapFile);
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
        result = writeTxtFile(instanceFile, instanceString);
        
        // update map-file, if needed
        if (!entryFound) {
            mapData.set(0, String.valueOf(id));
            mapData.add(id + "|" + identifier);
            result = writeTxtFile(mapFile, mapData);
        } 
        return result;
    }
    // OUTSORCED to AlgorithmProfile
//    /**
//     * Generates a String identifier using the given parameters.
//     * @param nesting Information how the instance represented by the resulting identifier is nested inside the 
//     *        infrastructure. The List starts at the highest level. This information is mandatory.
//     *        The entries need to have the following formatting 'name of the level' followed by '='  
//     *        Expected structure:
//     *        <p> 1. Element: Identifier of the pipeline (e.g. 'pipeline=somepipeline')
//     *        <p> 2. Element: Identifier of the family element inside the pipeline
//     *        <p> 3. Element: Identifier of the algorithm inside the family element
//     * @param predictedValue Name of the observed/predicted value.
//     * @param algorithmParameters Relevant parameters set in the algorithm, 
//     *        containing at least the number of tasks and executors.
//     * @param otherObservables Other relevant observable values (e.g. input rate), may be <null>.
//     * @return String identifier representing this parameter combination
//     */
//    public String calculateIdentifier(LinkedHashMap<String, String> nesting, IObservable predictedValue, 
//            SortedMap<String, Double> algorithmParameters, SortedMap<String, Double> otherObservables) {
//        String result = null;
//        if (sanityCheck(nesting, predictedValue, algorithmParameters)) {
//            result = "";
//            for (Map.Entry<String, String> entry : nesting.entrySet()) {
//                result += entry.getKey() + "=" + entry.getValue() + ":";
//            }
//            result += ";PREDICTED=" + predictedValue + ";algorithmParameters=[";
//            for (Map.Entry<String, Double> entry : algorithmParameters.entrySet()) {
//                result += entry.getKey() + "=" + entry.getValue() + ":";
//            }
//            result += "];";
//            
//            if (null != otherObservables) {
//                result += "otherObersvables=[";
//                for (Map.Entry<String, Double> entry : otherObservables.entrySet()) {
//                    result += entry.getKey() + "=" + entry.getValue() + ":";
//                }
//                result += "];";
//            }
//        }
//        return result;
//    }
//    
//    /**
//     * Checks if the given criteria violate certain minimal criteria. 
//     * @param nesting Information how the instance represented by the resulting identifier is nested inside the 
//     *        infrastructure.
//     * @param predictedValue Name of the observed/predicted value.
//     * @param algorithmParameters Relevant parameters set in the algorithm, 
//     *        containing at least the number of tasks and executors.
//     * @return <true> if no sanity check fails, <false> otherwise
//     */
//    private boolean sanityCheck(LinkedHashMap<String, String> nesting, IObservable predictedValue, 
//            SortedMap<String, Double> algorithmParameters) {
//        boolean sane = true;
//        
//        // null checks
//        if (null == nesting || null == predictedValue || null == algorithmParameters) {
//            sane = false;
//        // length checks    
//        } else if (nesting.size() == 0) {
//            sane = false;
//        // at least TASKS and EXECUTORS must be given as parameters
//        } else if (!algorithmParameters.keySet().contains("TASKS") 
//                || !algorithmParameters.keySet().contains("EXECUTORS")) {
//            System.err.println(this.toString() + ": The identifier must specify TASKS and EXECUTORS.");
//            sane = false;
//        // TASKS and EXECUTORS must be equal or greater 1
//        } else if (algorithmParameters.get("TASKS") < 1 || algorithmParameters.get("EXECUTORS") < 1) {
//            sane = false;
//        }
//        return sane;
//    }
    
    /**
     * Loads the content of the given file into the returned {@link ArrayList}.
     * @param file {@link File} to load.
     * @return Content 
     */
    private ArrayList<String> loadTxtFile(File file) {
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
            e.printStackTrace();
        }
        
        return content;
    }
    /**
     * Stores the content to the given file.
     * @param file {@link File} to write to.
     * @param content The lines to write.
     * @return If the writing was succesfull. 
     */
    private boolean writeTxtFile(File file, ArrayList<String> content) {
        boolean result = false;
        try {
            PrintStream writer = new PrintStream(file);
            for (String out : content) {
                writer.println(out);
            }
            writer.close();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


}
