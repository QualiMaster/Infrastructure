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
package eu.qualimaster.adaptation.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;

import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.Configuration;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.IReasoningHook;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.IRtValueAccess;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.IRtVilConcept;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.ReasoningHookAdapter;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Script;
import net.ssehub.easy.basics.messages.Status;
import net.ssehub.easy.reasoning.core.reasoner.Message;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.cst.ConstraintSyntaxTree;
import net.ssehub.easy.varModel.model.Constraint;

/**
 * Defines a reasoning hook, which filters out all irrelevant messages on adaptation level.
 * 
 * @author Holger Eichelberger
 */
public class ReasoningHook extends ReasoningHookAdapter {

    public static final IReasoningHook INSTANCE = new ReasoningHook();

    /**
     * Prevents external instantiation.
     */
    private ReasoningHook() {
    }
    
    @Override
    public Status analyze(Script script, IRtVilConcept concept, IRtValueAccess values, Message message) {
        Set<IDecisionVariable> found = new HashSet<IDecisionVariable>();
        ConstraintClassifier classifier = new ConstraintClassifier();
        List<Constraint> probConstraints = message.getProblemConstraints();
        List<Set<IDecisionVariable>> probVars = message.getProblemVariables();
        int count = Math.min(probConstraints.size(), probVars.size()); // shall be the same, but in any case
        for (int i = 0; i < count; i++) {
            Constraint c = probConstraints.get(i);
            ConstraintSyntaxTree cst = c.getConsSyntax();
            if (null != cst) {
                classifier.setVariables(probVars.get(i));
                cst.accept(classifier);
                classifier.collectRuntimeVariables(found);
                classifier.clear();
            }
        }
        
        Status result;
        if (!found.isEmpty()) {
            result = message.getStatus(); // pass only if relevant variable found
            String tmp = "";
            for (IDecisionVariable dv : found) {
                if (tmp.length() > 0) {
                    tmp += ",";
                }
                tmp += dv.getQualifiedName();
            }
            LogManager.getLogger(ReasoningHook.class).error("Failing reasoning message " + toText(message) 
                + " due to " + tmp);            
        } else {
            LogManager.getLogger(ReasoningHook.class).warn("Skipped reasoning message: " + toText(message));
            result = null; // ignore
        }
        return result;
    }

    /**
     * Is called if reasoning is considered to fail, e.g., to inform the user.
     * 
     * @param config the configuration
     */
    @Override
    public void reasoningFailed(Configuration config) {
        net.ssehub.easy.varModel.confModel.Configuration.printConfig(System.out, config.getConfiguration());
    }

}
