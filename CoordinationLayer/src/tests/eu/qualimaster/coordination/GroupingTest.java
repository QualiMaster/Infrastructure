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
package tests.eu.qualimaster.coordination;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.coordination.AbstractCoordinationCommandExecutionVisitor;
import eu.qualimaster.coordination.CoordinationExecutionCode;
import eu.qualimaster.coordination.IExecutionTracer;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CommandSequence;
import eu.qualimaster.coordination.commands.CommandSet;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.MonitoringChangeCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;
import eu.qualimaster.coordination.commands.ShutdownCommand;
import eu.qualimaster.coordination.commands.UpdateCommand;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Does a test of the grouping visitors.
 * 
 * @author Holger Eichelberger
 */
public class GroupingTest {
    
    private static final boolean DEBUG = false;
    private Set<CoordinationCommand> expected = new HashSet<CoordinationCommand>();
    private Set<CoordinationCommand> fail = new HashSet<CoordinationCommand>();

    /**
     * Notifies that a command was processed.
     * 
     * @param command the command
     * @return the execution result (<b>null</b>)
     */
    private CoordinationExecutionResult processed(CoordinationCommand command) {
        CoordinationExecutionResult result;
        if (fail.contains(command)) {
            if (DEBUG) {
                System.out.println("FAIL: " + command);
            }
            result = new CoordinationExecutionResult(command, "Failed due to signal sending", 
                CoordinationExecutionCode.SIGNAL_SENDING_ERROR); 
        } else {
            if (DEBUG) {
                System.out.println("SUCCESS: " + command);
            }
            expected.remove(command);
            result = null;
        }
        return result;
    }
    
    /**
     * Notifies that a list of parameter changes was processed.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param changes the changes
     */
    private void processed(String pipeline, String pipelineElement, List<ParameterChange> changes) {
        if (null != changes) {
            for (ParameterChange cng : changes) {
                processed(pipeline, pipelineElement, cng);
            }
        }
    }

    /**
     * Notifies that a parameter change was processed.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param change the change
     */
    private void processed(String pipeline, String pipelineElement, ParameterChange change) {
        Iterator<CoordinationCommand> iter = expected.iterator();
        while (iter.hasNext()) {
            CoordinationCommand cmd = iter.next();
            if (cmd instanceof ParameterChangeCommand) {
                ParameterChangeCommand<?> pCmd = (ParameterChangeCommand<?>) cmd;
                if (pCmd.getPipeline().equals(pipeline) && pCmd.getPipelineElement().equals(pipelineElement)) {
                    if (pCmd.getParameter().equals(change.getName()) && pCmd.getValue().equals(change.getValue())) {
                        iter.remove();
                    }
                }
            }
        }
    }
    
    /**
     * A test execution visitor.
     * 
     * @author Holger Eichelberger
     */
    private class ExecutionVisitor extends AbstractCoordinationCommandExecutionVisitor {

        /**
         * Creates the execution visitor.
         * 
         * @param tracer the tracer (may be <b>null</b>)
         */
        protected ExecutionVisitor(IExecutionTracer tracer) {
            super(tracer);
        }

        @Override
        public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
            ScheduleWavefrontAdaptationCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
            return processed(command);
        }

        @Override
        public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
            return processed(command);
        }

        @Override
        protected CoordinationExecutionResult handleAlgorithmChange(AlgorithmChangeCommand command,
            List<ParameterChange> parameters) {
            processed(command.getPipeline(), command.getPipelineElement(), parameters);
            return processed(command);
        }

        @Override
        protected CoordinationExecutionResult handleParameterChange(ParameterChangeCommand<?> command,
            List<ParameterChange> huckup) {
            processed(command.getPipeline(), command.getPipelineElement(), huckup);
            return processed(command);
        }
        
    }
    
    /**
     * Does a test about command sequences and sets.
     */
    @Test
    public void groupingTest() {
        CommandSequence seq = new CommandSequence();
        seq.add(record(new AlgorithmChangeCommand("SwitchPip", "snk", "Random Sink")));
        seq.add(record(new AlgorithmChangeCommand("SwitchPip", "Replay Sink", "Random Sink")));
        CommandSet set = new CommandSet();
        set.add(record(new AlgorithmChangeCommand("SwitchPip", "processor", "SwitchProcessor1")));
        set.add(record(new ParameterChangeCommand<Integer>("SwitchPip", "processor", "delay", 0)));
        seq.add(set);
        
        ExecutionVisitor vis = new ExecutionVisitor(null);
        CoordinationExecutionResult res = seq.accept(vis);
        Assert.assertNull(res);
        Assert.assertTrue(expected.isEmpty());
    }

    /**
     * Records created commands as expected and successful.
     * 
     * @param command the command
     * @return <code>command</code>
     */
    private CoordinationCommand record(CoordinationCommand command) {
        return record(command, false);
    }
    
    /**
     * Records created commands as expected and successful.
     * 
     * @param command the command
     * @param fail whether the command shall "fail" due to a signal sending failure (only for non-joined top-level 
     *     commands)
     * @return <code>command</code>
     */
    private CoordinationCommand record(CoordinationCommand command, boolean fail) {
        if (fail) {
            this.fail.add(command);
        } else {
            expected.add(command);
        }
        return command;
    }
    
    /**
     * A grouping test if signal sending fails.
     */
    @Test
    public void groupingTestSendingFails() {
        CommandSequence seq = new CommandSequence();
        seq.add(record(new AlgorithmChangeCommand("SwitchPip", "snk", "Random Sink")));
        seq.add(record(new AlgorithmChangeCommand("SwitchPip", "Replay Sink", "Random Sink"), true));
        CommandSet set = new CommandSet();
        set.add(record(new AlgorithmChangeCommand("SwitchPip", "processor", "SwitchProcessor1")));
        set.add(record(new ParameterChangeCommand<Integer>("SwitchPip", "processor", "delay", 0)));
        seq.add(set);
        
        ExecutionVisitor vis = new ExecutionVisitor(null);
        CoordinationExecutionResult res = seq.accept(vis);
        Assert.assertNotNull(res);
        Assert.assertTrue(expected.isEmpty());
    }
    
    /**
     * Clears the internal state of the test.
     */
    @After
    public void after() {
        fail.clear();
        expected.clear();
    }

}
