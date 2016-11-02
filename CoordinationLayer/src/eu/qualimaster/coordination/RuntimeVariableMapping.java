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
package eu.qualimaster.coordination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ssehub.easy.instantiation.rt.core.model.rtVil.VariableValueCopier;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;

/**
 * Collects the references of (runtime) variables, i.e., for a given
 * variable, who is referencing this. Instances of this class are 
 * created by {@link CopyMappingCreationListener} applied to {@link VariableValueCopier}.
 * 
 * @author Holger Eichelberger
 */
public class RuntimeVariableMapping {
    
    private Map<IDecisionVariable, IDecisionVariable> mapping = new HashMap<IDecisionVariable, IDecisionVariable>();
    private Map<IDecisionVariable, List<IDecisionVariable>> reverseMapping
        = new HashMap<IDecisionVariable, List<IDecisionVariable>>();
    
    /**
     * Creates an instance.
     */
    public RuntimeVariableMapping() {
    }
    
    /**
     * Returns the referencing variable.
     * 
     * @param variable the variable to look for
     * @return the referencing variable (<b>null</b> if there is none)
     */
    public IDecisionVariable getReferencedBy(IDecisionVariable variable) {
        return null == variable ? null : mapping.get(variable);
    }
    
    /**
     * Reverse function to {@link #getReferencedBy(IDecisionVariable)}.
     * 
     * @param originalVar the holding, parent variable to look for
     * @return the mapped(runtime/adaptation) variables belonging to the given parent (<b>null</b> if there is none)
     */
    public List<IDecisionVariable> getMappedVariables(IDecisionVariable originalVar) {
        return null == originalVar ? null : reverseMapping.get(originalVar);
    }
    
    /**
     * Records that <code>referenced</code> is referenced by <code>origin</code>.
     * 
     * @param referenced the referenced variable
     * @param origin the referencing variable
     */
    public void addReferencedBy(IDecisionVariable referenced, IDecisionVariable origin) {
        mapping.put(referenced, origin);
        List<IDecisionVariable> mappedChilds = reverseMapping.get(origin);
        if (null == mappedChilds) {
            mappedChilds = new ArrayList<IDecisionVariable>();
            reverseMapping.put(origin, mappedChilds);
        }
        mappedChilds.add(referenced);
    }
    
    @Override
    public String toString() {
        return mapping.toString();
    }

}
