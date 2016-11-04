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
package eu.qualimaster.monitoring;

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Observables;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.AbstractVariable;

/**
 * Maps IVML names to observables according to a fixed naming scheme. The issue behind this mapper is that
 * IVML cannot be extended in types and observables are used as IVML variable instances. At some point, we made
 * the decision to keep just one name in the configuration model, not both names.
 * 
 * @author Holger Eichelberger
 */
public class ObservableMapper {

    private static final Map<String, IObservable> NAME_OBSERVABLE_MAPPING = new HashMap<String, IObservable>();

    static {
        registerAll(Observables.OBSERVABLES);
    }
    
    /**
     * Registers all observables according to the variable name convention, i.e., 
     * turns observable names in Java-like variable names. "Cpus" becomes "CPUs" and
     * "Dfes" becomes "DFEs".
     * 
     * @param observables the observables to be registered
     */
    private static final void registerAll(IObservable[] observables) {
        for (IObservable obs : observables) {
            StringBuilder name = new StringBuilder(obs.name());
            int i = 0; 
            boolean lastUnderscore = false;
            while (i < name.length()) {
                char c = name.charAt(i);
                if ('_' == c) {
                    name.deleteCharAt(i);
                    lastUnderscore = true;
                } else {
                    char newChar;
                    if (lastUnderscore) {
                        newChar = Character.toUpperCase(c);
                    } else {
                        newChar = Character.toLowerCase(c);
                    }
                    name.setCharAt(i, newChar);
                    lastUnderscore = false;
                    i++;
                }
            }
            String tmpName = name.toString();
            tmpName = tmpName.replace("Cpus", "CPUs");
            tmpName = tmpName.replace("Dfes", "DFEs");
            NAME_OBSERVABLE_MAPPING.put(tmpName, obs);
        }
    }

    /**
     * Maps an IVML name to an observable.
     * 
     * @param ivmlName the IVML name
     * @return the observable or <b>null</b> if no mapping is possible
     */
    public static final IObservable getObservable(String ivmlName) {
        return NAME_OBSERVABLE_MAPPING.get(ivmlName);
    }

    /**
     * Maps a variable declaration to an observable.
     * 
     * @param var the variable declaration
     * @return the observable or <b>null</b> if no mapping is possible
     */
    public static final IObservable getObservable(AbstractVariable var) {
        return getObservable(var.getName());
    }

    /**
     * Maps a decision variable to an observable.
     * 
     * @param var the variable
     * @return the observable or <b>null</b> if no mapping is possible
     */
    public static final IObservable getObservable(IDecisionVariable var) {
        return getObservable(var.getDeclaration());
    }

}
