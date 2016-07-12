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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a processing entry.
 * 
 * @author Holger Eichelberger
 */
public class ProcessingEntry {
    
    private int tasks;
    private int executors;
    private int workers;

    /**
     * Creates a processing entry.
     * 
     * @param tasks the number of tasks (negative is set to zero)
     * @param executors the number of executors (negative is set to zero)
     * @param workers the number of workers (negative is set to zero)
     */
    ProcessingEntry(int tasks, int executors, int workers) {
        this.tasks = Math.max(0, tasks);
        this.executors = Math.max(0, executors);
        this.workers = Math.max(0, workers);
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean result;
        if (obj instanceof ProcessingEntry) {
            ProcessingEntry me = (ProcessingEntry) obj;
            result = tasks == me.tasks && executors == me.executors && workers == me.workers;
        } else {
            result = false;
        }
        return result;
    }

    /**
     * Returns the number of tasks.
     * 
     * @return the number of tasks
     */
    public int getTasks() {
        return tasks;
    }

    /**
     * Returns the number of executors.
     * 
     * @return the number of executors
     */
    public int getExecutors() {
        return executors;
    }
    
    /**
     * Returns the number of workers.
     * 
     * @return the number of workers
     */
    public int getWorkers() {
        return workers;
    }

    @Override
    public int hashCode() {
        return tasks ^ executors ^ workers;
    }

    /**
     * Defines the projection types.
     * 
     * @author Holger Eichelberger
     */
    public enum ProjectionType {
        WORKER,
        EXECUTOR,
        TASK
    }
    
    /**
     * Projects processing entries according to <code>type</code>.
     * 
     * @param type the type
     * @param processing the processing entries
     * @return the projected list
     */
    public static List<Integer> project(ProjectionType type, Collection<ProcessingEntry> processing) {
        List<Integer> result = new ArrayList<Integer>();
        for (ProcessingEntry ent : processing) {
            switch (type) {
            case EXECUTOR:
                result.add(ent.getExecutors());
                break;
            case TASK:
                result.add(ent.getTasks());
                break;
            case WORKER:
                result.add(ent.getWorkers());
                break;
            default:
                break;
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "(t" + tasks + " e " + executors + " w " + workers + ")";
    }

}