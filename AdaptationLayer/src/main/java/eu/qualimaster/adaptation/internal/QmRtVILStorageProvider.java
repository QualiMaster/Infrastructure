/*
 * Copyright 2009-2014 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.adaptation.internal;

import net.ssehub.easy.instantiation.rt.core.model.rtVil.RtVilStorage;

/**
 * Implements the Storage Provider for rt-VIL. Needs to be connected to the data management layer. Change
 * registration in {@link eu.qualimaster.adaptation.AdaptationManager#start()}.
 * 
 * @author Holger Eichelberger
 */
public class QmRtVILStorageProvider extends RtVilStorage {

    @Override
    public Object getValue(String script, String variable) {
        return null; // TODO use Data Management Layer here
    }

    @Override
    public void setValue(String script, String variable, Object value) {
        // TODO use Data Management Layer here
    }

}
