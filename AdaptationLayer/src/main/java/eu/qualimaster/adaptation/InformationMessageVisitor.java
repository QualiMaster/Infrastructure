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
package eu.qualimaster.adaptation;

import eu.qualimaster.adaptation.external.ExecutionResponseMessage.ResultType;

import java.util.Map;

import eu.qualimaster.adaptation.external.InformationMessage;
import eu.qualimaster.coordination.ParallelismChangeRequest;
import eu.qualimaster.coordination.commands.AbstractCoordinationCommandVisitor;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.MonitoringChangeCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.PipelineCommand.Status;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;
import eu.qualimaster.coordination.commands.ShutdownCommand;
import eu.qualimaster.coordination.commands.UpdateCommand;

/**
 * Creates an information message visitor turning coordination commands into information messages.
 * 
 * @author Holger Eichelberger
 */
class InformationMessageVisitor extends AbstractCoordinationCommandVisitor {

    private String prefix;
    private CoordinationCommandExecutionEvent response;
    
    /**
     * Creates an information visitor.
     * 
     * @param prefix the prefix to be prepended before each message (may be <b>null</b> or empty). If given and not 
     *  empty, a colon is prepended.
     */
    public InformationMessageVisitor(String prefix) {
        if (null == prefix) {
            this.prefix = "";
        } else if (prefix.length() > 0) {
            this.prefix = prefix + ": ";
        } else {
            this.prefix = "";
        }
    }
    
    /**
     * Sets the actual response to be considered for the information message. The response is cleared upon visiting.
     *  
     * @param response the actual response
     */
    public void setResponse(CoordinationCommandExecutionEvent response) {
        this.response = response;
    }

    /**
     * Schedules an information message.
     * 
     * @param pipeline the pipeline (may be <b>null</b>)
     * @param pipelineElement the pipeline element (may be <b>null</b>)
     * @param description the description (may be <b>null</b> for just clearing the {@link #response})
     */
    private void schedule(String pipeline, String pipelineElement, String description) {
        if (null == description) {
            response = null;
        } else {
            String postfix = "";
            ResultType resultType = null;
            if (null != response) {
                if (response.isSuccessful()) {
                    postfix = "successful";
                    resultType = ResultType.SUCCESSFUL;
                } else {
                    postfix = "failed: " + response.getMessage();
                    resultType = ResultType.FAILED;
                }
                response = null;
            } else {
                postfix = "requested";
            }
            postfix = " " + postfix;
            InformationMessage msg = new InformationMessage(pipeline, pipelineElement, prefix + description + postfix, 
                resultType);
            AdaptationManager.send(msg);
        }
    }
    
    @Override
    public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
        schedule(command.getPipeline(), command.getPipelineElement(), 
            "algorithm change to '" + command.getAlgorithm() + "'");
        return null;
    }

    @Override
    public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
        schedule(command.getPipeline(), command.getPipelineElement(), 
            "change parameter '" + command.getParameter() + "' to '" +  command.getValue());
        return null;
    }

    @Override
    public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
        schedule(command.getPipeline(), null, 
            (command.getStatus() == Status.START ? "start pipeline" : "stop pipeline"));
        return null;
    }

    @Override
    public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
        ScheduleWavefrontAdaptationCommand command) {
        schedule(null, null, null);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
        schedule(null, null, null);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
        String text;
        if (null != command.getIncrementalChanges()) {
            text = " ";
            int count = 0;
            for (Map.Entry<String, ParallelismChangeRequest> ent : command.getIncrementalChanges().entrySet()) {
                if (count > 0) {
                    text = text + ", ";
                }
                text += ent.getKey() + " ->";
                ParallelismChangeRequest req = ent.getValue();
                if (req.getExecutorDiff() != 0) {
                    text += " executor diff " + req.getExecutorDiff();
                }
                if (null != req.getHost()) {
                    text += " target machine " + req.getHost(); 
                }
                count++;
            }
        } else {
            text = " workers " + command.getNumberOfWorkers();
            if (null != command.getExecutors()) {
                text += " executors " + command.getExecutors(); 
            }
        }
        schedule(command.getPipeline(), null, "parallelism change " + text);
        return null;
    }

    @Override
    public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
        return null;
    }

    @Override
    public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
        return null;
    }

    @Override
    public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
        return null;
    }

    @Override
    public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
        schedule(command.getPipeline(), null, (command.getStartReplay() ? "start" : "end")
            + "replay #" + command.getTicket());
        return null;
    }

    @Override
    public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
        schedule(command.getPipeline(), command.getPipelineElement(), 
            "change load schedding to '" + command.getShedder());
        return null;
    }

    @Override
    public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
        return null;
    }

}
