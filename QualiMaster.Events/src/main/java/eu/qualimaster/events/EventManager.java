package eu.qualimaster.events;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.Configuration;

/**
 * Generic event manager for the infrastructure. Multiple event handlers
 * may be registered for one class of event (intended for infrastructure events). Please note that the
 * standard way of using this class is via the static methods. The instance methods are intended for testing 
 * only. <br/>
 * The event manager provides a simple timer mechanism for the client side, i.e., after setting 
 * {@link #setTimerPeriod(long)} regular {@link TimerEvent timer events} are passed to respective
 * event handlers (on the {@link TimerEvent#CHANNEL timer event channel}, consider {@link AbstractTimerEventHandler} for
 * implementation). The timer mechanism is intended to reduce the number of running threads.
 * 
 * @author Holger Eichelberger
 */
public class EventManager {

    public static final String LOG_PREFIX_RECEIVED = "received";
    public static final int SO_TIMEOUT = 5000;
    private static final Logger LOGGER = LogManager.getLogger(EventManager.class);
    private static final EventManager INSTANCE = new EventManager();
    private static final long WRITE_WAIT = 20;
    private static ExecutorService sender = Executors.newCachedThreadPool();
    
    private final String managerId = new VMID().toString() + "-" +  System.nanoTime(); // not static for testing
    private final Map<String, List<EventHandler<? extends IEvent>>> registrations = 
        Collections.synchronizedMap(new HashMap<String, List<EventHandler<? extends IEvent>>>());
    private final Set<Class<? extends IEvent>> disableLogging = Collections.synchronizedSet(
        new HashSet<Class<? extends IEvent>>());
    private ExecutorService executor;
    private AtomicInteger unprocessed = new AtomicInteger();
    private boolean isRunning;
    private ServerSocket serverSocket;
    private BlockingQueue<IEvent> toSend = new LinkedBlockingQueue<IEvent>();
    private Map<String, ClientConnection> clients = new HashMap<String, ClientConnection>();
    private Map<String, EventHandler<? extends IEvent>> clientHandlers 
        = new HashMap<String, EventHandler<? extends IEvent>>();
    private Set<Thread> threads = Collections.synchronizedSet(new HashSet<Thread>());
    private boolean isClient;
    private long timerPeriod;
    private AtomicBoolean initializing = new AtomicBoolean();
    
    /**
     * Registers an event handler.
     * 
     * @param handler the event handler
     */
    public static synchronized void register(EventHandler<? extends IEvent> handler) {
        INSTANCE.doRegister(handler);
    }

    /**
     * Registers an event handler with this event manager.
     * 
     * @param handler the event handler
     */
    public void doRegister(EventHandler<? extends IEvent> handler) {
        Class<? extends IEvent> eClass = handler.handles();
        List<EventHandler<? extends IEvent>> handlers = registrations.get(handler.getEventClassName());
        if (null == handlers) {
            handlers = new ArrayList<EventHandler<? extends IEvent>>();
            registrations.put(handler.getEventClassName(), handlers);
        }
        if (isClient) {
            if (null == executor) {
                executor = Executors.newCachedThreadPool();
            }
            if (!ILocalEvent.class.isAssignableFrom(eClass)) {
                // this is one, we may have multiple forward handlers for different events
                doSend(new ForwardHandlerEvent(managerId, handler.getEventClassName()));
                ClientConnection conn = clients.get(managerId); // get own socket
                if (null != conn) {
                    clients.remove(managerId); // start the reading worker only the first time
                    Socket socket = conn.getSocket();
                    if (null != socket) {
                        try {
                            // this is a client, inform the server for receiving events but register the handler
                            ReadingWorker worker = new ReadingWorker(socket, true);
                            startThread(worker);
                        } catch (IOException e) {
                            LOGGER.error("While setting up event forwarding: " + e.getMessage());
                        }
                    }
                }
            }
        } 
        handlers.add(handler);
    }
    
    /**
     * Starts and registers a thread for a runnable.
     * 
     * @param runnable the thread runnable
     * @return the created thread
     */
    private Thread startThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        threads.add(thread);
        return thread;
    }

    /**
     * Notifies about ending the current thread.
     */
    private void notifyThreadEnd() {
        notifyThreadEnd(Thread.currentThread());
    }
    
    /**
     * Notifies about ending a thread.
     * 
     * @param thread the thread to end
     */
    private void notifyThreadEnd(Thread thread) {
        threads.remove(thread);
    }

    /**
     * Unregisters an event handler.
     * 
     * @param handler the event handler
     */
    public static synchronized void unregister(EventHandler<? extends IEvent> handler) {
        INSTANCE.doUnregister(handler);
    }
    
    /**
     * Unregisters an event handler from this event manager.
     * 
     * @param handler the event handler
     */
    public void doUnregister(EventHandler<? extends IEvent> handler) {
        List<EventHandler<? extends IEvent>> handlers = registrations.get(handler.getEventClassName());
        if (null != handlers) {
            handlers.remove(handler);
        }
    }

    /**
     * Handles a given event. Ignores the event if the event manager was not started.
     * 
     * @param event the event to be handled.
     */
    public static void handle(IEvent event) {
        INSTANCE.doHandle(event);
    }
    
    /**
     * Sends events asynchronously.
     * 
     * @param event the event to be sent.
     */
    public static void asyncSend(final IEvent event) {
        INSTANCE.doAsyncSend(event);
    }

    /**
     * Sends events asynchronously.
     * 
     * @param event the event to be sent.
     */
    public void doAsyncSend(final IEvent event) {
        sender.execute(new Runnable() {

            @Override
            public void run() {
                doSend(event);
            }
            
        });
    }

    /**
     * Sends a given event. If the {@link EventManager} was not started,
     * it will try connecting to its {@link Configuration configured} server.
     * 
     * @param event the event to be sent.
     */
    public static void send(IEvent event) {
        INSTANCE.doSend(event);
    }
    
    /**
     * Sets the client timer period.
     * 
     * @param period the period (no time if not positive)
     */
    public static void setTimerPeriod(long period) {
        INSTANCE.doSetTimerPeriod(period);
    }
    
    /**
     * Sets the client timer period.
     * 
     * @param period the period in ms (no timer events if not positive, negative values become zero, 
     *     must be greater then {@link #WRITE_WAIT})
     */
    public void doSetTimerPeriod(long period) {
        if (isClient) {
            if (period > WRITE_WAIT) {
                this.timerPeriod = period - WRITE_WAIT;
            } else {
                this.timerPeriod = 0;
            }
        }
    }
    
    /**
     * Returns the timer period (client side only).
     * 
     * @return the client-side timer period
     */
    public static long getTimerPeriod() {
        return INSTANCE.doGetTimerPeriod();
    }
    
    /**
     * Returns the timer period (client side only).
     * 
     * @return the client-side timer period
     */
    public long doGetTimerPeriod() {
        long result;
        if (timerPeriod > 0) {
            result = timerPeriod + WRITE_WAIT;
        } else {
            result = 0;
        }
        return result;
    }

    /**
     * Handles a given event. If the {@link EventManager} was not started,
     * it will try connecting to its {@link Configuration configured} server.
     * 
     * @param event the event to be handled.
     */
    public void doSend(IEvent event) {
        if (null != event) {
            if (!isRunning) {
                doStart(isLocalhost(Configuration.getEventHost()), false);
            }
            doHandle(event);
        }
    }
    
    /**
     * Handles a given event. Ignores the event if the event manager was not started.
     * 
     * @param event the event to be handled.
     */
    public void doHandle(IEvent event) {
        if (event instanceof IReturnableEvent) {
            fillReturnInfo((IReturnableEvent) event);
        }
        doHandleImpl(event);
    }

    /**
     * Handles a given event. Ignores the event if the event manager was not started.
     * 
     * @param event the event to be handled.
     */
    private void doHandleImpl(IEvent event) {
        if (null != event) {
            if (isLoggingEnabled(event)) {
                LOGGER.info(LOG_PREFIX_RECEIVED + " " + event);
            }
            if (null != toSend) { // client
                try {
                    toSend.put(event);
                } catch (InterruptedException e) {
                }
            } else {
                boolean done = false;
                if (event instanceof IResponseEvent) {
                    done = doHandleResponse((IResponseEvent) event);
                }                
                if (!done) {
                    doHandleLocal(event);
                }
            }
        }
    }
    
    /**
     * Handles a response event.
     * 
     * @param event the event to handle
     * @return <code>true</code> if the event was fully handled, <code>true</code> if information was missing and
     *    the event needs to be handled in a different way
     */
    private boolean doHandleResponse(IResponseEvent event) {
        boolean done = false;
        String receiverId = ((IResponseEvent) event).getReceiverId();
        if (null != receiverId && !receiverId.equals(managerId)) { // local
            EventHandler<?> handler = clientHandlers.get(receiverId);
            if (null != handler && !handler.consume(event)) {
                executor.execute(new DispatchRunnable(handler, event));
                done = true;
            }
        }
        return done;
    }
    
    /**
     * Handles events locally, i.e., without offering them to the send queue and sending them. This is required
     * for handling forward events on client side.
     * 
     * @param event the event to be handled.
     */
    private void doHandleLocal(IEvent event) {
        if (null != executor) {
            Set<EventHandler<? extends IEvent>> handlers = getHandlers(event.getClass(), event.getChannel(), null);
            if (null != handlers) {
                try {
                    for (EventHandler<? extends IEvent> handler : handlers) {
                        unprocessed.incrementAndGet();
                        boolean consume = handler.consume(event);
                        if (isLoggingEnabled(event)) {
                            LOGGER.info((consume ? "consumed" : "dispatching") + " " + event);
                        }
                        if (!consume) {
                            executor.execute(new DispatchRunnable(handler, event));
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    // a handler was added/removed during execution - just ignore
                }
            }
        }
    }
    
    /**
     * Returns whether <code>host</code> is localhost.
     * 
     * @param host the host to test
     * @return <code>true</code> if <code>host</code> is localhost, <code>false</code> else
     */
    private static boolean isLocalhost(String host) {
        boolean result = false;
        if (null != host) {
            String tmp = host.toLowerCase();
            if ("localhost".equals(tmp) || "127.0.0.1".equals(tmp) || "::1".equals(tmp)) {
                result = true;
            } else {
                try {
                    InetAddress localHostaddr = InetAddress.getLocalHost();
                    if (host.equals(localHostaddr.getHostName().toLowerCase())
                            || host.equals(localHostaddr.getCanonicalHostName().toLowerCase())
                            || host.equals(localHostaddr.getHostAddress().toLowerCase())) {
                        result = true;
                    }
                } catch (UnknownHostException e) {
                    // unknown != localhost
                }
            }
        }
        return result;
    }
    
    /**
     * Returns all handlers for <code>cls</code>.
     * 
     * @param cls the event class to search for
     * @param channel the event channel name (may be <b>null</b> for all)
     * @param handlers the actual handlers (may be <b>null</b> at call)
     * @return the handlers for <code>cls</code> (may be <b>null</b> if there are none)
     */
    private Set<EventHandler<? extends IEvent>> getHandlers(Class<?> cls, String channel,  
        Set<EventHandler<? extends IEvent>> handlers) {
        List<EventHandler<? extends IEvent>> tmp = registrations.get(cls.getName());
        if (null != tmp) {
            if (null == handlers) {
                handlers = new HashSet<EventHandler<? extends IEvent>>();
            }
            for (int t = 0; t < tmp.size(); t++) {
                EventHandler<? extends IEvent> handler = tmp.get(t);
                if (handler.handlesChannel(channel)) {
                    handlers.add(handler);
                }
            }
        }
        if (null != cls.getSuperclass()) {
            handlers = getHandlers(cls.getSuperclass(), channel, handlers);
        }
        Class<?>[] ifaces = cls.getInterfaces();
        for (int i = 0, n = ifaces.length; i < n; i++) {
            handlers = getHandlers(ifaces[i], channel, handlers);
        }
        return handlers;
    }

    /**
     * Dispatches the event to an event handler.
     * 
     * @author Holger Eichelberger
     */
    private class DispatchRunnable implements Runnable {
        
        private EventHandler<?> handler;
        private IEvent event;
        
        /**
         * Creates the runnable.
         * 
         * @param handler the event handler for dispatching the event
         * @param event the event
         */
        DispatchRunnable(EventHandler<?> handler, IEvent event) {
            this.handler = handler;
            this.event = event;
        }

        @Override
        public void run() {
            handler.doHandle(event);
            unprocessed.decrementAndGet();
        }
        
    }
    
    /**
     * Returns the number of unprocessed messages. [testing]
     * 
     * @return the number of unprocessed messages
     */
    public static int unprocessed() {
        return INSTANCE.getUnprocessed();
    }

    /**
     * Returns the number of unprocessed messages. [testing]
     * 
     * @return the number of unprocessed messages
     */
    public int getUnprocessed() {
        return unprocessed.get();
    }
    
    /**
     * Initializes the legacy local mode. Works only if not already started.
     */
    public static void initLegacy() {
        if (!INSTANCE.isRunning) {
            INSTANCE.toSend = null; // legacy
        }
    }

    /**
     * Starts the event manager.
     */
    public static void start() {
        initLegacy();
        start(true, false); // legacy, keep in local mode
    }

    /**
     * Starts the event manager.
     * 
     * @param localMode run only on the localhost
     * @param server start as server (with remote receiver thread)
     */
    public static void start(boolean localMode, boolean server) {
        INSTANCE.doStart(localMode, server);
    }
    
    /**
     * Starts the event bus as server.
     */
    public static void startServer() {
        start(false, true);
    }
    
    /**
     * A handler for the internal forward events.
     * 
     * @author Holger Eichelberger
     */
    private class ForwardReceptionEventHandler extends EventHandler<ForwardHandlerEvent> {

        /**
         * Creates a forward event handler.
         */
        protected ForwardReceptionEventHandler() {
            super(ForwardHandlerEvent.class);
        }

        @Override
        protected void handle(ForwardHandlerEvent event) {
            String id = event.getClientId();
            ClientConnection conn = clients.get(id);
            if (null != conn) {
                try {
                    ForwardEventHandler handler = new ForwardEventHandler(event.getEventClass(), conn);
                    register(handler);
                    clientHandlers.put(id, handler);
                } catch (IOException e) {
                    LOGGER.error("cannot create forward event handler for " + event.getClientId() + " "
                        + event.getEventClass() + ":" + e.getMessage());
                }
            } else {
                LOGGER.error("cannot create forward event handler for " + event.getClientId() + " " 
                    + event.getEventClass() + ": no such client");
            }
        }
        
    }
    
    /**
     * Forwards events to a client.
     * 
     * @author Holger Eichelberger
     */
    private class ForwardEventHandler extends EventHandler<IEvent> {

        private ClientConnection conn;
        private String eventClass;

        /**
         * Forwards events to a client.
         * 
         * @param eventClass the event class
         * @param conn the client connection
         * @throws IOException if the event handler cannot be created due to I/O problems
         */
        protected ForwardEventHandler(String eventClass, ClientConnection conn) throws IOException {
            super(IEvent.class);
            this.eventClass = eventClass;
            this.conn = conn;
            conn.getStream(); // if not open, force sending the header
        }
        
        @Override
        String getEventClassName() {
            return eventClass;
        }

        @Override
        protected void handle(IEvent event) {
            boolean unregister = false;
            try {
                ObjectOutputStream out = conn.getStream();
                if (null == out) {
                    unregister = true;
                } else {
                    try {
                        synchronized (out) { // multiple forward handlers may share out
                            out.writeObject(event);
                        }
                    } catch (EOFException | SocketException e) {
                        // client disconnected
                        unregister = true;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("handling " + getEventClassName() + ": " + e.getMessage(), e);
            }
            if (unregister) {
                String id = conn.getClientId();
                clients.remove(id);
                clientHandlers.remove(id);
                unregister(this);
                conn.close();
            }
        }
        
    }

    /**
     * Starts this event manager.
     * 
     * @param localMode run only on the localhost
     * @param server start as server (with remote receiver thread)
     */
    public void doStart(boolean localMode, boolean server) {
        boolean inInit = initializing.getAndSet(true);
        if (!isRunning && !inInit) {
            if (server) {
                toSend = null;
                executor = Executors.newCachedThreadPool();
                if (!localMode) {
                    try {
                        serverSocket = new ServerSocket(Configuration.getEventPort());
                        serverSocket.setSoTimeout(SO_TIMEOUT); // enable non-blocking accepts
                        isRunning = true;
                        startThread(new ServerRunnable());
                        register(new ForwardReceptionEventHandler());
                        LOGGER.info("Event manager started in server mode on port " 
                            + Configuration.getEventPort() + ".");
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage() + "Event manager not started.");
                    }
                } else {
                    isRunning = true;
                }
            } else {
                String conn = Configuration.getEventHost() + "/" + Configuration.getEventPort();
                try {
                    Socket s = createClientSocket();
                    if (null == toSend) {
                        toSend = new LinkedBlockingQueue<IEvent>();
                    }
                    // store for forwarding, will be removed if forwarding is enabled
                    clients.put(managerId, new ClientConnection(managerId, s)); 
                    WritingWorker worker = new WritingWorker(s);
                    worker.sendId();
                    isRunning = true;
                    startThread(worker);
                    LOGGER.info(" Event manager started in client mode (server " + conn + ").");
                    isClient = true;
                } catch (IOException e) {
                    if (localMode) {
                        executor = Executors.newCachedThreadPool();
                        isRunning = true;
                    } else {
                        LOGGER.error(e.getMessage() + " - Event manager client not started (server " + conn + ").");
                    }
                }
            }
            disableLoggingFor(TimerEvent.class); // don't show internal regular events which can be consumed locally
            disableLoggingFor(Configuration.getEventDisableLogging());
            initializing.set(false);
        }
    }

    /**
     * Creates a client socket.
     * 
     * @return the client socket
     * @throws IOException in case that creating the client socket fails
     */
    private static Socket createClientSocket() throws IOException {
        Socket s = new Socket(InetAddress.getByName(Configuration.getEventHost()), 
            Configuration.getEventPort());
        s.setKeepAlive(true);
        s.setSoTimeout(SO_TIMEOUT); // enable non-blocking communication, also for properly ending threads
        return s;
    }
    
    /**
     * Stops the event manager. May take {@link #SO_TIMEOUT} time.
     */
    public static void stop() {
        INSTANCE.doStop();
    }

    /**
     * Clears the handler registrations.
     */
    public static void clearRegistrations() {
        INSTANCE.doClearRegistrations();
    }

    /**
     * Clears the handler registrations.
     */
    public void doClearRegistrations() {
        registrations.clear();
    }

    /**
     * Stops this event manager. May take {@link #SO_TIMEOUT} time.
     */
    public synchronized void doStop() {
        if (isRunning) {
            isRunning = false;
            if (null != executor) {
                executor.shutdown();
                executor = null;
            }
            clients.clear();
            clientHandlers.clear();
            timerPeriod = 0;
            
            // wait for end of threads (may take 500 ms due to SOTimeouts), but not longer than 4*SO_TIMEOUT
            long timestamp = System.currentTimeMillis();
            while (!threads.isEmpty() && (System.currentTimeMillis() - timestamp) < SO_TIMEOUT * 4) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            if (null != toSend) {
                toSend.clear();
            }
        }
    }

    /**
     * Cleans up the event processing by waiting and blocking until all events are processed.
     */
    public static void cleanup() {
        INSTANCE.doCleanup();
    }

    /**
     * Cleans up the event processing by waiting and blocking until all events are processed.
     */
    public void doCleanup() {
        while (unprocessed.get() > 0 || (null != toSend && !toSend.isEmpty())) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }        
    }
    
    /**
     * Implements a reconnectable server process.
     * 
     * @author Holger Eichelberger
     */
    private class ServerRunnable implements Runnable {

        @Override
        public void run() {
            while (isRunning) {
                ReadingWorker worker = null;
                Thread thread = null;
                Socket s = null;
                try {
                    s = serverSocket.accept();
                    LOGGER.info("accepted event connection from " + s.getRemoteSocketAddress());
                    worker = new ReadingWorker(s, false);
                    thread = startThread(worker);
                } catch (SocketTimeoutException e) {
                    // this is ok due to non-blocking mode
                } catch (EOFException e) {
                    LOGGER.error("End of file exception: " + (null == s ? "unknown" : s.getRemoteSocketAddress()));
                    closeReadingWorker(thread, worker);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            notifyThreadEnd();
        }
    }
    
    /**
     * Closes a reading worker and it's thread.
     * 
     * @param thread the thread (may be <b>null</b>)
     * @param worker the worker (may be <b>null</b>)
     */
    private void closeReadingWorker(Thread thread, ReadingWorker worker) {
        if (null != worker) {
            worker.stop();
        }
        if (null != thread) {
            notifyThreadEnd(thread);
        }
    }
    
    /**
     * Implements the reading thread receiving and dispatching messages.
     * 
     * @author Holger Eichelberger
     */
    private class ReadingWorker implements Runnable {

        private Socket socket;
        private ObjectInputStream in;
        private boolean isReading = true;
        private String clientId;
        private boolean handleLocal;
        
        /**
         * Creates a reading worker.
         * 
         * @param socket the socket to listen to / read from
         * @param handleLocal whether the event shall potentially be sent or handled locally (forward reception)
         * @throws IOException if the stream cannot be opened
         */
        public ReadingWorker(Socket socket, boolean handleLocal) throws IOException {
            this.socket = socket;
            this.handleLocal = handleLocal;
            in = new ObjectInputStream(socket.getInputStream());
        }
        
        @Override
        public void run() {
            while (!handleLocal && isRunning && null == clientId && isReading) {
                try {
                    if (in.available() > 0) {
                        clientId = in.readUTF();
                        clients.put(clientId, new ClientConnection(clientId, socket));
                    } // not ready
                } catch (IOException e) {
                    // not ready
                }
                if (null == clientId) {
                    sleep(10);
                }
            }
            while (isRunning && isReading) {
                IEvent event = null;
                try {
                    event = (IEvent) in.readObject();
                } catch (SocketTimeoutException e) {
                    // this is ok due to non-blocking mode
                } catch (EOFException | SocketException e) {
                    // client disconnected, connection timeout
                    isReading = false;
                } catch (IOException e) {
                    //LOGGER.error(e.getMessage(), e);
                    isReading = false;
                } catch (ClassNotFoundException e) {
                    LOGGER.error(e.getMessage(), e);
                    isReading = false;
                }
                if (null != event) {
                    if (handleLocal) {
                        doHandleLocal(event);
                    } else {
                        doHandleImpl(event);
                    }
                }
                sleep(20);
            }
            try {
                in.close();
                socket.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            notifyThreadEnd();
        }
        
        /**
         * Allows stopping the worker immediately.
         */
        private void stop() {
            isReading = false;
        }
        
    }
    
    /**
     * Sleeps for a given time.
     * 
     * @param ms the time to sleep
     */
    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Fills a returnable event with return information.
     * 
     * @param event the event to fill
     */
    private void fillReturnInfo(IReturnableEvent event) {
        event.setSenderId(managerId);
        event.setMessageId(UUID.randomUUID().toString());
    }

    /**
     * Implements the writing thread for sending messages.
     * 
     * @author Holger Eichelberger
     */
    private class WritingWorker implements Runnable {

        private Socket socket;
        private ObjectOutputStream out;
        private long lastTimerEvent;

        /**
         * Creates a writing worker.
         * 
         * @param socket the socket to listen to / read from
         * @throws IOException if the stream cannot be opened
         */
        private WritingWorker(Socket socket) throws IOException {
            this.socket = socket;
            out = new ObjectOutputStream(socket.getOutputStream());
        }
        
        /**
         * Sends the ID of the virtual machine this event manager is running within.
         * 
         * @throws IOException in case of I/O problems
         */
        public void sendId() throws IOException {
            out.writeUTF(managerId);
        }
        
        @Override
        public void run() {
            boolean workerRunning = true;
            while (isRunning && workerRunning) {
                try {
                    if (!toSend.isEmpty()) { // needed to allow thread to be terminated
                        IEvent event = toSend.take();
                        out.writeObject(event);
                        if (isLoggingEnabled(event)) {
                            LOGGER.info("sending " + event);
                        }
                    }
                    Thread.sleep(WRITE_WAIT);
                } catch (SocketException e) {
                    workerRunning = false;
                } catch (InterruptedException e) {
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (timerPeriod > 0) {
                    long now = System.currentTimeMillis();
                    if (0 == lastTimerEvent || (now - lastTimerEvent) > timerPeriod) {
                        doHandleLocal(TimerEvent.INSTANCE);
                        lastTimerEvent = now;
                    }
                }
            }
            try {
                out.close();
                socket.close();
            } catch (SocketException e) {
              // already done  
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            notifyThreadEnd();
        }
        
    }
    
    /**
     * Implements an auto-reconnecting simple event connection for sending events directly to the event bus. This class 
     * is intended to reduce the overall number of threads as it sends events directly. However, do not use instances 
     * of this class together with event bus clients in the same JVM.
     * 
     * @author Holger Eichelberger
     */
    public static class EventSender {

        private Socket socket;
        private ObjectOutputStream out;

        /**
         * Creates an event sender for the event bus.
         */
        public EventSender() {
            connect();
        }

        /**
         * Tries connecting to the event bus.
         */
        private void connect() {
            try {
                this.socket = createClientSocket();
                out = new ObjectOutputStream(socket.getOutputStream());
                out.writeUTF(INSTANCE.managerId);
            } catch (IOException e) {
                close();
            }
        }
        
        /**
         * Sends an event to the event bus.
         * 
         * @param event the event
         */
        public void send(IEvent event) {
            if (null == out) {
                connect();
            }
            if (null != out) {
                try {
                    out.writeObject(event);
                    if (INSTANCE.isLoggingEnabled(event)) {
                        LOGGER.info("sending " + event);
                    }
                } catch (IOException e) {
                    close();
                }
            }
        }

        /**
         * Closes the event sender.
         */
        public void close() {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            if (null != socket) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        
    }

    /**
     * Disables logging for a list of classes given as comma-separated class names.
     * 
     * @param classNames the class names to disable logging for
     */
    @SuppressWarnings("unchecked")
    public static void disableLoggingFor(String classNames) {
        if (null != classNames && classNames.length() > 0) {
            StringTokenizer tokenizer = new StringTokenizer(classNames, ",");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken().trim();
                try {
                    Class<?> cls = Class.forName(token);
                    if (IEvent.class.isAssignableFrom(cls)) {
                        disableLoggingFor((Class<? extends IEvent>) cls);
                    } else {
                        LOGGER.info("disabling event logging: class " + token + " is not an event class. Ignored.");    
                    }
                } catch (ClassNotFoundException e) {
                    LOGGER.info("disabling event logging: cannot find class " + token + ". Ignored.");
                }
            }
        }
    }
    
    /**
     * Disables logging for the given event class.
     * 
     * @param cls the event class
     */
    public static void disableLoggingFor(Class<? extends IEvent> cls) {
        INSTANCE.doDisableLoggingFor(cls);
    }

    /**
     * Disables logging for the given event class on this event manager.
     * 
     * @param cls the event class
     */
    public void doDisableLoggingFor(Class<? extends IEvent> cls) {
        if (null != cls) {
            disableLogging.add(cls);
        }
    }
    
    /**
     * Clears logging settings for the given event class.
     * 
     * @param cls the event class
     */
    public static void clearLoggingSettingsFor(Class<? extends IEvent> cls) {
        INSTANCE.doClearLoggingSettingsFor(cls);
    }
    
    /**
     * Clears logging settings for the given event class on this event manager.
     * 
     * @param cls the event class
     */
    public void doClearLoggingSettingsFor(Class<? extends IEvent> cls) {
        if (null != cls) {
            disableLogging.remove(cls);
        }
    }

    /**
     * Returns whether logging is enabled for the given event class.
     * 
     * @param event the event to check for
     * @return <code>true</code> if logging is enabled, <code>false</code> else
     */
    public static boolean shallBeLogged(IEvent event) {
        return INSTANCE.isLoggingEnabled(event);
    }
    
    /**
     * Returns whether logging is enabled for the given event class on this manager.
     * 
     * @param event the event to check for
     * @return <code>true</code> if logging is enabled, <code>false</code> else
     */
    public boolean isLoggingEnabled(IEvent event) {
        boolean result;
        if (null == event) {
            result = false;
        } else {
            result = !disableLogging.contains(event.getClass());
        }
        return result;
    }
    
    /**
     * Returns the event manager instance.
     * 
     * @return the instance
     */
    public static EventManager getInstance() {
        return INSTANCE;
    }
    
}