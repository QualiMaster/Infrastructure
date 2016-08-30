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
package backtype.storm.stateTransfer;

/**
 * Defines basic state handling strategies (for content).
 * 
 * @author Holger Eichelberger
 */
public enum StateHandlingStrategy {

    /**
     * Default strategy without specific handling of contained data.
     */
    DEFAULT,
    
    /**
     * In case of collections, clear the original one (if it exists) and fill it with the data from the transferred
     * state.
     */
    CLEAR_AND_FILL,
    
    /**
     * In case of collections, merge the new data from the transferred state into the actual state. 
     * Override existing entries.
     */
    MERGE,
    
    /**
     * In case of collections, merge the new data from the transferred state into the actual state. 
     * Keep existing entries.
     */
    MERGE_AND_KEEP_OLD
    
}
