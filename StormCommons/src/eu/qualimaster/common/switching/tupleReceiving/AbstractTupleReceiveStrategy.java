package eu.qualimaster.common.switching.tupleReceiving;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.actions.IAction;
import eu.qualimaster.common.switching.actions.SwitchStates.ActionState;

/**
 * Provide an abstract strategy for tuple receiving.
 * @author Cui Qin
 *
 */
public abstract class AbstractTupleReceiveStrategy implements ITupleReceiveStrategy {
    protected static final String STRATEGYTYPE = "tupleReceiving";
    private static final Logger LOGGER = Logger.getLogger(AbstractTupleReceiveStrategy.class);
    private ISwitchTupleSerializer serializer;
    private AbstractSignalConnection signalCon;
    private Map<ActionState, List<IAction>> actionMap;
    
    /**
     * Constructor of the abstract strategy of tuple receiving.
     * @param serializer the tuple serializer
     * @param signalCon the signal connection used to send signals
     * @param actionMap the map containing the switch actions
     */
    public AbstractTupleReceiveStrategy(ISwitchTupleSerializer serializer, AbstractSignalConnection signalCon, 
            Map<ActionState, List<IAction>> actionMap) {
        this.serializer = serializer;
        this.signalCon = signalCon;
        this.actionMap = actionMap;
    }
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }

    /**
     * Return the tuple serializer.
     * @return the tuple serializer
     */
    public ISwitchTupleSerializer getSerializer() {
        return serializer;
    }   
    
    /**
     * Returns the signal connection.
     * @return the signal connection
     */
    public AbstractSignalConnection getSignalCon() {
        return signalCon;
    }
    
    /**
     * Returns the action map.
     * @return the action map
     */
    public Map<ActionState, List<IAction>> getActionMap() {
        return actionMap;
    }

    @Override
    public void initTupleReceiveServer(int port) {
        LOGGER.info("Creating a socket server with the port:" + port);
        TupleReceiveServer server = new TupleReceiveServer(this, port);
        server.start();
    }
}
