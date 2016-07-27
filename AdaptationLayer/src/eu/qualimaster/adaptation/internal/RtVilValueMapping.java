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
package eu.qualimaster.adaptation.internal;

import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.systemState.TypeMapper;
import eu.qualimaster.monitoring.systemState.TypeMapper.TypeCharacterizer;
import eu.qualimaster.observables.AnalysisObservables;
import eu.qualimaster.observables.IObservable;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.AbstractIvmlVariable;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.IVariableValueMapper;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.IvmlElement;

/**
 * Maps a frozen system state to rtVIL. Due to reasoning it is currently only possible to map isEnacting
 * and isValid here.
 * 
 * @author Holger Eichelberger
 */
public class RtVilValueMapping implements IVariableValueMapper {

    private FrozenSystemState state = new FrozenSystemState();

    /**
     * Defines a new system state as basis for the mapping.
     * 
     * @param state the new state (<b>null</b> clears the state)
     */
    public void setSystemState(FrozenSystemState state) {
        this.state = state;
    }
    
    @Override
    public Object getValue(IvmlElement elt) {
        return null;
    }

    @Override
    public boolean isEnacting(IvmlElement elt) {
        return toBoolean(getValue(elt, AnalysisObservables.IS_ENACTING), false);
    }

    @Override
    public boolean isValid(IvmlElement elt) {
        return toBoolean(getValue(elt, AnalysisObservables.IS_VALID), true);
    }
    
    /**
     * Returns the value of an observable in <code>elt</code>.
     * 
     * @param elt the IVML element to return the value for
     * @param observable the observable
     * @return the value, may be <b>null</b> if not defined
     */
    private Double getValue(IvmlElement elt, IObservable observable) {
        Double result = null;
        if (null != state && elt instanceof AbstractIvmlVariable) {
            AbstractIvmlVariable var = (AbstractIvmlVariable) elt;
            TypeCharacterizer characterizer = TypeMapper.findCharacterizer(var.getIvmlType());
            if (null != characterizer) {
                String prefix = characterizer.getFrozenStatePrefix();
                String key = characterizer.getFrozenStateKey(var.getDecisionVariable());
                if (null != prefix && null != key) {
                    result = state.getObservation(prefix, key, observable, null);
                }
            }
        }
        return result;
    }
    
    /**
     * Turns a value into a boolean.
     * 
     * @param value the value
     * @param deflt the default value if <code>value</code> is <b>null</b>
     * @return the boolean value
     */
    private static boolean toBoolean(Double value, boolean deflt) {
        boolean result;
        if (null == value) {
            result = deflt;
        } else {
            result = value >= 0.5;
        }
        return result;
    }

}
