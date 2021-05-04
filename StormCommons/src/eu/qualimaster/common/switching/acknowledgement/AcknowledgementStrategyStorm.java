package eu.qualimaster.common.switching.acknowledgement;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import switching.logging.LogProtocol;
import switching.logging.QueueStatus;

/**
 * The acknowledgement strategy using Storm acknowledgement technique.
 * @author Cui Qin
 *
 */
public class AcknowledgementStrategyStorm extends AbstractAcknowledgementStrategy {
    private static final Logger LOGGER = Logger.getLogger(AcknowledgementStrategyStorm.class);
    private transient ConcurrentLinkedDeque<ISwitchTuple> outQueue;
    private transient Iterator<ISwitchTuple> iterator = null;
    private LogProtocol logProtocol = null;
    
    /**
     * Constructor.
     * @param outQueue the queue storing the tuples that are pushed into processing
     */
    public AcknowledgementStrategyStorm(ConcurrentLinkedDeque<ISwitchTuple> outQueue) {
        this.outQueue = outQueue;
    }
     
    /**
     * Constructor.
     * @param outQueue the queue storing the tuples that are pushed into processing
     * @param logProtocol the log protocol used to write logs into corresponding files
     */
    public AcknowledgementStrategyStorm(ConcurrentLinkedDeque<ISwitchTuple> outQueue, LogProtocol logProtocol) {
        this(outQueue);
        this.logProtocol = logProtocol;
    }
    
    @Override
    public long ack(Object msgId) {
        long lastProcessedId = 0;
        if (null != logProtocol) {
            logProtocol.createQUEUELog(QueueStatus.OUTPUT, outQueue.size());
        }
        if (outQueue != null && !outQueue.isEmpty()) {
            ISwitchTuple ackItem = outQueue.peek();
            if (null != ackItem) {
                if (msgId.equals(ackItem.getId())) {
                    ackItem = outQueue.remove();
                    lastProcessedId = ackItem.getId();
                } else {
                    iterator = outQueue.descendingIterator();
                    while (iterator.hasNext()) {
                        ackItem = iterator.next();
                        if (msgId.equals(ackItem.getId())) {
                            lastProcessedId = ackItem.getId();
                            outQueue.remove(ackItem);
                            break;
                        }
                    }
                }
            }
        } else if (null == outQueue) {
            LOGGER.warn("The output queue is null!");
        }
        return lastProcessedId;
    }
    
    /**
     * Acknowledge all the tuples in case of duplicates in the outQueue.
     * @param msgId the message id of the tuple acknowledged
     * @return the id of the last processed tuple
     */
    public long ackAll(Object msgId) {
        long lastProcessedId = 0;
        if (null != logProtocol) {
            logProtocol.createQUEUELog(QueueStatus.OUTPUT, outQueue.size());
        }
        if (outQueue != null && !outQueue.isEmpty()) {
            ISwitchTuple ackItem;
            iterator = outQueue.iterator();
            while (iterator.hasNext()) {
                ackItem = iterator.next();
                if (msgId.equals(ackItem.getId())) {
                    lastProcessedId = ackItem.getId();
                    outQueue.remove(ackItem);
                }
            }
        } else if (null == outQueue) {
            LOGGER.warn("The output queue is null!");
        }
        return lastProcessedId;
    }
    
}
