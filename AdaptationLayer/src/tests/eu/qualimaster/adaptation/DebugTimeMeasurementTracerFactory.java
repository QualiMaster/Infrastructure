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
package tests.eu.qualimaster.adaptation;

import eu.qualimaster.adaptation.internal.AdaptationLoggerFactory;
import net.ssehub.easy.instantiation.core.model.buildlangModel.ITracer;
import net.ssehub.easy.instantiation.core.model.tracing.ConsoleTracerFactory;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.AbstractIvmlVariable;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.Configuration;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.IChangeHistoryTracer;
import net.ssehub.easy.varModel.model.values.Value;

/**
 * A specific debug tracer factory with time measurement.
 * 
 * @author Holger Eichelberger
 */
public class DebugTimeMeasurementTracerFactory extends TimeMeasurementTracerFactory {
    
    /**
     * Default constructor for on-demand class loading.
     */
    public DebugTimeMeasurementTracerFactory() {
        super(new ConsoleTracerFactory(true));
    }
    
    /**
     * A debugging time delegating tracer also emitting the changes on the configuration history.
     * 
     * @author Holger Eichelberger
     */
    private class DebugTimeDelegatingTracer extends TimeDelegatingTracer implements IChangeHistoryTracer {

        /**
         * Creates the tracer.
         * 
         * @param tracer the tracer to delegate to
         */
        protected DebugTimeDelegatingTracer(ITracer tracer) {
            super(tracer);
        }
        
        /**
         * Returns the IVML instance name of <code>variable</code>.
         * 
         * @param variable the variable
         * @return the instance name
         */
        private String getInstanceName(AbstractIvmlVariable variable) {
            return net.ssehub.easy.varModel.confModel.Configuration.getInstanceName(variable.getDecisionVariable());
        }

        @Override
        public void recordedOriginalVariable(AbstractIvmlVariable variable, Value value) {
            getDelegate().trace("Change history - orig variable " + getInstanceName(variable) + " " + value);
        }

        @Override
        public void recordedChangedVariable(AbstractIvmlVariable variable, Value value) {
            getDelegate().trace("Change history - changed variable " + getInstanceName(variable) + " " + value);
        }

        @Override
        public void committing(Configuration config) {
            getDelegate().trace("Change history - committing");
        }

        @Override
        public void committed(Configuration config) {
            getDelegate().trace("Change history - committed");
        }

        @Override
        public void rollingBack(Configuration config) {
            getDelegate().trace("Change history - rolling back");
        }

        @Override
        public void rolledBack(Configuration config) {
            getDelegate().trace("Change history - rolled back");
        }

        @Override
        public void started(Configuration config) {
            getDelegate().trace("Change history - started");
        }

    }
    
    @Override
    public ITracer createBuildLanguageTracerImpl() {
        return AdaptationLoggerFactory.createTracer(
            new DebugTimeDelegatingTracer(getFactory().createBuildLanguageTracerImpl()));
    }

}
