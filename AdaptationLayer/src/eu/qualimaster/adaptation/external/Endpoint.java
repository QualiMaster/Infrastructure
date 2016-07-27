package eu.qualimaster.adaptation.external;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketTimeoutException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import eu.qualimaster.adaptation.external.ExecutionResponseMessage.ResultType;

/**
 * A server endpoint for communicating with the adaptation layer.
 * 
 * @author Holger Eichelberger
 */
public abstract class Endpoint {

    public static final String PROTOCOL_VERSION = "0.5.0";
    protected static final int SO_TIMEOUT = 500;
    private static final int WAIT_TIME = 100;
    private IDispatcher dispatcher;
    private boolean running = true;
    private boolean dispatching = true;
    private Queue<Message> toSend = new ConcurrentLinkedQueue<Message>();
    private Queue<Message> toDispatch = new ConcurrentLinkedQueue<Message>();
    private AtomicInteger readingWorkers = new AtomicInteger(0);
    
    /**
     * Creates an endpoint.
     * 
     * @param dispatcher the message dispatcher
     */
    protected Endpoint(IDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        Thread thread = new Thread(new Dispatcher());
        thread.start();
    }
    
    /**
     * Dispatches one message.
     * 
     * @author Holger Eichelberger
     */
    private class Dispatcher implements Runnable {
        
        // checkstyle: stop exception type check
        
        @Override
        public void run() {
            while (dispatching) {
                synchronized (toDispatch) {
                    if (!toDispatch.isEmpty()) {
                        Message msg = toDispatch.poll();
                        if (msg instanceof AuthenticateMessage) {
                            authenticate((AuthenticateMessage) msg);
                        } else if (msg instanceof ConnectedMessage) {
                            connected((ConnectedMessage) msg); 
                        } else {
                            try {
                                msg.dispatch(dispatcher);
                                dispatched(msg);
                            } catch (Throwable t) {
                                Logging.error("while dispatching " + msg + " " + t.getMessage(), t);
                            }
                        }
                    }
                    try {
                        toDispatch.wait(WAIT_TIME);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        
        // checkstyle: resume exception type check

    }
    
    /**
     * Performs the authentication.
     * 
     * @param msg the authentication message
     */
    protected void authenticate(AuthenticateMessage msg) {
        schedule(new ExecutionResponseMessage(msg, ResultType.FAILED, "Cannot authenticate"));
    }

    /**
     * Called for a connected message.
     * 
     * @param msg the message
     */
    protected void connected(ConnectedMessage msg) {
    }

    /**
     * Is called when <code>msg</code> was dispatched.
     * 
     * @param msg the message
     */
    protected void dispatched(Message msg) {
    }

    /**
     * Marks a worker.
     * 
     * @author Holger Eichelberger
     */
    protected interface Worker extends Runnable {
    }
    
    /**
     * Implements the reading thread receiving and dispatching messages.
     * 
     * @author Holger Eichelberger
     */
    protected class ReadingWorker implements Worker {

        private ObjectInputStream in;
        private boolean receiving = true;
        private Closeable closable;
        
        /**
         * Creates a reading worker for the given stream.
         * 
         * @param in the input stream
         * @param closable the closeable to be closed at the end of the communication
         */
        public ReadingWorker(ObjectInputStream in, Closeable closable) {
            this.in = in;
            this.closable = closable;
        }

        @Override
        public void run() {
            readingWorkers.incrementAndGet();
            while (running && receiving) {
                try {
                    Message msg = (Message) in.readObject();
                    synchronized (toDispatch) {
                        toDispatch.offer(msg);
                        toDispatch.notify();
                    }
                    if (msg.isDisconnect()) {
                        receiving = false;
                    }
                } catch (SocketTimeoutException e) {
                    // this is ok due to non-blocking mode
                }  catch (EOFException e) {
                    receiving = false; // client disconnect
                } catch (IOException e) {
                    Logging.error(e.getMessage());
                    receiving = false;
                } catch (ClassNotFoundException e) {
                    Logging.error(e.getMessage(), e);
                    receiving = false;
                }
            }
            try {
                in.close();
            } catch (IOException e) {
                Logging.error(e.getMessage(), e);
            }
            if (null != closable) {
                try {
                    closable.close();
                } catch (IOException e) {
                    Logging.error(e.getMessage(), e);
                }
            }
            readingWorkers.decrementAndGet();
        }

    }

    /**
     * Implements the writing worker.
     * 
     * @author Holger Eichelberger
     */
    protected class WritingWorker implements Worker, Closeable {

        private ObjectOutputStream out;
        private Closeable closeable;
        private boolean sending = true;
        private Queue<Message> toSend = Endpoint.this.toSend; // basically go for the global send queue

        /**
         * Creates writing worker utilizing the global send queue (client).
         * 
         * @param out the output stream
         * @param closeable to be closed at the end of the communication
         */
        protected WritingWorker(ObjectOutputStream out, Closeable closeable) {
            this(out, closeable, true);
        }

        /**
         * Creates a writing worker with given send queue.
         * 
         * @param out the output stream
         * @param closeable to be closed at the end of the communication
         * @param globalQueue use the global send queue or create a local one
         */
        public WritingWorker(ObjectOutputStream out, Closeable closeable, boolean globalQueue) {
            this.out = out;
            this.closeable = closeable;
            this.toSend = globalQueue ? Endpoint.this.toSend : new ConcurrentLinkedQueue<Message>();
        }
        
        /**
         * Explicitly schedules the given message. This message shall be used in case of local writer worker send 
         * queues.
         * 
         * @param msg the message to be scheduled
         */
        public void schedule(Message msg) {
            toSend.offer(msg);
        }

        @Override
        public void run() {
            while (running && sending) {
                synchronized (toSend) {
                    if (!toSend.isEmpty()) {
                        Message msg = toSend.poll();
                        try {
                            out.writeObject(msg);
                        } catch (SocketTimeoutException e) {
                            // this is ok due to non-blocking mode
                        } catch (IOException e) {
                            Logging.error(e.getMessage());
                            sending = false;
                        }
                    }
                    try {
                        toSend.wait(WAIT_TIME);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (null != closeable) {
                closeable.close();
            }
        }
    }
    
    /**
     * Implements a routing worker to be used if {@link WritingWorker writing workers} operate on own local queues.
     * 
     * @author Holger Eichelberger
     */
    public class RoutingWorker implements Worker {
        
        @Override
        public void run() {
            while (running) {
                synchronized (toSend) {
                    if (!toSend.isEmpty()) {
                        route(toSend.poll());
                    }
                    try {
                        toSend.wait(WAIT_TIME);
                    } catch (InterruptedException e) {
                    }
                }
            }            
        }
        
    }

    /**
     * Routes a message to the respective {@link WritingWorker}. This method is called by {@link RoutingWorker} and 
     * must be implemented if the {@link RoutingWorker} is used. Messages given by <code>msg</code> must be routed
     * or they are lost.
     * 
     * @param msg the message to route
     */
    protected void route(Message msg) {
    }
    
    /**
     * Starts a worker thread.
     * 
     * @param worker the worker
     * @return the started worker thread
     */
    protected static Thread startWorker(Worker worker) {
        Thread thread = new Thread(worker);
        thread.start();
        return thread;
    }
    
    /**
     * Schedules a message for sending. In case of a {@link RequestMessage request message}, the client id and the
     * message id are changed by this method.
     * 
     * @param msg the message to be sent
     */
    public void schedule(Message msg) {
        if (null != msg) {
            if (msg instanceof RequestMessage) {
                RequestMessage rMsg = (RequestMessage) msg;
                rMsg.setMessageId(UUID.randomUUID().toString());
                addMessageInformation((RequestMessage) msg);
            }
            synchronized (toSend) {
                toSend.offer(msg);
                toSend.notify();
            }
        }
    }
    
    /**
     * Adds response message information.
     * 
     * @param msg the message to add information to
     */
    protected abstract void addMessageInformation(RequestMessage msg);
    
    /**
     * Returns whether the endpoint is running.
     * 
     * @return <code>true</code> if the endpoint is running, <code>false</code> else
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Changes the running flag.
     * 
     * @param running the new state of the flag
     */
    protected void setRunning(boolean running) {
        this.running = running;
    }
    
    /**
     * Stops the server endpoint.
     */
    public void stop() {
        dispatching = false;
        running = false;
    }
    
    /**
     * Returns whether this endpoint still has work to be completed.
     * 
     * @return <code>true</code> if there is work to be completed, <code>false</code> else
     */
    public boolean hasWork() {
        return !toSend.isEmpty() || !toDispatch.isEmpty() || readingWorkers.get() > 0; 
    }

}
