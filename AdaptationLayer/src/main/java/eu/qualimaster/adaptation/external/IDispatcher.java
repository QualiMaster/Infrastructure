package eu.qualimaster.adaptation.external;

/**
 * A message dispatcher, a kind of visitor.
 * 
 * @author Holger Eichelberger
 * @author Bendix Harries
 */
public interface IDispatcher {

    /**
     * Handles a disconnect message. [server side]
     * 
     * @param msg the message
     */
    public void handleDisconnectRequest(DisconnectRequest msg);
    
    /**
     * Handles a switch algorithm message. [server side]
     * 
     * @param msg the message
     */
    public void handleSwitchAlgorithmRequest(SwitchAlgorithmRequest msg);
    
    /**
     * Handles a request for changing a parameter. [server side]
     * 
     * @param msg the message
     */
    public void handleChangeParameterRequest(ChangeParameterRequest<?> msg);
    
    /**
     * Handles a monitoring data message. [client side]
     * 
     * @param msg the message
     */
    public void handleMonitoringDataMessage(MonitoringDataMessage msg);
    
    /**
     * Handles an algorithm change message. [client side]
     * 
     * @param msg the message
     */
    public void handleAlgorithmChangedMessage(AlgorithmChangedMessage msg);

    /**
     * Handles a hardware alive message. [client side]
     * 
     * @param msg the message
     */
    public void handleHardwareAliveMessage(HardwareAliveMessage msg);

    /**
     * Handles a pipeline message. [server side]
     * 
     * @param msg the message
     */
    public void handlePipelineMessage(PipelineMessage msg);

    /**
     * Handles a logging message. [client side]
     * 
     * @param msg the message
     */
    public void handleLoggingMessage(LoggingMessage msg);

    /**
     * Handles a logging filter message. [server side]
     * 
     * @param msg the message
     */
    public void handleLoggingFilterRequest(LoggingFilterRequest msg);

    /**
     * Handles a command execution message. [client side]
     * 
     * @param msg the message
     */
    public void handleExecutionResponseMessage(ExecutionResponseMessage msg);

    /**
     * Handles a pipeline status request. [server side]
     * 
     * @param msg the message
     */
    public void handlePipelineStatusRequest(PipelineStatusRequest msg);

    /**
     * Handles a pipeline status response. [client side]
     * 
     * @param msg the message
     */
    public void handlePipelineStatusResponse(PipelineStatusResponse msg);
    
    /**
     * Handles an update message for a cloud resource.
     * 
     * @param msg the message
     */
    public void handleUpdateCloudResourceMessage(UpdateCloudResourceMessage msg);
    
    /**
     * Handles a cloud pipeline message.
     * 
     * @param msg the message
     */
    public void handleCloudPipelineMessage(CloudPipelineMessage msg);

    /**
     * Handles a data replay message.
     * 
     * @param msg the message
     */
    public void handleReplayMessage(ReplayMessage msg);

    /**
     * Handles a configuration change message.
     * 
     * @param msg the message
     */
    public void handleConfigurationChangeMessage(ConfigurationChangeRequest msg);

    /**
     * Handles a resource change message.
     * 
     * @param msg the message
     */
    public void handleResourceChangeMessage(ResourceChangeRequest msg);
    
}
