package eu.qualimaster.common.switching.acknowledgement;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.switching.actions.IAction;
import eu.qualimaster.common.switching.actions.SwitchStates;
import eu.qualimaster.common.switching.actions.SwitchStates.ActionState;

/**
 * The acknowledgement strategy using Storm acknowledgement technique.
 * @author Cui Qin
 *
 */
public class AcknowledgementStrategyStorm extends AbstractAcknowledgementStrategy {
    private static final Logger LOGGER = Logger.getLogger(AcknowledgementStrategyStorm.class);
    private transient ConcurrentLinkedDeque<ISwitchTuple> outQueue;
    private transient Iterator<ISwitchTuple> iterator = null;
    private Map<ActionState, List<IAction>> actionMap;
    private PrintWriter out = null;
    
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
     * @param actionMap the map containing the switch actions
     */
    public AcknowledgementStrategyStorm(ConcurrentLinkedDeque<ISwitchTuple> outQueue, 
            Map<ActionState, List<IAction>> actionMap) {
        this(outQueue);
        this.actionMap = actionMap;
    }
     
    /**
     * Constructor.
     * @param outQueue the queue storing the tuples that are pushed into processing
     * @param actionMap the map containing the switch actions
     * @param out the <code>PrintWriter</code> instance used to write logs into corresponding files
     */
    public AcknowledgementStrategyStorm(ConcurrentLinkedDeque<ISwitchTuple> outQueue, 
            Map<ActionState, List<IAction>> actionMap, PrintWriter out) {
        this(outQueue, actionMap);
        this.out = out;
    }
    
    @Override
    public long ack(Object msgId) {
        long lastProcessedId = 0;
        if (null != out) {
            out.println("The size of the output queue:" + outQueue.size());
            out.flush();
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

}
