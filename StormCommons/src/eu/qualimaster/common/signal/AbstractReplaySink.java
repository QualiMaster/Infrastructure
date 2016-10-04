/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.common.signal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import eu.qualimaster.dataManagement.common.replay.Tuple;
import eu.qualimaster.dataManagement.sinks.replay.ReplayRecorder;
import eu.qualimaster.dataManagement.sinks.replay.ReplayStreamer;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.ReplayChangedMonitoringEvent;

/**
 * The base bolt class for replay sinks. Implementing classes shall register the
 * individual tuple handlers in {@link #registerHandlers(Map, TopologyContext)}.
 * Emitting the data via replay streamers is handled by this class.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractReplaySink extends BaseSignalBolt implements IReplayListener {

    private static final long serialVersionUID = 2348634834739948474L;
    private transient Map<Class<?>, TupleHandler<?>> handlers = new HashMap<Class<?>, TupleHandler<?>>();
    private transient ReplayRunnable replayRunnable;

    /**
     * Implements a tuple handler, i.e., a combination of {@link ReplayRecorder}
     * and related {@link ReplayStreamer streamers}.
     * 
     * @param <T>
     *            the tuple type
     * @author Holger Eichelberger
     */
    private static class TupleHandler<T> {

        private Class<T> tupleClass;
        private Tuple schema;
        private Map<Integer, ReplayStreamer<T>> streamers = Collections
                        .synchronizedMap(new HashMap<Integer, ReplayStreamer<T>>());
        private ReplayRecorder<T> recorder;
        private String location;
        private IStorageStrategyDescriptor strategy;
        private AbstractReplaySink sink;
        private ITupleEmitter<T> emitter;

        /**
         * Creates a replay handler. Call {@link #setEmitter(ITupleEmitter)}
         * afterwards!
         * 
         * @param tupleClass
         *            the tuple class handled by this handler.
         * @param schema
         *            the database meta information (schema) describing
         *            <code>tupleClass</code>
         * @param location
         *            the storage location
         * @param strategy
         *            the storage strategy
         * @param sink
         *            the parent sink
         */
        private TupleHandler(Class<T> tupleClass, Tuple schema, String location, IStorageStrategyDescriptor strategy,
                        AbstractReplaySink sink) {
            this.tupleClass = tupleClass;
            this.schema = schema;
            this.location = location;
            this.strategy = strategy;
            this.sink = sink;
            this.recorder = new ReplayRecorder<T>(tupleClass, schema, location, strategy);
        }

        /**
         * Defines the tuple emitter.
         * 
         * @param emitter
         *            the emitter
         */
        private void setEmitter(ITupleEmitter<T> emitter) {
            this.emitter = emitter;
        }

        /**
         * Stores the given <code>tuple</code> using the recorder.
         * 
         * @param tuple
         *            the tuple
         */
        private void store(Object tuple) {
            if (null != recorder) {
                try {
                    recorder.store(tupleClass.cast(tuple));
                } catch (IOException e) {
                    //getLogger().info("recorder.store error: " + e.getMessage());
                    getLogger().error(e.getMessage(), e);
                }
            } else {
                getLogger().info("recorder is null");
            }
        }

        /**
         * Prepares this handler for shutdown of the pipeline.
         * 
         * @param signal
         *            the pipeline shutdown signal
         */
        private void prepareShutdown(ShutdownSignal signal) {
            if (null != recorder) {
                try {
                    recorder.close();
                } catch (IOException e) {
                    getLogger().error(e.getMessage(), e);
                }
                recorder = null;
            }
            for (Map.Entry<Integer, ReplayStreamer<T>> entry : streamers.entrySet()) {
                close(entry.getValue());
                entry.setValue(null);
            }
            streamers.clear();
        }

        /**
         * Close the given streamer.
         * 
         * @param streamer
         *            the streamer (may be <b>null</b>)
         */
        private void close(ReplayStreamer<T> streamer) {
            if (null != streamer) {
                try {
                    streamer.close();
                } catch (IOException e) {
                    getLogger().error(e.getMessage(), e);
                }
            }
        }

        /**
         * Returns the logger for this class.
         * 
         * @return the logger
         */
        private Logger getLogger() {
            return LogManager.getLogger(getClass());
        }

        /**
         * Notifies this tuple handler about a received replay signal.
         * 
         * @param signal
         *            the signal
         * @return the actual number of replay streamers
         */
        private int notifyReplay(ReplaySignal signal) {
            int result;
            synchronized (streamers) {
                ReplayStreamer<T> streamer = streamers.get(signal.getTicket());
                if (signal.getStartReplay()) {
                    // create streamer for ticket, configure stream
                    if (null == streamer) {
                        streamer = new ReplayStreamer<T>(tupleClass, schema, location, strategy);
                        streamers.put(signal.getTicket(), streamer);
                    }
                    streamer.setStart(signal.getStart());
                    streamer.setEnd(signal.getEnd());
                    streamer.setSpeed(signal.getSpeed());
                    streamer.setQuery(signal.getQuery());
                } else {
                    streamers.remove(signal.getTicket());
                    if (null != streamer) {
                        close(streamer);
                    }
                }
                EventManager.send(new ReplayChangedMonitoringEvent(sink.getPipeline(), sink.getName(),
                                signal.getTicket(), signal.getStartReplay(), signal.getCauseMessageId()));
                result = streamers.size();
            }
            return result;
        }

        /**
         * Streams the tuple for replay.
         */
        private void stream() {
            synchronized (streamers) {
                Iterator<Map.Entry<Integer, ReplayStreamer<T>>> iter = streamers.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Integer, ReplayStreamer<T>> entry = iter.next();
                    ReplayStreamer<T> streamer = entry.getValue();
                    if (!streamer.isEOD()) {
                        iter.remove();
                        close(streamer);
                    } else {
                        T tuple = entry.getValue().getData();
                        if (null != tuple) {
                            emitter.emit(entry.getKey(), tuple);
                        }
                    }
                }
            }
        }

    }

    /**
     * Defines the interface for a replay tuple emitter.
     * 
     * @param <T>
     *            the tuple type
     * @author Holger Eichelberger
     */
    protected interface ITupleEmitter<T> {

        /**
         * Emits the given tuple.
         * 
         * @param ticket
         *            the ticket number
         * @param tuple
         *            the tuple
         */
        public void emit(int ticket, T tuple);

    }

    /**
     * Creates a base signal Bolt.
     * 
     * @param name
     *            the name of the bolt
     * @param namespace
     *            the namespace of the bolt
     * @param sendRegular
     *            whether this monitor shall care for sending regular events (
     *            <code>true</code>) or not (<code>false</code>, for
     *            thrift-based monitoring)
     */
    protected AbstractReplaySink(String name, String namespace, boolean sendRegular) {
        super(name, namespace, sendRegular);
    }

    /**
     * Adds a new tuple handler. This method shall only be called during
     * {@link #registerHandlers(Map,TopologyContext)}.
     * 
     * @param <T>
     *            the tuple type
     * @param tupleClass
     *            the tuple class being handled
     * @param schema
     *            the database meta information (schema) describing
     *            <code>tupleClass</code>
     * @param location
     *            the storage location
     * @param strategy
     *            the storage strategy
     * @param emitter
     *            the tuple emitter
     */
    protected <T> void addTupleHandler(Class<T> tupleClass, Tuple schema, String location,
                    IStorageStrategyDescriptor strategy, ITupleEmitter<T> emitter) {
        TupleHandler<T> handler = new TupleHandler<T>(tupleClass, schema, location, strategy, this);
        handler.setEmitter(emitter);
        if (null == handlers) {
            handlers = new HashMap<Class<?>, TupleHandler<?>>();
        }
        //LogManager.getLogger(getClass()).info("registering class " + tupleClass.toString() + " to handlers");
        handlers.put(tupleClass, handler);
    }

    /**
     * Registers the tuple handlers for this replay sink. Call
     * {@link #addTupleHandler(Class, Tuple, String, IStorageStrategyDescriptor)}
     * .
     * 
     * @param conf
     *            the topology configuration
     * @param context
     *            the topology contect
     */
    @SuppressWarnings("rawtypes")
    protected abstract void registerHandlers(Map conf, TopologyContext context);

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        super.prepare(conf, context, collector);
        registerHandlers(conf, context);
    }

    @Override
    public void notifyReplay(ReplaySignal signal) {
        LogManager.getLogger(getClass()).info("notifying with:" + signal);
        int streamerCount = 0;
        if (null != handlers) {
            for (TupleHandler<?> handler : handlers.values()) {
                streamerCount += handler.notifyReplay(signal);
            }
        }
        if (null == replayRunnable && streamerCount > 0) {
            replayRunnable = new ReplayRunnable();
            Thread t = new Thread(replayRunnable);
            t.start();
        } else if (null != replayRunnable && 0 == streamerCount) {
            replayRunnable.end();
            replayRunnable = null;
        }
    }

    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        super.prepareShutdown(signal);
        if (null != handlers) {
            for (TupleHandler<?> handler : handlers.values()) {
                handler.prepareShutdown(signal);
            }
        }
    }

    /**
     * Emits a data tuple from a replay streamer.
     * 
     * @param ticket
     *            the streamer ticket number
     * @param tuple
     *            the data tuple
     */
    // protected abstract void emit(int ticket, T tuple);

    /**
     * Stores the given <code>tuple</code> using the recorder.
     * 
     * @param tuple
     *            the tuple
     */
    protected void store(Object tuple) {
        if (null != tuple && null != handlers) {
            Class<?> cls = tuple.getClass();
            TupleHandler<?> handler = checkClass(cls);
            if (null != handler && null == handlers.get(cls)) { // speed up lookup
                handlers.put(cls, handler);
            }
            if (null != handler) {
                handler.store(tuple);
            } else {
                LogManager.getLogger(getClass()).info("no handler for " + cls.getName());
            }
        }
    }
    
    /**
     * Checks <code>cls</code> for the first registered handler including super classes and super interfaces.
     * 
     * @param cls the class to check
     * @return the handler or <b>null</b> if none is registered
     */
    private TupleHandler<?> checkClass(Class<?> cls) {
        TupleHandler<?> handler = handlers.get(cls);
        if (null == handler && !cls.isInterface() && null != cls.getSuperclass()) {
            handler = checkClass(cls.getSuperclass());
        }
        if (null == handler) {
            Class<?>[] ifs = cls.getInterfaces();
            for (int i = 0; null == handler && i < ifs.length; i++) {
                handler = checkClass(ifs[i]);
            }
        }
        return handler;
    }

    /**
     * Implements a runnable emitting data from the streamers.
     * 
     * @author Holger Eichelberger
     */
    private class ReplayRunnable implements Runnable {

        private boolean running = true;

        @Override
        public void run() {
            while (running) {
                if (null != handlers) {
                    for (TupleHandler<?> handler : handlers.values()) {
                        handler.stream();
                    }
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }

        /**
         * Ends this runnable.
         */
        private void end() {
            running = false;
        }

    }

    @Override
    public void onSignal(byte[] data) {
        boolean done = ReplaySignal.notify(data, getPipeline(), getName(), this);
        if (!done) {
            super.onSignal(data);
        }
    }

}
