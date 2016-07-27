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
package eu.qualimaster.monitoring.tracing;

import java.util.List;
import java.util.Map;

import eu.qualimaster.monitoring.systemState.AlgorithmParameter;

/**
 * Defines the interface for a parameter provider (parameters come from the configuration).
 * 
 * @author Holger Eichelberger
 */
public interface IParameterProvider {

    /**
     * Returns the actual algorithm parameters.
     * 
     * @return the parameters (may be <b>null</b> for no parameters)
     */
    public Map<String, List<AlgorithmParameter>> getAlgorithmParameters();
}