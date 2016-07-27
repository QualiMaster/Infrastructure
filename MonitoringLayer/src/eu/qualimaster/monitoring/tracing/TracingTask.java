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
package eu.qualimaster.monitoring.tracing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;

import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.monitoring.AbstractMonitoringTask;
import eu.qualimaster.monitoring.AbstractMonitoringTask.IPiggybackTask;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.systemState.AlgorithmParameter;
import eu.qualimaster.monitoring.systemState.SystemState;
import net.ssehub.easy.varModel.confModel.CompoundVariable;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.ContainerVariable;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.AbstractVariable;
import net.ssehub.easy.varModel.model.ModelQuery;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.Project;
import net.ssehub.easy.varModel.model.values.BooleanValue;
import net.ssehub.easy.varModel.model.values.CompoundValue;
import net.ssehub.easy.varModel.model.values.ConstraintValue;
import net.ssehub.easy.varModel.model.values.ContainerValue;
import net.ssehub.easy.varModel.model.values.EnumValue;
import net.ssehub.easy.varModel.model.values.IValueVisitor;
import net.ssehub.easy.varModel.model.values.IntValue;
import net.ssehub.easy.varModel.model.values.MetaTypeValue;
import net.ssehub.easy.varModel.model.values.NullValue;
import net.ssehub.easy.varModel.model.values.RealValue;
import net.ssehub.easy.varModel.model.values.ReferenceValue;
import net.ssehub.easy.varModel.model.values.StringValue;
import net.ssehub.easy.varModel.model.values.Value;
import net.ssehub.easy.varModel.model.values.VersionValue;

/**
 * Implements a piggyback (attachable) tracing task. As different sources may request pipeline tracing but there
 * shall be at most one tracing task, this class provides a usage counter.
 * 
 * @author Holger Eichelberger
 */
public class TracingTask implements IPiggybackTask {

    private ValueVisitor valueVisitor = new ValueVisitor();
    private int usageCount = 0;
    private AbstractMonitoringTask parent;
    
    /**
     * Creates the tracing task.
     * 
     * @param parent the parent task this task is a piggyback of
     */
    public TracingTask(AbstractMonitoringTask parent) {
        this.parent = parent;
        incrementUsageCount();
    }
    
    @Override
    public void run() {
        SystemState state = MonitoringManager.getSystemState();
        ParameterProvider parameters = new ParameterProvider();
        Tracing.traceAlgorithms(state, parameters);
        Tracing.traceInfrastructure(state, parameters);
    }

    @Override
    public void stop() {
    }

    /**
     * Implements a deferring and caching parameter provider.
     * 
     * @author Holger Eichelberger
     */
    private class ParameterProvider implements IParameterProvider {

        private Configuration config;
        private AbstractVariable algorithms;
        private Map<String, List<AlgorithmParameter>> parameter;

        /**
         * Creates a parameter provider.
         */
        ParameterProvider() {
        }
        
        @Override
        public Map<String, List<AlgorithmParameter>> getAlgorithmParameters() {
            if (null == parameter) {
                Models models = RepositoryConnector.getModels(Phase.MONITORING);
                if (null == config && null != models) {
                    models.startUsing();
                    config = models.getConfiguration();
                    if (null != config) {
                        Project prj = config.getProject();
                        try {
                            // ok as long as the model is not changing - then clear variables...
                            algorithms = ModelQuery.findVariable(prj, "algorithms", null); 
                        } catch (ModelQueryException e) {
                            LogManager.getLogger(Tracing.class).error("cannot find 'algorithms'", e);
                        }
                    }
                    models.endUsing();
                }
                if (null != config && null != algorithms) {
                    parameter = collectAlgorithmParameters(config, algorithms);
                }
            }
            return parameter;
        }
        
    }

    
    /**
     * Collects the parameters of all algorithms in <code>config</code>.
     * 
     * @param config the configuration to take into account
     * @param algorithms the algorithms variable in <code>config</code>
     * @return the algorithm-parameter mapping
     */
    private Map<String, List<AlgorithmParameter>> collectAlgorithmParameters(Configuration config, 
        AbstractVariable algorithms) {
        Map<String, List<AlgorithmParameter>> result = new HashMap<String, List<AlgorithmParameter>>();
        IDecisionVariable var = config.getDecision(algorithms);
        if (var instanceof ContainerVariable) {
            ContainerVariable cVar = (ContainerVariable) var;
            for (int n = 0; n < cVar.getNestedElementsCount(); n++) {
                IDecisionVariable elt = var.getNestedElement(n);
                if (elt instanceof CompoundVariable) {
                    CompoundVariable comp = (CompoundVariable) elt;
                    String name = toString(comp.getNestedVariable("name"));
                    if (null != name) {
                        List<AlgorithmParameter> param = collectAlgorithmParameters(
                            comp.getNestedVariable("parameters"));
                        if (null != param) {
                            result.put(name, param);
                        }
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Collects the algorithm parameters for <code>var</code> representing an algorithm.
     * 
     * @param var the variable
     * @return the parameters of <code>var</code>, may be <b>null</b> if there are none
     */
    private List<AlgorithmParameter> collectAlgorithmParameters(IDecisionVariable var) {
        List<AlgorithmParameter> result = null;
        if (var instanceof ContainerVariable) {
            ContainerVariable cVar = (ContainerVariable) var;
            result = new ArrayList<AlgorithmParameter>();
            for (int n = 0; n < cVar.getNestedElementsCount(); n++) {
                IDecisionVariable elt = var.getNestedElement(n);
                if (elt instanceof CompoundVariable) {
                    CompoundVariable comp = (CompoundVariable) elt;
                    String name = toString(comp.getNestedVariable("name"));
                    if (null != name) {
                        result.add(new AlgorithmParameter(name, toString(comp.getNestedVariable("value"))));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Turns the value of <code>var</code> into a string.
     * 
     * @param var the variable
     * @return the value as string, empty if <code>var</code> is null
     */
    private String toString(IDecisionVariable var) {
        String result;
        if (null == var) {
            result = "";
        } else {
            result = toString(var.getValue());
        }
        return result;
    }

    /**
     * Turns the <code>value</code> into a string.
     * 
     * @param value the value
     * @return the value as string, empty if <code>value</code> is null
     */
    private String toString(Value value) {
        String result;
        if (null == value) {
            result = "";
        } else {
            value.accept(valueVisitor);
            result = valueVisitor.getValue();
        }
        return result;
    }

    /**
     * A visitor for turning (simple) values into strings.
     * 
     * @author Holger Eichelberger
     */
    private class ValueVisitor implements IValueVisitor {

        private String result;

        /**
         * Returns the actual value.
         * 
         * @return the value
         */
        public String getValue() {
            return result;
        }
        
        @Override
        public void visitConstraintValue(ConstraintValue value) {
            result = "";
        }

        @Override
        public void visitEnumValue(EnumValue value) {
            result = value.getValue().getName();
        }

        @Override
        public void visitStringValue(StringValue value) {
            result = value.getValue();
        }

        @Override
        public void visitCompoundValue(CompoundValue value) {
            result = "";
        }

        @Override
        public void visitContainerValue(ContainerValue value) {
            result = "";
        }

        @Override
        public void visitIntValue(IntValue value) {
            result = String.valueOf(value.getValue());
        }

        @Override
        public void visitRealValue(RealValue value) {
            result = String.valueOf(value.getValue());
        }

        @Override
        public void visitBooleanValue(BooleanValue value) {
            result = String.valueOf(value.getValue());
        }

        @Override
        public void visitReferenceValue(ReferenceValue referenceValue) {
            result = "";
        }

        @Override
        public void visitMetaTypeValue(MetaTypeValue value) {
            result = "";
        }

        @Override
        public void visitNullValue(NullValue value) {
            result = "";
        }

        @Override
        public void visitVersionValue(VersionValue value) {
            result = value.toString();
        }
        
    }
    
    /**
     * Increments the usage count.
     */
    public void incrementUsageCount() {
        usageCount++;
    }

    /**
     * Decrements the usage count.
     */
    public void decrementUsageCount() {
        if (usageCount > 0) {
            usageCount--;
        }
    }
    
    /**
     * Returns the number of instances using this instance.
     * 
     * @return the number of using instances
     */
    public int getUsageCount() {
        return usageCount;
    }
    
    /**
     * Returns the parent task.
     * 
     * @return the parent task
     */
    public AbstractMonitoringTask getParent() {
        return parent;
    }

}
