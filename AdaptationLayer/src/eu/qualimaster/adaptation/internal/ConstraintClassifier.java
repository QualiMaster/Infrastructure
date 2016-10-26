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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.easy.extension.QmConstants;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.cst.AbstractConstraintTreeVisitor;
import net.ssehub.easy.varModel.cst.IfThen;
import net.ssehub.easy.varModel.cst.OCLFeatureCall;
import net.ssehub.easy.varModel.cst.Variable;
import net.ssehub.easy.varModel.model.AbstractVariable;
import net.ssehub.easy.varModel.model.datatypes.BooleanType;
import net.ssehub.easy.varModel.model.values.EnumValue;
import net.ssehub.easy.varModel.model.values.Value;

/**
 * Classifies relevant constraint that indicate failing of reasoning in adaptation.
 * Pass in the problem variables from reasoning (as we do not evaluate not all variables are required), let
 * the classifier visit the constraint in question, ask for {@link #isRuntimeEnact()} or the involved 
 * {@link #collectRuntimeVariables(List)}. For reuse, call {@link #clear()}. 
 * 
 * @author Holger Eichelberger
 */
class ConstraintClassifier extends AbstractConstraintTreeVisitor {
    
    private Map<AbstractVariable, IDecisionVariable> variables = new HashMap<>();
    private Set<IDecisionVariable> found = new HashSet<>();

    /**
     * Creates an empty constraint classifier. Call {@link #setVariables(Set)} next.
     */
    ConstraintClassifier() {
    }
    
    /**
     * Creates a classifier with initial variables.
     * 
     * @param variables the problem variables
     */
    ConstraintClassifier(Set<IDecisionVariable> variables) {
        setVariables(variables);
    }
    
    /**
     * Sets the variables.
     * 
     * @param variables the problem variables
     */
    public void setVariables(Set<IDecisionVariable> variables) {
        for (IDecisionVariable v : variables) {
            if (isRuntimeEnact(v)) {
                this.variables.put(v.getDeclaration(), v);
            }
        }
    }
    
    /**
     * Clears this classifier for reuse.
     */
    void clear() {
        variables.clear();
        found.clear();
    }
    
    /**
     * Returns whether the constraint is relevant for runtime enactment.
     * 
     * @return <code>true</code> if relevant, <code>false</code> else
     */
    boolean isRuntimeEnact() {
        return !found.isEmpty();
    }
    
    /**
     * Returns the name of the relevant runtime variables.
     * 
     * @param result the list of variables to be modified
     */
    void collectRuntimeVariables(Collection<IDecisionVariable> result) {
        result.addAll(found);
    }
    
    @Override
    public void visitVariable(Variable var) {
        AbstractVariable aVar = var.getVariable();
        if (null != aVar) {
            IDecisionVariable decVar = variables.get(aVar);
            if (null != decVar) { // if found (already classified as runtime enact)
                found.add(decVar);
            }
        }
    }
    
    /**
     * Returns whether the given variable has runtime enact as binding time.
     * 
     * @param var the variable to test
     * @return <code>true</code> for runtime-enact, <code>false</code> else
     */
    private boolean isRuntimeEnact(IDecisionVariable var) {
        boolean result = false;
        for (int a = 0; a < var.getAttributesCount(); a++) {
            IDecisionVariable attribute = var.getAttribute(a);
            AbstractVariable decl = attribute.getDeclaration();
            if (QmConstants.ANNOTATION_BINDING_TIME.equals(decl.getName())) {
                Value val = attribute.getValue();
                if (val instanceof EnumValue) {
                    EnumValue eVal = (EnumValue) val;
                    if (eVal.getValue().getName().equals(QmConstants.CONST_BINDING_TIME_RUNTIME_ENACT)) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void visitOclFeatureCall(OCLFeatureCall call) {
        if (call.getResolvedOperation() == BooleanType.IMPLIES) {
            // exclude the condition
            for (int p = 0; p < call.getParameterCount(); p++) {
                call.getParameter(p).accept(this);
            }
        } else {
            super.visitOclFeatureCall(call);
        }
    }

    @Override
    public void visitIfThen(IfThen ifThen) {
        // exclude the condition
        ifThen.getThenExpr().accept(this);
        if (null != ifThen.getElseExpr()) {
            ifThen.getElseExpr().accept(this);
        }
    }
    
}
