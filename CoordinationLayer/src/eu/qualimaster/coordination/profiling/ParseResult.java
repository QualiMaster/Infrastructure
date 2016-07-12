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
package eu.qualimaster.coordination.profiling;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.coordination.profiling.ProcessingEntry.ProjectionType;

/**
 * Represents the result of parsing.
 * 
 * @author Holger Eichelberger
 */
public class ParseResult {

    private File dataFile;
    private Set<ProcessingEntry> processing = new LinkedHashSet<ProcessingEntry>();
    private Map<String, Set<Serializable>> parameters = new HashMap<String, Set<Serializable>>();

    /**
     * Creates a result instance.
     */
    ParseResult() {
    }
    
    /**
     * Changes the data file that shall be used.
     * 
     * @param dataFile the data file
     */
    void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }
    
    /**
     * Returns the data file that shall be used.
     * 
     * @return the data file
     */
    public File getDataFile() {
        return dataFile;
    }
    
    /**
     * Returns the processing entries that shall be varied.
     * 
     * @return the processing entries (if none was read from a control file, the result contains at least [1;1;1])
     */
    public List<ProcessingEntry> getProcessingEntries() {
        if (processing.isEmpty()) {
            processing.add(new ProcessingEntry(1, 1, 1));
        }
        List<ProcessingEntry> result = new ArrayList<ProcessingEntry>();
        result.addAll(processing);
        return result;
    }
    
    /**
     * Returns the individual worker entries.
     * 
     * @return the executor entries
     */
    public List<Integer> getWorkers() {
        return ProcessingEntry.project(ProjectionType.WORKER, processing);
    }

    /**
     * Returns the individual worker entries.
     * 
     * @return the executor entries
     */
    public List<Integer> getExecutors() {
        return ProcessingEntry.project(ProjectionType.EXECUTOR, processing);
    }
    
    /**
     * Returns the individual task entries.
     * 
     * @return the task entries
     */
    public List<Integer> getTasks() {
        return ProcessingEntry.project(ProjectionType.TASK, processing);
    }
    
    /**
     * Returns the parameter name - value variation mapping.
     *  
     * @return the parameter name / value variation mapping
     */
    public Map<String, List<Serializable>> getParameters() {
        Map<String, List<Serializable>> result = new HashMap<String, List<Serializable>>();
        for (Map.Entry<String, Set<Serializable>> ent : parameters.entrySet()) {
            List<Serializable> tmp = new ArrayList<Serializable>();
            tmp.addAll(ent.getValue());
            result.put(ent.getKey(), tmp);
        }
        return result;
    }
    
    /**
     * Returns all parameter names defined.
     * 
     * @return the parameter names
     */
    public Set<String> getParameterNames() {
        return parameters.keySet();
    }
    
    /**
     * Merges the corresponding entries from <code>tasks</code>, <code>executors</code>, <code>workers</code> 
     * into {@link #processing}.
     * 
     * @param tasks the tasks
     * @param executors the executors
     * @param workers the workers
     */
    void merge(List<Integer> tasks, List<Integer> executors, List<Integer> workers) {
        int size = Math.min(tasks.size(), Math.min(executors.size(), workers.size()));
        for (int m = 0; m < size; m++) {
            processing.add(new ProcessingEntry(tasks.get(m), executors.get(m), workers.get(m)));
        }
    }
    
    /**
     * Adds a parameter value (values are added only once).
     * 
     * @param parameterName the name of the parameter
     * @param value the value
     */
    void addParameter(String parameterName, Serializable value) {
        Set<Serializable> existing = parameters.get(parameterName);
        if (null == existing) {
            existing = new LinkedHashSet<Serializable>();
            parameters.put(parameterName, existing);
        }
        existing.add(value);
    }
    
    /**
     * Merges the results form <code>result</code> into this instance.
     * 
     * @param result the instance to be merged
     * @param includeDataFile if the data file information shall be considered
     */
    void merge(ParseResult result, boolean includeDataFile) {
        if (includeDataFile) {
            dataFile = result.dataFile;
        }
        processing.addAll(result.processing);
        for (Map.Entry<String, Set<Serializable>> entry : result.parameters.entrySet()) {
            String parameterName = entry.getKey();
            Set<Serializable> existing = parameters.get(parameterName);
            if (null == existing) {
                existing = new LinkedHashSet<Serializable>();
                parameters.put(parameterName, existing);
            } 
            existing.addAll(entry.getValue());
        }
    }
    
    @Override
    public String toString() {
        return dataFile + " " + processing + " " + parameters;
    }

}
