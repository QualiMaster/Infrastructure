package eu.qualimaster.common.switching;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.serializer.KryoGeneralTupleSerializer;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;
import eu.qualimaster.common.signal.TopologySignal;
import eu.qualimaster.common.switching.IState.SwitchState;
/**
 * Provides a strategy serving separate intermediary node which is equipped with the corresponding algorithm.
 * @author Cui Qin
 *
 */
public class SeparateIntermediaryStrategy extends AbstractSwitchStrategy {
    private static final Logger LOGGER = Logger.getLogger(SeparateIntermediaryStrategy.class);
    private static final int QUEUE_SIZE = 100;
    private Map<String, Serializable> parameters;
    @SuppressWarnings("rawtypes")
    private Map conf;
    private transient Queue<IGeneralTuple> inQueue = null; //handle incoming tuples
    private transient LinkedList<IGeneralTuple> outQueue = null; //handle outgoing tuples
    private transient Queue<IGeneralTuple> tmpQueue = null; //handle transferred tuples
    private transient SynchronizedQueue<IGeneralTuple> syn = null;
    private transient SynchronizedQueue<IGeneralTuple> tmpSyn = null;
    private SwitchState currentState;
    private long lastProcessedId;
    
    /**
     * Creates a strategy serving separate intermediary node.
     * @param conf the storm conf
     * @param state the switch state
     */
    public SeparateIntermediaryStrategy(Map conf, SwitchState state) {
        this.conf = conf;
        parameters = new HashMap<String, Serializable>();
        inQueue = new ConcurrentLinkedQueue<IGeneralTuple>();
        outQueue = new LinkedList<IGeneralTuple>();
        tmpQueue = new ConcurrentLinkedQueue<IGeneralTuple>();
        currentState = state;
    }
    
    @Override
    public Serializable getSignalValue(String signalName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TupleReceiverHandler getTupleReceiverHandler() {
        syn = new SynchronizedQueue<IGeneralTuple>(inQueue, QUEUE_SIZE);
        tmpSyn = new SynchronizedQueue<IGeneralTuple>(tmpQueue, QUEUE_SIZE);
        KryoGeneralTupleSerializer genSer = new KryoGeneralTupleSerializer(conf); 
        KryoSwitchTupleSerializer swiSer = new KryoSwitchTupleSerializer(conf);
        TupleReceiverHandler handler = new TupleReceiverHandler(genSer, swiSer, syn, tmpSyn);
        return handler;
    }

    @Override
    public IGeneralTuple produceTuple() {
        IGeneralTuple tuple = null;
        if (currentState.equals(SwitchState.ACTIVE_DEFAULT)) { //TODO:check which state is needed to check here
            tuple = syn.consume();
            if (!tuple.isGeneralTuple()) { //queue the emitted tuple in the switch phase 
                outQueue.add(tuple);
            }
        }
        return tuple;
    }

    @Override
    public void doSignal(TopologySignal signal) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void ack(Object msgId) {
        Iterator<IGeneralTuple> iterator = outQueue.descendingIterator();
        while (iterator.hasNext()) {
            ISwitchTuple ackItem = (ISwitchTuple) iterator.next();
            if (msgId.equals(ackItem.getId())) {
                lastProcessedId = ackItem.getId();
                boolean flag = outQueue.remove(ackItem);
                LOGGER.info(System.currentTimeMillis() + " Acked the tuple with the msgId: " + msgId 
                        + " removed: " + flag + ", outQueue size: " + outQueue.size());
                break;
            }
        }
        
    }

}
