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

import java.util.Stack;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationCommandNotifier;
import eu.qualimaster.coordination.commands.CoordinationCommandNotifier.ICoordinationCommandNotifier;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import net.ssehub.easy.instantiation.core.model.buildlangModel.ITracer;
import net.ssehub.easy.instantiation.core.model.buildlangModel.Rule;
import net.ssehub.easy.instantiation.core.model.buildlangModel.RuleExecutionResult;
import net.ssehub.easy.instantiation.core.model.buildlangModel.Script;
import net.ssehub.easy.instantiation.core.model.common.RuntimeEnvironment;
import net.ssehub.easy.instantiation.core.model.common.VilException;
import net.ssehub.easy.instantiation.core.model.expressions.Expression;
import net.ssehub.easy.instantiation.core.model.vilTypes.Map;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.DelegatingTracer;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Strategy;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Tactic;

/**
 * A delegating log tracer informing a given {@link IAdaptationLogger} about ongoing execution. Registers and 
 * unregisters itself as a {@link ICoordinationCommandNotifier} while being notified about the execution of the 
 * main rt-VIL script.
 * 
 * @author Holger Eichelberger
 */
public class DelegatingLogTracer extends DelegatingTracer implements ICoordinationCommandNotifier {

    private IAdaptationLogger logger;
    private boolean expressionFailed = false;
    private Integer lastFailCode;
    private String lastFailReason;
    private Stack<Script> scripts = new Stack<Script>();
    
    /**
     * Creates a delegating log tracer.
     * 
     * @param delegate the tracer to delegate further logging to
     * @param logger the logger (may be <b>null</b>, then an instace of {@link AdaptationLoggerAdapter} is used
     */
    public DelegatingLogTracer(ITracer delegate, IAdaptationLogger logger) {
        super(delegate);
        this.logger = null == logger ? new AdaptationLoggerAdapter() : logger;
    }
    
    @Override
    public void visitScript(Script script, RuntimeEnvironment<?, ?> environment) {
        scripts.push(script);
        super.visitScript(script, environment);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void visitScriptBody(Script script, RuntimeEnvironment<?, ?> environment) {
        super.visitScriptBody(script, environment);
        if (isMainVil()) { // inform only about main script, not imports or calls to other scripts
            CoordinationCommandNotifier.addNotifier(this);
            AdaptationEvent event = getParameterValue(script, 3, environment, AdaptationEvent.class);
            Object binding = getParameterValue(script, 4, environment, Object.class);
            java.util.Map<?, ?> map = null;
            if (binding instanceof Map) {
                map = ((Map<?, ?>) binding).toMap();
            } else if (binding instanceof java.util.Map) {
                map = (java.util.Map<?, ?>) binding;
            }
            FrozenSystemState state = null;
            if (null != map) {
                try {
                    state = new FrozenSystemState((java.util.Map<String, Double>) map);
                } catch (ClassCastException e) {
                    // just in case, then state remains null
                }
            }
            logger.startAdaptation(event, state);
        }
    }

    @Override
    public void visitedScript(Script script) {
        super.visitedScript(script);
        if (isMainVil()) { // inform only about main script, not imports or calls to other scripts
            boolean successful = expressionFailed;
            successful |= lastFailReason != null || lastFailCode != null;
            logger.endAdaptation(successful);
            CoordinationCommandNotifier.removeNotifier(this);
        }
        scripts.pop();
    }
    
    /**
     * Returns whether <code>script</code> is the main QM VIL script. As a prerequisite, {@link #scripts} must 
     * be correct.
     * 
     * @return <code>true</code> for the main script, <code>false</code> else
     */
    private boolean isMainVil() {
        return 1 == scripts.size();
    }
    
    /**
     * Returns a typed parameter value.
     * 
     * @param <T> the parameter type
     * @param script the script to return the parameter for
     * @param index the 0-based index
     * @param environment the environment holding the actual values
     * @param type the target type
     * @return the parameter value, <b>null</b> if the parameter does not exist or cannot be casted
     */
    private static <T> T getParameterValue(Script script, int index, RuntimeEnvironment<?, ?> environment, 
        Class<T> type) {
        T result = null;
        if (script.getParameterCount() >= index + 1) {
            try {
                Object obj = environment.getValue(script.getParameter(index));
                if (type.isInstance(obj)) {
                    result = type.cast(obj);
                }
            } catch (VilException e) {
                // undefined
            }
        }
        return result;
    }

    @Override
    public void failedAt(Expression expression) {
        super.failedAt(expression);
        expressionFailed = true;
    }

    @Override
    public void visitedRule(Rule rule, RuntimeEnvironment<?, ?> environment, Object result) {
        super.visitedRule(rule, environment, result);
        boolean successful = false;
        if (result instanceof RuleExecutionResult) {
            RuleExecutionResult res = (RuleExecutionResult) result;
            successful = RuleExecutionResult.Status.SUCCESS == res.getStatus();
            lastFailCode = res.getFailCode();
            lastFailReason = res.getFailReason();
        }
        if (rule instanceof Strategy) {
            logger.executedStrategy(rule.getName(), successful);
        } else if (rule instanceof Tactic) {
            logger.executedTactic(rule.getName(), successful);
        }
    }

    @Override
    public void notifySent(CoordinationCommand command) {
        logger.enacting(command);
    }

}
