package eu.qualimaster.common.switching.acknowledgement;

import eu.qualimaster.common.switching.IStrategy;
/**
 * Interface for the acknowledgement strategy.
 * @author Cui Qin
 *
 */
public interface IAcknowledgementStrategy extends IStrategy {
    /**
     * Acknowledge the processed tuple.
     * @param msgId the id of the processed tuple
     * @return the id of the last processed tuple
     */
    public long ack(Object msgId);
}
