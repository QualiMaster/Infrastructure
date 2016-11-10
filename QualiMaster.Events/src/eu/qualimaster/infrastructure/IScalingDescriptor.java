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
package eu.qualimaster.infrastructure;

import java.io.Serializable;
import java.util.Map;

/**
 * A scaling descriptor indicating how scaling a {@link IScalableTopology} will affect a manual implemented 
 * sub-topology. Instance implementing this interface must be serializable!
 * 
 * @author Holger Eichelberger
 */
public interface IScalingDescriptor extends Serializable {

    /**
     * Returns how scaling will/shall affect a sub-topology.
     * 
     * @param factor the scaling factor
     * @param executors request the scaling for executors or tasks
     * @return the distribution of executors/tasks over the sub-topology after scaling, as component name - number of 
     *     executors/tasks, return the actual distribution if no scaling is possible or factor is 0.
     */
    public Map<String, Integer> getScalingResult(double factor, boolean executors);
    
    /**
     * Returns how changing the overall number of executors will affect this sub-topology.
     * 
     * @param oldExecutors the old overall number of executors
     * @param newExecutors the new overall number of executors
     * @param diffs return the differences (<code>true</code>) or the absolute values (<code>false</code>)
     * @return the distribution of executors over the sub-topology after scaling, as component name - number of 
     *     executors, return the actual distribution if no scaling is possible or <code>executors</code> is 0.
     */
    public Map<String, Integer> getScalingResult(int oldExecutors, int newExecutors, boolean diffs);
    
}
