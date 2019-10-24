package eu.qualimaster.common.switching.tupleReceiving;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;
import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.switching.SynchronizedQueue;
import eu.qualimaster.common.switching.actions.GoToActiveINTAction;
import eu.qualimaster.common.switching.actions.IAction;
import eu.qualimaster.common.switching.actions.SwitchStates;
import eu.qualimaster.common.switching.actions.SwitchStates.ActionState;

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
    private AbstractSignalConnection signalCon;
    private Map<ActionState, List<IAction>> actionMap;
    private Socket socket;
    private InputStream in;
    private Input kryoInput = null;
    private boolean cont = true;

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
     * @param signalCon the signal connection used to send signals
     * @param actionMap the map containing the switch actions
     * @throws IOException
     *             IO exception
     */
    public SeparatedTupleReceiverHandler(SynchronizedQueue<ISwitchTuple> synInQueue,
            SynchronizedQueue<ISwitchTuple> synTmpQueue, ISwitchTupleSerializer serializer, 
            AbstractSignalConnection signalCon, Map<ActionState, List<IAction>> actionMap) throws IOException {
        this.synInQueue = synInQueue;
        this.synTmpQueue = synTmpQueue;
        this.serializer = (KryoSwitchTupleSerializer) serializer;
        this.signalCon = signalCon;
        this.actionMap = actionMap;
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
                    // LOGGER.info(System.currentTimeMillis() + ", Received the
                    // data with id :" + switchTuple.getId()
                    // + ", firstId: " + SwitchStates.getFirstId() + ", is
                    // transferred data? "
                    // + (switchTuple.getId() > SwitchStates.getFirstId()));
                    if (switchTuple.getId() > SwitchStates.getFirstTupleId() || switchTuple.getId() == 0) {
                        synInQueue.produce(switchTuple);
                        LOGGER.info(System.currentTimeMillis() + ", inQueue-Received data with id: "
                                + switchTuple.getId() + ", firstId:" + SwitchStates.getFirstTupleId());
                    } else { // will be only executed in the target one
                        synTmpQueue.produce(switchTuple);
                        LOGGER.info(System.currentTimeMillis() + ", tmpQueue-Received the transferred data with id: "
                                + switchTuple.getId() + ", firstId:" + SwitchStates.getFirstTupleId());
                        if (synOnce) {
                            synOnce = false;
                            SwitchStates.executeActions(ActionState.FIRST_TRANSFERRED_DATA_ARRIVED, actionMap, null);
                        }
                        if (switchTuple.getId() == SwitchStates.getFirstTupleId()) {
                            LOGGER.info(System.currentTimeMillis() + ", reached the last transferred data, firstId:"
                                    + SwitchStates.getFirstTupleId());
                            new GoToActiveINTAction(signalCon).execute();
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
