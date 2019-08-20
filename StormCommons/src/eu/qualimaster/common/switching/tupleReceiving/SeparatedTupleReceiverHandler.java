package eu.qualimaster.common.switching.tupleReceiving;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;
import eu.qualimaster.common.signal.AbstractSignalStrategy;
import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.SynchronizedQueue;

/**
 * An handler for receiving tuples for the warm-up switch with data
 * synchronization.
 * 
 * @author Cui Qin
 *
 */
public class SeparatedTupleReceiverHandler implements ITupleReceiverHandler {
    private static final Logger LOGGER = Logger.getLogger(SeparatedTupleReceiverHandler.class);
    private static boolean synOnce = true;
    private SynchronizedQueue<ISwitchTuple> synInQueue;
    private SynchronizedQueue<ISwitchTuple> synTmpQueue;
    private KryoSwitchTupleSerializer serializer;
    private Socket socket;
    private InputStream in;
    private Input kryoInput = null;
    private boolean cont = true;
    private AbstractSignalStrategy signalStrategy;

    /**
     * Constructor for the tuple receiving handler for the warm-up switch with
     * data synchronization.
     * 
     * @param synInQueue
     *            the synchronized input queue to store the regular tuples
     * @param synTmpQueue
     *            the synchronized temporary queue to store the transferred
     *            tuples
     * @param serializer
     *            the tuple serializer
     * @throws IOException
     *             IO exception
     */
    public SeparatedTupleReceiverHandler(SynchronizedQueue<ISwitchTuple> synInQueue,
            SynchronizedQueue<ISwitchTuple> synTmpQueue, ISwitchTupleSerializer serializer)
                    throws IOException {
        this.synInQueue = synInQueue;
        this.synTmpQueue = synTmpQueue;
        this.serializer = (KryoSwitchTupleSerializer) serializer;
        signalStrategy = (AbstractSignalStrategy) SwitchStrategies.getInstance().getStrategies().get("signal");
    }

    @Override
    public void run() {
        while (cont && kryoInput != null && serializer != null) {
            try {
                int len = kryoInput.readInt();
                byte[] ser = new byte[len];
                kryoInput.readBytes(ser);
                ISwitchTuple switchTuple = serializer.deserialize(ser);
                if (switchTuple != null) {
                    LOGGER.info(System.currentTimeMillis() + ", Received the data with id :" + switchTuple.getId()
                            + ", firstId: " + SignalStates.getFirstId() + ", is transferred data? "
                            + (switchTuple.getId() > SignalStates.getFirstId()));
                    if (switchTuple.getId() > SignalStates.getFirstId() || switchTuple.getId() == 0) {
                        synInQueue.produce(switchTuple);
                    } else { // will be only executed in the target one
                        synTmpQueue.produce(switchTuple);
                        LOGGER.info(System.currentTimeMillis() + ", Received the transferred data with id: "
                                + switchTuple.getId() + ", firstId:" + SignalStates.getFirstId());
                        if (synOnce) {
                            synOnce = false;
                            signalStrategy.synchronizeEarly();
                        }
                        if (switchTuple.getId() == SignalStates.getFirstId()) {
                            LOGGER.info(System.currentTimeMillis() + ", reached the last transferred data, firstId:"
                                    + SignalStates.getFirstId());
                            // goToActive(); //TODO
                        }
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

}
