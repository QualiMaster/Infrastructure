package eu.qualimaster.common.switching.tupleReceiving;

import org.apache.log4j.Logger;

import eu.qualimaster.base.serializer.ISwitchTupleSerializer;

/**
 * Provide an abstract strategy for tuple receiving.
 * @author Cui Qin
 *
 */
public abstract class AbstractTupleReceiveStrategy implements ITupleReceiveStrategy {
    protected static final String STRATEGYTYPE = "tupleReceiving";
    private static final Logger LOGGER = Logger.getLogger(AbstractTupleReceiveStrategy.class);
    private ISwitchTupleSerializer serializer;
    
    /**
     * Constructor of the abstract strategy of tuple receiving.
     * @param serializer the tuple serializer
     */
    public AbstractTupleReceiveStrategy(ISwitchTupleSerializer serializer) {
        this.serializer = serializer;
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
    
    @Override
    public void initTupleReceiveServer(int port) {
        LOGGER.info("Creating a socket server with the port:" + port);
        TupleReceiveServer server = new TupleReceiveServer(this, port);
        server.start();
    }
}
