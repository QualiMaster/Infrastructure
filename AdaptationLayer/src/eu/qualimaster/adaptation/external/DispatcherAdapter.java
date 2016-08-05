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
package eu.qualimaster.adaptation.external;

/**
 * An empty (adapter) dispatcher implementation.
 * 
 * @author Holger Eichelberger
 * @author Bendix Harries
 */
public class DispatcherAdapter implements IDispatcher {

    @Override
    public void handleDisconnectRequest(DisconnectRequest msg) {
    }

    @Override
    public void handleSwitchAlgorithmRequest(SwitchAlgorithmRequest msg) {
    }

    @Override
    public void handleChangeParameterRequest(ChangeParameterRequest<?> msg) {
    }

    @Override
    public void handleMonitoringDataMessage(MonitoringDataMessage msg) {
    }

    @Override
    public void handleAlgorithmChangedMessage(AlgorithmChangedMessage msg) {
    }

    @Override
    public void handleHardwareAliveMessage(HardwareAliveMessage msg) {
    }

    @Override
    public void handlePipelineMessage(PipelineMessage msg) {
    }

    @Override
    public void handleLoggingMessage(LoggingMessage msg) {
    }

    @Override
    public void handleLoggingFilterRequest(LoggingFilterRequest msg) {
    }

    @Override
    public void handleExecutionResponseMessage(ExecutionResponseMessage msg) {
    }

    @Override
    public void handlePipelineStatusRequest(PipelineStatusRequest msg) {
    }

    @Override
    public void handlePipelineStatusResponse(PipelineStatusResponse msg) {
    }

    @Override
    public void handleUpdateCloudResourceMessage(UpdateCloudResourceMessage msg) {       
    }
    
    @Override
    public void handleCloudPipelineMessage(CloudPipelineMessage msg){
    }

    @Override
    public void handleReplayMessage(ReplayMessage msg) {
    }

    @Override
    public void handleConfigurationChangeMessage(ConfigurationChangeMessage msg) {
    }

    @Override
    public void handleResourceChangeMessage(ResourceChangeMessage msg) {
    }

}
