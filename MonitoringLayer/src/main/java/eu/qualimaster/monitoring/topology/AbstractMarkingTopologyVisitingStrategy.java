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
package eu.qualimaster.monitoring.topology;

import java.util.HashSet;
import java.util.Set;

import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;

/**
 * Supports simple marking of (e.g., processed) nodes. We separate between marking for loops and visited for 
 * distinguishing already processed nodes.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractMarkingTopologyVisitingStrategy implements ITopologyVisitingStrategy {

    private Set<Processor> done = new HashSet<Processor>();
    private Set<Processor> visited = new HashSet<Processor>();

    /**
     * Marks <code>processor</code>.
     * 
     * @param processor the processor to be marked
     * @return <code>true</code> if <code>processor</code> was marked before, <code>false</code> else
     */
    protected boolean markAsVisited(Processor processor) {
        return !visited.add(processor);
    }
    
    /**
     * Returns whether <code>processor</code> is marked.
     * 
     * @param processor the processor to be checked
     * @return <code>true</code> if <code>processor</code> is marked, <code>false</code> else
     */
    protected boolean wasVisited(Processor processor) {
        return visited.contains(processor);
    }
    
    /**
     * Marks <code>processor</code>.
     * 
     * @param processor the processor to be marked
     * @return <code>true</code> if <code>processor</code> was marked before, <code>false</code> else
     */
    protected boolean mark(Processor processor) {
        return !done.add(processor);
    }
    
    /**
     * Returns whether <code>processor</code> is marked.
     * 
     * @param processor the processor to be checked
     * @return <code>true</code> if <code>processor</code> is marked, <code>false</code> else
     */
    protected boolean isMarked(Processor processor) {
        return done.contains(processor);
    }
    
    /**
     * Unmarks <code>processor</code>.
     * 
     * @param processor the processor to be unmarked
     */
    protected void unmark(Processor processor) {
        done.remove(processor);
    }
    
    @Override
    public void clear() {
        done.clear();
        visited.clear();
    }

}
