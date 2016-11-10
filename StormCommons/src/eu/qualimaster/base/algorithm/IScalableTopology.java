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
package eu.qualimaster.base.algorithm;

import eu.qualimaster.infrastructure.IScalingDescriptor;

/**
 * Indicates a scalable topology. Separate interface due to evolution. This is intended for manual implementation
 * of {@link ITopologyCreate} onl.y
 * 
 * @author Holger Eichelberger
 */
public interface IScalableTopology {
    
    /**
     * Returns the scaling descriptor for this scalable topology.
     * 
     * @return the scaling descriptor (may be <b>null</b> if no scaling will happen)
     */
    public IScalingDescriptor getScalingDescriptor();
    
}
