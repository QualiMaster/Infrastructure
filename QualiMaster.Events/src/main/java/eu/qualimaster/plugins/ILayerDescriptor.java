/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.plugins;

/**
 * Describes an infrastructure layer and, thus, a point in time when to start registered plugins. Instances are assumed 
 * to be singletons.
 * 
 * @author Holger Eichelberger
 */
public interface ILayerDescriptor {

    /**
     * The name of the layer.
     * 
     * @return the name (shall not contain spaces)
     */
    public String name();
    
}
