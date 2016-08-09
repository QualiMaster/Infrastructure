package eu.qualimaster.common.switching;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.serializer.IGeneralTupleSerializer;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
/**
 * Tuple handler for receiving tuples, including the ones in both general and switch mode. 
 * @author Cui Qin
 *
 */
public class TupleReceiverHandler implements ITupleReceiverHandler {
    private static final Logger LOGGER = Logger.getLogger(TupleReceiverHandler.class);
    private static final int STRING_BYTES_LEN = 6;
    private Socket socket;
    private InputStream in; 
    private Input kryoInput = null;
    private boolean cont = true;
    private IGeneralTupleSerializer genSer = null;
    private ISwitchTupleSerializer swiSer = null;
    private SynchronizedQueue<IGeneralTuple> syn = null;
    private boolean general = true;
    /**
     * Create a handler receiving the general tuples.
     * @param genSer the serializer for the general tuple
     * @param swiSer the serializer for the switch tuple
     * @param syn the synchronized queue for storing tuples
     * @throws IOException the IO exception
     */
    public TupleReceiverHandler(IGeneralTupleSerializer genSer, ISwitchTupleSerializer swiSer, 
            SynchronizedQueue<IGeneralTuple> syn) {
        this.genSer = genSer;
        this.swiSer = swiSer;
        this.syn = syn;
    }
    
    @Override
    public void run() {
        while (cont) {
            try {
                if (kryoInput != null && kryoInput.canReadInt()) {
                    int len = kryoInput.readInt();
                    byte[] ser = new byte[len];
                    kryoInput.readBytes(ser);
                    if (len == STRING_BYTES_LEN) { //switch the tuple serializer
                        switchMode(new String(ser));
                    } else {
                        enqueue(ser); //enqueue the received tuple
                    }
                }
            } catch (KryoException e) {
                e.printStackTrace();
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
    public void stop() throws IOException {
        if (null != socket) {
            LOGGER.info("Stopping handler");
            cont = false;
            in.close();
            in = null;
            kryoInput.close();
            kryoInput = null;
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
     * Switches the tuple mode based on the received flag.
     * @param mode the received flag
     */
    private void switchMode(String mode) {
        if ("switch".equals(mode)) {
            general = false;
        } else if ("general".equals(mode)) {
            general = true;
        }
    }
    
    /**
     * Enqueue the received tuple.
     * @param ser the tuple bytes
     */
    private void enqueue(byte[] ser) {
        IGeneralTuple tuple;
        if (general) {
            tuple = genSer.deserialize(ser); 
        } else {
            tuple = swiSer.deserialize(ser);
        }
        if (tuple != null) {
            syn.produce(tuple);
        }
    }     
    
}
