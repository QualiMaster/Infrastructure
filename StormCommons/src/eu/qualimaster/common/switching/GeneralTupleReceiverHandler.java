package eu.qualimaster.common.switching;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.serializer.IGeneralTupleSerializer;
/**
 * Tuple handler for receiving general tuples. 
 * @author Cui Qin
 *
 */
public class GeneralTupleReceiverHandler implements ITupleReceiverHandler {
    private static final Logger LOGGER = Logger.getLogger(GeneralTupleReceiverHandler.class);
    private Socket socket;
    private InputStream in; 
    private Input kryoInput = null;
    private boolean cont = true;
    private IGeneralTupleSerializer genSer = null;
    private SynchronizedQueue<IGeneralTuple> syn = null;
    /**
     * Create a handler receiving the general tuples.
     * @param genSer the serializer for the general tuple
     * @param syn the synchronized queue for storing tuples
     * @throws IOException the IO exception
     */
    private GeneralTupleReceiverHandler(IGeneralTupleSerializer genSer, SynchronizedQueue<IGeneralTuple> syn) {
        this.genSer = genSer;
        this.syn = syn;
    }
    
    @Override
    public void run() {
        while (cont) {
            try {
                int len = kryoInput.readInt();
                byte[] ser = new byte[len];
                kryoInput.readBytes(ser);
                IGeneralTuple genTuple = genSer.deserialize(ser); 
                if (genTuple != null) {
                    syn.produce(genTuple);
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

}
