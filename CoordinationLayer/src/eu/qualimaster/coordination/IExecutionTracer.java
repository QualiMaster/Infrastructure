package eu.qualimaster.coordination;

import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CommandSequence;
import eu.qualimaster.coordination.commands.CommandSet;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.MonitoringChangeCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;

/**
 * Allows to trace the execution.
 * 
 * @author Holger Eichelberger
 */
public interface IExecutionTracer {

    /**
     * Is called when an algorithm change command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedAlgorithmChangeCommand(AlgorithmChangeCommand command, CoordinationExecutionResult result);

    /**
     * Is called when a parameter change command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedParameterChangeCommand(ParameterChangeCommand<?> command, CoordinationExecutionResult result);

    /**
     * Is called when a command sequence was executed.
     * 
     * @param sequence the command sequence
     * @param result the execution result
     */
    public void executedCommandSequence(CommandSequence sequence, CoordinationExecutionResult result);

    /**
     * Is called when a command set was executed.
     * 
     * @param set the command sequence
     * @param result the execution result
     */
    public void executedCommandSet(CommandSet set, CoordinationExecutionResult result);

    /**
     * Is called when a pipeline command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedPipelineCommand(PipelineCommand command, CoordinationExecutionResult result);

    /**
     * Is called when a schedule wavefront command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedScheduleWavefrontAdaptationCommand(ScheduleWavefrontAdaptationCommand command, 
        CoordinationExecutionResult result);

    /**
     * Is called when a monitoring change command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedMonitoringChangeCommand(MonitoringChangeCommand command, CoordinationExecutionResult result);
    
    /**
     * Is called when a parallelism change command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedParallelismChangeCommand(ParallelismChangeCommand command, CoordinationExecutionResult result);
    
    /**
     * Is called when a log entry was written.
     * 
     * @param text the text of the log entry
     */
    public void logEntryWritten(String text);

    /**
     * Is called when a replay command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedReplayCommand(ReplayCommand command, CoordinationExecutionResult result);

    /**
     * Is called when a load schedding command command was executed.
     * 
     * @param command the command
     * @param result the execution result
     */
    public void executedLoadScheddingCommand(LoadSheddingCommand command, CoordinationExecutionResult result);

}
