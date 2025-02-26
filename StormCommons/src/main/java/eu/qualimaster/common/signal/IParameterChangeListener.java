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
package eu.qualimaster.common.signal;

/**
 * A listener reacting on a parameter change.
 * 
 * @author Holger Eichelberger
 */
public interface IParameterChangeListener {
    
    /**
     * Is called when a parameter shall be changed.
     * 
     * @param signal the signal describing the parameter change
     */
    public void notifyParameterChange(ParameterChangeSignal signal);

}
