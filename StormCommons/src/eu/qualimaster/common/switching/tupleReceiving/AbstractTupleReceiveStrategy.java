package eu.qualimaster.common.switching.tupleReceiving;

import java.io.IOException;

import org.apache.log4j.Logger;

import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.actions.SwitchActionMap;

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
    private SwitchActionMap switchActionMap;
    private TupleReceiveServer server;
    
    /**
     * Constructor of the abstract strategy of tuple receiving.
     * @param serializer the tuple serializer
     * @param signalCon the signal connection used to send signals
     * @param switchActionMap the map containing the switch actions
     */
    public AbstractTupleReceiveStrategy(ISwitchTupleSerializer serializer, AbstractSignalConnection signalCon, 
    		SwitchActionMap switchActionMap) {
        this.serializer = serializer;
        this.signalCon = signalCon;
        this.switchActionMap = switchActionMap;
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
     * Returns the switch action map.
     * @return the switch action map
     */
    public SwitchActionMap getActionMap() {
        return switchActionMap;
    }

    @Override
    public void initTupleReceiveServer(int port) {
        LOGGER.info("Creating a socket server with the port:" + port);
        server = new TupleReceiveServer(this, port);
        server.start();
    }
    
    @Override
    public void stopTupleReceiveServer() {
    	LOGGER.info("Stopping the tuple receive server....");
    	try {
			server.stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
