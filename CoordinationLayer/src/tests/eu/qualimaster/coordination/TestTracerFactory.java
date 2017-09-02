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
package tests.eu.qualimaster.coordination;

import net.ssehub.easy.instantiation.core.model.buildlangModel.IBuildlangElement;
import net.ssehub.easy.instantiation.core.model.buildlangModel.IEnumeratingLoop;
import net.ssehub.easy.instantiation.core.model.buildlangModel.Rule;
import net.ssehub.easy.instantiation.core.model.buildlangModel.Script;
import net.ssehub.easy.instantiation.core.model.common.RuntimeEnvironment;
import net.ssehub.easy.instantiation.core.model.common.VariableDeclaration;
import net.ssehub.easy.instantiation.core.model.common.VilException;
import net.ssehub.easy.instantiation.core.model.execution.IInstantiatorTracer;
import net.ssehub.easy.instantiation.core.model.execution.TracerFactory;
import net.ssehub.easy.instantiation.core.model.expressions.AbstractTracerBase;
import net.ssehub.easy.instantiation.core.model.expressions.CallExpression.CallType;
import net.ssehub.easy.instantiation.core.model.expressions.Expression;
import net.ssehub.easy.instantiation.core.model.templateModel.Def;
import net.ssehub.easy.instantiation.core.model.templateModel.ITemplateLangElement;
import net.ssehub.easy.instantiation.core.model.templateModel.Template;
import net.ssehub.easy.instantiation.core.model.vilTypes.Collection;
import net.ssehub.easy.instantiation.core.model.vilTypes.FieldDescriptor;
import net.ssehub.easy.instantiation.core.model.vilTypes.OperationDescriptor;

/**
 * Just a simple tracer for testing.
 * 
 * @author Holger Eichelberger
 */
public class TestTracerFactory extends TracerFactory {

    private static Tracer tracer = new Tracer();
    
    @Override
    public net.ssehub.easy.instantiation.core.model.templateModel.ITracer 
        createTemplateLanguageTracerImpl() {
        return tracer;
    }

    @Override
    public net.ssehub.easy.instantiation.core.model.buildlangModel.ITracer 
        createBuildLanguageTracerImpl() {
        return tracer;
    }

    @Override
    public IInstantiatorTracer createInstantiatorTracerImpl() {
        return tracer;
    }
    
    /**
     * Implements the actual tracer.
     * 
     * @author Holger Eichelberger
     */
    private static class Tracer extends AbstractTracerBase 
        implements net.ssehub.easy.instantiation.core.model.templateModel.ITracer,
        net.ssehub.easy.instantiation.core.model.buildlangModel.ITracer, IInstantiatorTracer {

        @Override
        public void trace(String arg0) {
        }

        @Override
        public void traceExecutionException(VilException arg0) {
        }

        @Override
        public void valueDefined(VariableDeclaration arg0, FieldDescriptor arg1, Object arg2) {
        }

        @Override
        public void failedAt(Expression arg0) {
        }

        @Override
        public void visitedCallExpression(OperationDescriptor arg0, CallType arg1, Object[] arg2, Object arg3) {
        }

        @Override
        public void visitingCallExpression(OperationDescriptor arg0, CallType arg1, Object[] arg2) {
        }

        @Override
        public void traceError(String arg0) {
        }

        @Override
        public void traceMessage(String arg0) {
        }

        @Override
        public Collection<Object> adjustSequenceForJoin(Collection<Object> arg0) {
            return arg0;
        }

        @Override
        public Collection<?> adjustSequenceForMap(Collection<?> arg0) {
            return arg0;
        }

        @Override
        public void failedAt(IBuildlangElement arg0) {
        }

        @Override
        public void reset() {
        }

        @Override
        public void visitRule(Rule arg0, RuntimeEnvironment<?, ?> arg1) {
            System.out.println("executing " + arg0.getName());
        }

        @Override
        public void visitScript(Script arg0, RuntimeEnvironment<?, ?> env) {
            System.out.println("executing " + arg0.getName());
        }

        @Override
        public void visitSystemCall(String[] arg0) {
        }

        @Override
        public void visitedInstantiator(String arg0, Object arg1) {
        }

        @Override
        public void visitedRule(Rule arg0, RuntimeEnvironment<?, ?> arg1, Object arg2) {
        }

        @Override
        public void visitedScript(Script arg0) {
        }

        @Override
        public void visitingInstantiator(String arg0) {
        }

        @Override
        public void failedAt(ITemplateLangElement arg0) {
        }

        @Override
        public void visitAlternative(boolean arg0) {
        }

        @Override
        public void visitDef(Def arg0, RuntimeEnvironment<?, ?> arg1) {
        }

        @Override
        public void visitLoop(VariableDeclaration arg0) {
        }

        @Override
        public void visitTemplate(Template arg0) {
            System.out.println("executing " + arg0.getName());
        }

        @Override
        public void visitedDef(Def arg0, RuntimeEnvironment<?, ?> arg1, Object arg2) {
        }

        @Override
        public void visitedLoop(VariableDeclaration arg0) {
        }

        @Override
        public void visitedSwitch(Object arg0, int arg1, Object arg2) {
        }

        @Override
        public void visitedTemplate(Template arg0) {
        }

        @Override
        public void visitLoop(IEnumeratingLoop loop, RuntimeEnvironment<?, ?> environment) {
        }

        @Override
        public void visitIteratorAssignment(IEnumeratingLoop loop,
            net.ssehub.easy.instantiation.core.model.buildlangModel.VariableDeclaration var,
            Object value) {
        }

        @Override
        public void visitedLoop(IEnumeratingLoop loop, RuntimeEnvironment<?, ?> environment) {
        }

        @Override
        public void visitWhileBody() {
        }

        @Override
        public void visitedWhileBody() {
        }

        @Override
        public void visitScriptBody(Script arg0, RuntimeEnvironment<?, ?> arg1) {
        }
        
        @Override
        public void enable(boolean enable) {
        }
        
        @Override
        public void visitFlush() {
        }

        @Override
        public void visitedFlush() {
        }
        
    }

}
