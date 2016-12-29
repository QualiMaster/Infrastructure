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
    private static final Map<String, IObservable> TYPENAME_OBSERVABLE_MAPPING = new HashMap<String, IObservable>();

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
            NAME_OBSERVABLE_MAPPING.put(getMappedName(obs), obs);
            TYPENAME_OBSERVABLE_MAPPING.put(getMappedTypeName(obs), obs);
        }
    }

    /**
     * Returns the mapped (variable) name.
     * 
     * @param obs the observable
     * @return the mapped (variable) name
     */
    public static String getMappedName(IObservable obs) {
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
        return tmpName;
    }
    
    /**
     * Returns the mapped type name.
     * 
     * @param obs the observable
     * @return the mapped type name
     */
    public static String getMappedTypeName(IObservable obs) {
        StringBuilder name = new StringBuilder(obs.name());
        int i = 0; 
        boolean lastUnderscore = false;
        while (i < name.length()) {
            char c = name.charAt(i);
            if ('_' == c) {
                lastUnderscore = true;
            } else {
                char newChar;
                if (0 == i || lastUnderscore) {
                    newChar = Character.toUpperCase(c);
                } else {
                    newChar = Character.toLowerCase(c);
                }
                name.setCharAt(i, newChar);
                lastUnderscore = false;
            }
            i++;
        }
        return name.toString();
    }

    /**
     * Maps an IVML type name to an observable.
     * 
     * @param ivmlTypeName the IVML type name
     * @return the observable or <b>null</b> if no mapping is possible
     */
    public static final IObservable getObservableByType(String ivmlTypeName) {
        return TYPENAME_OBSERVABLE_MAPPING.get(ivmlTypeName);
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

    /**
     * Returns the full name mapping.
     * 
     * @return the full name mapping (copy)
     */
    public static Map<String, IObservable> getNameMapping() {
        Map<String, IObservable> result = new HashMap<String, IObservable>();
        result.putAll(NAME_OBSERVABLE_MAPPING);
        return result;
    }

    /**
     * Returns the full type name mapping.
     * 
     * @return the full type name mapping (copy)
     */
    public static Map<String, IObservable> getTypeNameMapping() {
        Map<String, IObservable> result = new HashMap<String, IObservable>();
        result.putAll(TYPENAME_OBSERVABLE_MAPPING);
        return result;
    }

    /**
     * Returns the full reverse name mapping.
     * 
     * @return the full reverse name mapping (copy)
     */
    public static Map<IObservable, String> getReverseNameMapping() {
        Map<IObservable, String> result = new HashMap<IObservable, String>();
        for (Map.Entry<String, IObservable> ent : NAME_OBSERVABLE_MAPPING.entrySet()) {
            result.put(ent.getValue(), ent.getKey());
        }
        return result;
    }

    /**
     * Returns the full reverse type name mapping.
     * 
     * @return the full reverse type name mapping (copy)
     */
    public static Map<IObservable, String> getReverseTypeNameMapping() {
        Map<IObservable, String> result = new HashMap<IObservable, String>();
        for (Map.Entry<String, IObservable> ent : TYPENAME_OBSERVABLE_MAPPING.entrySet()) {
            result.put(ent.getValue(), ent.getKey());
        }
        return result;
    }

}
