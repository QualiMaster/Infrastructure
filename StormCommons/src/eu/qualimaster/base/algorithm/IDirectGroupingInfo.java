package eu.qualimaster.base.algorithm;

/**
 * Defines the interface for the additional information needed in the Storm direct grouping.
 * @author qin
 *
 */
public interface IDirectGroupingInfo {
    
    /**
     * Sets the task id.
     * @param taskId the task id
     */
    public void setTaskId(int taskId);
    /**
     * Returns the task id.
     * @return the task id
     */
    public int getTaskId();
}
