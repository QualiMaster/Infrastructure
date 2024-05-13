package eu.qualimaster.coordination.commands;

import eu.qualimaster.common.QMInternal;

/**
 * A visitor for coordination commands.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public interface ICoordinationCommandVisitor {

    /**
     * Visits an {@link AlgorithmChangeCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command);

    /**
     * Visits a {@link CommandSequence}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitCommandSequence(CommandSequence command);

    /**
     * Visits a {@link CommandSet}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitCommandSet(CommandSet command);

    /**
     * Visits a {@link ParameterChangeCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command);

    /**
     * Visits a {@link PipelineCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command);
    
    /**
     * Visits a {@link ScheduleWavefrontAdaptationCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
        ScheduleWavefrontAdaptationCommand command);
    
    
    /**
     * Visits a {@link MonitoringChangeCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command);

    /**
     * Visits a {@link ParallelismChangeCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command);

    /**
     * Visits a {@link ProfileAlgorithmCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command);

    /**
     * Visits a {@link ShutdownCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command);
    
    /**
     * Visits a {@link UpdateCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command);

    /**
     * Visits a {@link ReplayCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitReplayCommand(ReplayCommand command);

    /**
     * Visits a {@link LoadSheddingCommand}.
     * 
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command);

    /**
     * Visits a {@link CloudExecutionCommand}.
     * @param command the command
     * @return <b>null</b> if successful, the failed command and a code for failing else
     */
    public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command);
}
