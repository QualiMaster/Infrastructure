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

import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.monitoring.tracing.TraceReader;
import eu.qualimaster.monitoring.tracing.TraceReader.PipelineEntry;

/**
 * Reads and replays a profile. [preliminary]
 * 
 * @author Holger Eichelberger
 */
public class ProfileReader {

    /**
     * Meta-information about the read profile.
     * 
     * @author Holger Eichelberger
     */
    public static class Meta {
        
        private Map<String, List<String>> predecessors;
        private Map<String, Map<Object, Serializable>> parameters;
        private String algorithm;
        
        /**
         * Creates a meta object.
         * 
         * @param algorithm the name of the profiled algorithm
         * @param predecessors the predecessors per pipeline element
         * @param parameters the parameters per pipeline element
         */
        public Meta(String algorithm, Map<String, List<String>> predecessors, Map<String, 
            Map<Object, Serializable>> parameters) {
            this.algorithm = algorithm;
            this.predecessors = predecessors;
            this.parameters = parameters;
        }
        
        /**
         * Returns the predecessors of a given pipeline name.
         * 
         * @param name the name
         * @return the list of predecessors
         */
        public List<String> getPredecessors(String name) {
            return predecessors.get(name);
        }
        
        /**
         * Returns the actual parameters.
         * 
         * @param name the name of the pipeline element
         * @return the parameters
         */
        public Map<Object, Serializable> getParameters(String name) {
            return parameters.get(name);
        }
        
        /**
         * Returns the profiled algorithm.
         * 
         * @return the algorithm name
         */
        public String getAlgorithm() {
            return algorithm;
        }

    }
    
    /**
     * Reads back a profile.
     * 
     * @param input the input CSV file from profiling
     * @param outputFolder the output folder for the information written by the profile manager
     * @param algorithm the profiled algorithm name
     * @param additionalParams additional algorithm parameters set during the original run (may be <b>null</b>)
     * @return the read entries from the CSV file
     * @throws IOException in case of I/O problems
     */
    public static List<PipelineEntry> readBackProfile(File input, File outputFolder, String algorithm,
        Map<Object, Serializable> additionalParams) throws IOException {
        Map<String, List<String>> predecessors = new HashMap<String, List<String>>();
        List<String> pred = new ArrayList<String>();
        pred.add(AlgorithmProfileHelper.SRC_NAME);
        predecessors.put(AlgorithmProfileHelper.FAM_NAME, pred);
        Map<Object, Serializable> param = new HashMap<Object, Serializable>();
        if (null != additionalParams) {
            param.putAll(additionalParams);
        }
        Map<String, Map<Object, Serializable>> parameters = new HashMap<String, Map<Object, Serializable>>();
        parameters.put(AlgorithmProfileHelper.FAM_NAME, param);
        
        Meta meta = new Meta(algorithm, predecessors, parameters);
        TraceReader reader = new TraceReader();
        List<PipelineEntry> entries = reader.readCsv(input);
        AlgorithmProfilePredictionManager.useTestData(outputFolder.getAbsolutePath());
        AlgorithmProfilePredictionManager.start();
        AlgorithmProfilePredictionManager.fill(entries, meta);
        AlgorithmProfilePredictionManager.stop();
        return entries;
    }
    
}
