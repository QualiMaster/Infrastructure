package eu.qualimaster.adaptation.internal;
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
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

import eu.qualimaster.adaptation.internal.IvmlElementIDentifier.ObservableTuple;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.systemState.TypeMapper;
import eu.qualimaster.monitoring.systemState.TypeMapper.TypeCharacterizer;
import eu.qualimaster.observables.IObservable;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.AbstractIvmlVariable;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.IvmlElement;
import net.ssehub.easy.instantiation.rt.core.model.confModel.AbstractVariableIdentifier;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;

/**
 * Uses {@link IvmlElement}s and {@link IObservable}s to identify temporary value mappings
 * inside the {@link net.ssehub.easy.adaptiveVarModel.confModel.AdaptiveConfiguration}.
 * @author El-Sharkawy
 */
public class IvmlElementIDentifier extends AbstractVariableIdentifier<ObservableTuple> {
    
    /**
     * A 2-tuple consisting of {@link IvmlElement} and {@link IObservable}, which are used to genrate unique
     * identifiers.
     * @author El-Sharkawy
     *
     */
    public static class ObservableTuple {
        
        private IvmlElement element;
        private IObservable observable;

        /**
         * Sole constructor of this class.
         * @param element The (top level) variable to map.
         * @param observable An observable nested inside of <tt>element</tt>
         */
        public ObservableTuple(IvmlElement element, IObservable observable) {
            this.element = element;
            this.observable = observable;
        }
    }

    @Override
    protected String variableToID(ObservableTuple variable) {
        String id = null;
        if (variable.element instanceof AbstractIvmlVariable) {
            AbstractIvmlVariable var = (AbstractIvmlVariable) variable.element;
            TypeCharacterizer characterizer = TypeMapper.findCharacterizer(var.getIvmlType());
            if (null != characterizer) {
                String prefix = characterizer.getFrozenStatePrefix();
                String key = characterizer.getFrozenStateKey(var.getDecisionVariable());
                
                id = prefix + FrozenSystemState.SEPARATOR + key + FrozenSystemState.SEPARATOR
                    + (null == variable.observable ? null : variable.observable.name());
            }
        }
        
        return id;
    }

    @Override
    protected boolean isNestedVariable(String id) {
        return null != id && StringUtils.countMatches(id, FrozenSystemState.SEPARATOR) > 1;
    }

    @Override
    protected Iterator<String> getIDIterator(final String id) {
        return new Iterator<String>() {
            private String[] segments = id.split(FrozenSystemState.SEPARATOR);
            private int index = 1;

            @Override
            public boolean hasNext() {
                return segments.length > index; 
            }

            @Override
            public String next() {
                String id;
                if (1 == index) {
                    id = segments[0] + FrozenSystemState.SEPARATOR + segments[1];
                    index++;
                } else {
                    id = segments[index++];
                }
                
                return id;
            }
            
            @Override
            public void remove() {
                throw new UnsupportedOperationException("Removing segments are not supported. Tried this on: " + id);
            }
        };
    }

    @Override
    protected String iDecisionVariableToID(IDecisionVariable variable) {
        String id = null;
        TypeCharacterizer characterizer = TypeMapper.findCharacterizer(variable.getDeclaration().getType());
        if (null != characterizer) {
            String prefix = characterizer.getFrozenStatePrefix();
            String key = characterizer.getFrozenStateKey(variable);
            
            id = prefix + FrozenSystemState.SEPARATOR + key;
        }
        
        return id;
    }
}
