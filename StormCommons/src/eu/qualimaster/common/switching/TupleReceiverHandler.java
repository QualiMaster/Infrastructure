package eu.qualimaster.common.switching;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.serializer.IGeneralTupleSerializer;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
/**
 * Tuple handler for receiving tuples in both general and switch mode. 
 * @author Cui Qin
 *
 */
public class TupleReceiverHandler implements ITupleReceiverHandler {
    private static final Logger LOGGER = Logger.getLogger(TupleReceiverHandler.class);
    private Socket socket;
    private InputStream in; 
    private Input kryoInput = null;
    private boolean cont = true;
    private IGeneralTupleSerializer genSer = null;
    private ISwitchTupleSerializer swiSer = null;
    private SynchronizedQueue<IGeneralTuple> syn = null; //general queue
    private SynchronizedQueue<IGeneralTuple> tmpSyn = null; //tmp queue
    private boolean isGeneralTuple = true; //indicates the type of received tuples, default is general tuple
    private boolean useTemporaryQueue = false; //indicates the queue to be used, default is the general queue
    /**
     * Create a handler receiving the tuples in both {@link IGeneralTuple} and {@link ISwitchTuple} types,
     * and storing received tuples into either a general queue during the warm-up phase or a temporary queue 
     * for transferred data during data synchronization.
     * @param genSer the serializer for the general tuple {@link IGeneralTuple}
     * @param swiSer the serializer for the switch tuple {@link ISwitchTuple}
     * @param syn the general queue for storing tuples
     * @param tmpSyn the temporary queue for storing tuples
     */
    public TupleReceiverHandler(IGeneralTupleSerializer genSer, ISwitchTupleSerializer swiSer, 
            SynchronizedQueue<IGeneralTuple> syn, SynchronizedQueue<IGeneralTuple> tmpSyn) {
        this.genSer = genSer;
        this.swiSer = swiSer;
        this.syn = syn;
        this.tmpSyn = tmpSyn;
    } 
    
    /**
     * Create a handler receiving the tuples only in {@link IGeneralTuple} type and storing received tuples 
     * in the given queue.
     * @param genSer the serializer for the general tuple {@link IGeneralTuple}
     * @param syn the queue for storing tuples
     */
    public TupleReceiverHandler(IGeneralTupleSerializer genSer, SynchronizedQueue<IGeneralTuple> syn) {
        this.genSer = genSer;
        this.syn = syn;
    } 
    
    /**
     * Create a handler receiving the tuples only in {@link ISwitchTuple} type and storing received tuples 
     * in the given queue.
     * @param swiSer the serializer for the general tuple {@link ISwitchTuple}
     * @param syn the queue for storing tuples
     */
    public TupleReceiverHandler(ISwitchTupleSerializer swiSer, SynchronizedQueue<IGeneralTuple> syn) {
        this.swiSer = swiSer;
        this.syn = syn;
        isGeneralTuple = false;
    }
    
    @Override
    public void run() {
        while (cont && kryoInput != null) {
            try {
                if (kryoInput.canReadInt()) {
                    int len = kryoInput.readInt();
                    if (len == DataFlag.DATA_FLAG) { //switch the tuple serializer
                        byte[] ser = new byte[DataFlag.FLAG_BYTES_LEN];
                        kryoInput.readBytes(ser);
                        switchMode(new String(ser));   
                        LOGGER.info("Received flag: " + new String(ser));
                    } else {
                        byte[] ser = new byte[len];
                        kryoInput.readBytes(ser);
                        enqueue(ser); //enqueue the received tuple
                    }
                }
            } catch (KryoException e) {
                try {
                    stop();
                } catch (IOException e1) {
                }
            }
        }
    }

    @Override
    public void setSocket(Socket socket) {
        this.socket = socket;
        try {
            in = socket.getInputStream();
            kryoInput = new Input(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void stop() throws IOException {
        if (null != socket) {
            LOGGER.info("Stopping handler");
            cont = false;
            in.close();
            in = null;
            if (null != kryoInput) {
                kryoInput.close();
                kryoInput = null;
            }
            socket.close();
            socket = null;
            LOGGER.info("Stopped handler");
        }
    }

    @Override
    public boolean isStopped() {
        return null == socket;
    }
    
    /**
     * Switches the receiving mode based on the received flag.
     * @param mode the received flag
     */
    private void switchMode(String mode) {
        switch(mode) { 
        case DataFlag.GENERAL_TUPLE_FLAG: 
            isGeneralTuple = true;
            break;
        case DataFlag.SWITCH_TUPLE_FLAG:
            isGeneralTuple = false;
            break;
        case DataFlag.GENERAL_QUEUE_FLAG:
            useTemporaryQueue = false;
            break;
        case DataFlag.TEMPORARY_QUEUE_FLAG:
            useTemporaryQueue = true;
            break;
        default:
            isGeneralTuple = true;
            useTemporaryQueue = false;
            break;
        }
    }

    /**
     * Enqueue the received tuple.
     * @param ser the tuple bytes
     */
    private void enqueue(byte[] ser) {
        IGeneralTuple tuple;
        try {
            //determining the received tuple type
            if (isGeneralTuple) { 
                tuple = genSer.deserialize(ser); 
            } else {
                tuple = swiSer.deserialize(ser);
            }
            if (tuple != null) {
                //determining the queue to be used
                if (useTemporaryQueue) { 
                    tmpSyn.produce(tuple);
                } else {
                    syn.produce(tuple);
                }
            }
        } catch (NullPointerException e) { // TODO check whether catching RuntimeException in serializers is sufficient
            e.printStackTrace();
        }
    }     
    
}
