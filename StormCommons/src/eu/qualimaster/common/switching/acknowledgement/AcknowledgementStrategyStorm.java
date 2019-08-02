package eu.qualimaster.common.switching.acknowledgement;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;

/**
 * The acknowledgement strategy using Storm acknowledgement technique.
 * @author Cui Qin
 *
 */
public class AcknowledgementStrategyStorm extends AbstractAcknowledgementStrategy {
    private static final Logger LOGGER = Logger.getLogger(AcknowledgementStrategyStorm.class);
    private transient ConcurrentLinkedDeque<ISwitchTuple> outQueue;
    private transient Iterator<ISwitchTuple> iterator = null;
    
    /**
     * Constructor.
     * @param outQueue the queue storing the tuples that are pushed into processing
     */
    public AcknowledgementStrategyStorm(ConcurrentLinkedDeque<ISwitchTuple> outQueue) {
        this.outQueue = outQueue;
    }
    
    @Override
    public long ack(Object msgId) {
        long lastProcessedId = 0;
        if (!outQueue.isEmpty()) {
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
        }
        return lastProcessedId;
    }

}
