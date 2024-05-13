/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.logReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.IEvent;

/**
 * Implements a log reader. Call {@link #read()} for reading a log file and processing the registered
 * events. Finally, call {@link #close()}. Expects HH:mm:ss.SSS as date format (see {@link #setDateFormat(DateFormat)}).
 * 
 * @author Holger Eichelberger
 */
public class LogReader {

    private static final Map<String, EventReader<?>> REGISTERED = new HashMap<String, EventReader<?>>();
    private Map<String, EventReader<?>> readers = new HashMap<String, EventReader<?>>();
    private LineNumberReader reader;
    private EventProcessor<?> processor;
    //private DateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
    private DateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private int maxEventCount;
    private int maxLineCount;
    private int eventCount;
    private boolean considerTime = false;
    private long currentTime; 

    /**
     * The strategy class for handling a certain kind of detected events. 
     * 
     * @param <E> the actual event type
     * @author Holger Eichelberger
     */
    public abstract static class EventProcessor <E extends IEvent> {
        
        private Class<E> cls;

        /**
         * Creates an event processor. The given class automatically acts as event filter.
         * 
         * @param cls the event class
         */
        public EventProcessor(Class<E> cls) {
            this.cls = cls;
        }
        
        /**
         * Tries to handle <code>event</code>. If event is assignable from the event class, then the event is 
         * passed to {@link #accept()} and in case of acceptance to {@link #process(IEvent)}.
         * 
         * @param event the event
         * @return <code>true</code> if <code>event</code> has been handled, <code>false</code> else
         */
        boolean handle(IEvent event) {
            boolean handled = false;
            if (cls.isInstance(event)) {
                E evt = cls.cast(event);
                if (accept(evt)) {
                    process(evt);
                    handled = true;
                }
            }
            return handled;
        }
        
        /**
         * Filter for events.
         * 
         * @param event the event
         * @return <code>true</code> if the event shall be processed / accepted, <code>false</code> else
         */
        protected boolean accept(E event) {
            return true;
        }
        
        /**
         * Processes the event.
         * 
         * @param event the event
         */
        protected abstract void process(E event);
        
    }
    
    // default event readers
    static {
        register(PipelineElementMultiObservationMonitoringEventReader.class);
        register(AlgorithmChangedMonitoringEventReader.class);
    }
    
    /**
     * Creates a log reader with default logging to standard streams.
     * 
     * @param file the log file
     * @param processor the handling event processor
     * @throws IOException in case of I/O problems
     */
    public LogReader(File file, EventProcessor<?> processor) throws IOException {
        this(new FileReader(file), processor);
    }

    /**
     * Creates a log reader with default logging to standard streams.
     * 
     * @param in the input reader
     * @param processor the handling event processor
     */
    public LogReader(Reader in, EventProcessor<?> processor) {
        reader = new LineNumberReader(in);
        this.processor = processor;
        this.readers.putAll(REGISTERED);
    }
    
    /**
     * Registers an event reader globally.
     * 
     * @param cls the event reader class (<b>null</b> is ignored)
     * @throws IllegalArgumentException in case that the event reader cannot be instantiated
     */
    public static void register(Class<? extends EventReader<?>> cls) throws IllegalArgumentException {
        register(cls, REGISTERED);
    }

    /**
     * Registers an event reader in this log reader.
     * 
     * @param cls the event reader class (<b>null</b> is ignored)
     * @throws IllegalArgumentException in case that the event reader cannot be instantiated
     */
    public void doRegister(Class<? extends EventReader<?>> cls) throws IllegalArgumentException {
        register(cls, readers);
    }

    /**
     * Registers an event reader.
     * 
     * @param cls the event reader class (<b>null</b> is ignored)
     * @param readers the reader map to modify as a side effect
     * @throws IllegalArgumentException in case that the event reader cannot be instantiated
     */
    private static void register(Class<? extends EventReader<?>> cls, Map<String, EventReader<?>> readers) 
        throws IllegalArgumentException {
        if (null != cls) {
            try {
                EventReader<?> reader = cls.newInstance();
                readers.put(reader.getEventName(), reader);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
    
    /**
     * Defines the maximum number of events to process (regardless whether successful or not, just based on
     * the log line syntax).
     * 
     * @param maxEventCount the maximum number (no maximum if not positive)
     */
    public void setMaxEventCount(int maxEventCount) {
        this.maxEventCount = maxEventCount;
    }
    
    /**
     * Defines the maximum number of lines to process.
     * 
     * @param maxLineCount the maximum number of lines
     */
    public void setMaxLineCount(int maxLineCount) {
        this.maxLineCount = maxLineCount;
    }
    
    /**
     * Changes the date format for individual entries.
     * 
     * @param format the format (ignored if <b>null</b>)
     */
    public void setDateFormat(DateFormat format) {
        if (null != format) {
            this.format = format;
        }
    }
    
    /**
     * Do consider time while replying the data.
     * 
     * @param considerTime <code>true</code> for considering time, <code>false</code> else
     */
    public void considerTime(boolean considerTime) {
        this.considerTime = considerTime;
    }
    
    /**
     * Defines the output stream for logging parsed events.
     * 
     * @param out the output stream (may be <b>null</b> to disable logging)
     */
    public void setOut(PrintStream out) {
        this.out = out;
    }
    
    /**
     * Defines the error stream for logging problems.
     * 
     * @param err the error stream (may be <b>null</b> to disable logging)
     */    
    public void setErr(PrintStream err) {
        this.err = err;
    }
    
    /**
     * Returns the error stream for logging problems.
     * 
     * @return the error stream (may be <b>null</b> if logging is disabled)
     */
    public PrintStream getErr() {
        return err;
    }
    
    /**
     * Clears the registered event readers of this log reader.
     */
    public void doClearRegisteredEventReaders() {
        readers.clear();
    }

    /**
     * Clears the globally registered event readers.
     */
    public static void clearRegisteredEventReaders() {
        REGISTERED.clear();
    }
    
    /**
     * Unregisters an event reader from this log reader.
     * 
     * @param cls the event reader class (<b>null</b> is ignored)
     */
    public void doUnregister(Class<? extends EventReader<?>> cls) {
        unregister(cls, readers);
    }

    /**
     * Unregisters an event reader globally.
     * 
     * @param cls the event reader class (<b>null</b> is ignored)
     */
    public static void unregister(Class<? extends EventReader<?>> cls) {
        unregister(cls, REGISTERED);
    }

    /**
     * Unregisters an event reader.
     * 
     * @param cls the event reader class (<b>null</b> is ignored)
     * @param readers the map to remove the reader from
     */
    private static void unregister(Class<? extends EventReader<?>> cls, Map<String, EventReader<?>> readers) {
        if (null != cls) {
            readers.remove(cls.getSimpleName());
        }
    }

    /**
     * Closes the reader.
     * 
     * @throws IOException in case of I/O problems
     */
    public void close() throws IOException {
        reader.close();
    }

    /**
     * Represents a line entry.
     * 
     * @author Holger Eichelberger
     */
    public static class LineEntry {

        private IEvent event;
        private Date timestamp;
        private String thread;
        
        /**
         * Creates an entry instance.
         */
        private LineEntry() {
        }
        
        /**
         * Sets the timestamp of this entry.
         * 
         * @param timestamp the new timestamp (may be <b>null</b> if the entry shall be invalid)
         */
        private void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        /**
         * Sets the event of this entry.
         * 
         * @param event the event (may be <b>null</b> if the entry shall be invalid)
         */
        private void setEvent(IEvent event) {
            this.event = event;
        }
        
        /**
         * Defines the thread identifier.
         * 
         * @param thread the thread identifier (arbitrary string)
         */
        private void setThread(String thread) {
            this.thread = thread;
        }

        /**
         * Returns the timestamp found in the parsed line. Only time is significant, not date!
         * 
         * @return the timestamp (may be <b>null</b> if the event is invalid)
         */
        public Date getTimestamp() {
            return timestamp;
        }
        
        /**
         * Returns the event found in the parsed line.
         * 
         * @return the event (may be <b>null</b> if the event is invalid)
         */
        public IEvent getEvent() {
            return event;
        }
        
        /**
         * Returns the thread identifier.
         * 
         * @return the thread identifier (may be <b>null</b>)
         */
        public String getThread() {
            return thread;
        }

        /**
         * Returns whether this entry is valid.
         * 
         * @return <code>true</code> if valid, <code>false</code> else
         */
        public boolean isValid() {
            return null != event && null != timestamp;
        }
        
        /**
         * Clears this entry (turns settings to invalid).
         */
        private void clear() {
            event = null;
            timestamp = null;
            thread = null;
        }
        
    }

    /**
     * Reads the log file until <code>maxEventCount</code>.
     * 
     * @param maxEventCount the maximum number (no maximum if not positive)
     * 
     * @throws IOException in case of I/O problems
     */
    public void read(int maxEventCount) throws IOException {
        setMaxEventCount(maxEventCount);
        read();
    }
    
    /**
     * Reads the log file.
     * 
     * @throws IOException in case of I/O problems
     */
    public void read() throws IOException {
        String line = null;
        boolean stop = false;
        LineEntry entry = new LineEntry();
        do {
            line = reader.readLine();
            if (null != line) {
                line = line.trim();
                if (line.length() > 0) {
                    stop = parseLine(line, entry);
                    if (entry.isValid()) {
                        IEvent event = entry.getEvent();
                        if (null != out) {
                            out.printf("%3$d %1$tH:%1$tM:%1$tS:%1$tL %2$s\n", entry.getTimestamp(), event, 
                                reader.getLineNumber());
                        }
                        handleTime(entry);
                        if (null != processor) {
                            processor.handle(event);
                        }
                    }
                }
            }
            if (maxLineCount > 0) {
                stop |= reader.getLineNumber() >= maxLineCount;
            }
        } while (null != line && !stop);
    }
    
    /**
     * Handle the time.
     * 
     * @param entry the entry line
     */
    private void handleTime(LineEntry entry) {
        if (considerTime) {
            long entryTime = Math.abs(entry.getTimestamp().getTime());
            if (0 == currentTime) {
                // initialize and handle
                currentTime = entryTime;
            } else if (currentTime > 0) {
                long sleepTime = Math.abs(entryTime - currentTime);
                // sleep and handle
                sleep(sleepTime);
                currentTime = entryTime;
            } // else just handle
        }
    }
    
    /**
     * Sleeps for a certain time.
     * 
     * @param ms the time to sleep (negative is ignored)
     */
    private static void sleep(long ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Parses a log file line.
     * 
     * @param line the line to be parsed (empty lines are filtered out before)
     * @param entry the related line entry (to be modified as a side effect, invalid if {@link LineEntry#getEvent()} is 
     *     <b>null</b>.
     * @return <code>true</code> if parsing shall be stopped after this line, <code>true</code> else
     */
    protected boolean parseLine(String line, LineEntry entry) {
        entry.clear();
        String origLine = line;
        int pos = line.indexOf(' ');
        if (pos > 0) { // a valid entry always starts with the timestamp
            String tmp = line.substring(0, pos);
            try {
                entry.setTimestamp(format.parse(tmp));
            } catch (ParseException e) {
                if (null != err) {
                    err.print("Date format problem " + e.getMessage() + " in line " + origLine);
                }
            }
            line = consume(line, pos);
            pos = line.indexOf(']'); // tread thread as optional
            if (pos > 0) {
                tmp = line.substring(0, pos + 1).trim();
                if (tmp.startsWith("[") && tmp.endsWith("]") && tmp.length() > 0) {
                    tmp = tmp.substring(1, tmp.length() - 1);
                }
                entry.setThread(tmp);
                line = consume(line, pos);
            }
            line = consumeWhitespace(line);
            pos = line.indexOf(' '); // ignore the log level
            if (pos > 0) {
                line = consumeWhitespace(consume(line, pos));
            }
            boolean isEvent = false;
            pos = line.indexOf('-'); // causing class
            if (pos > 0) {
                tmp = line.substring(0, pos).trim();
                isEvent = tmp.equals(EventManager.class.getName());
                line = consumeWhitespace(consume(line, pos));
            }
            if (isEvent) {
                isEvent = false;
                pos = line.indexOf(' '); // log prefix for a received event
                if (pos > 0) {
                    tmp = line.substring(0, pos).trim();
                    isEvent = EventManager.LOG_PREFIX_RECEIVED.equals(tmp);
                    line = consumeWhitespace(consume(line, pos));
                }
            }
            if (isEvent) {
                pos = line.indexOf(' '); // simple event class name         
                if (pos > 0) {
                    tmp = line.substring(0, pos).trim();
                    line = consume(line, pos);
                    eventCount++;
                    EventReader<?> reader = readers.get(tmp);
                    if (null != reader) {
                        entry.setEvent(reader.parseEvent(line, this));
                    } else {
                        if (null != err) {
                            err.print("No event reader for " + tmp);
                        }
                    }
                }
            }
        }
        return (maxEventCount > 0 && eventCount >= maxEventCount);
    }
    
    /**
     * Consumes trailing whitespaces.
     * 
     * @param line the line to consume for
     * @return the line without consumed trailing whitespaces
     */
    static String consumeWhitespace(String line) {
        int pos = 0;
        while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
            pos++;
        }
        if (pos > 0) {
            line = line.substring(pos);
        }
        return line;
    }
    
    /**
     * Consumes line until <code>pos</code>.
     * 
     * @param line the line to be consumed
     * @param pos the position to consume to
     * @return the line without the consumed prefix, an <code>line</code> if <code>pos</code> is invalid
     */
    static String consume(String line, int pos) {
        String result;
        if (0 <= pos && pos <= line.length()) {
            result = line.substring(pos + 1);
        } else {
            result = line;
        }
        return result;
    }
    
    /**
     * Allows to handle the event in an usage dependent way.
     * 
     * @param event the event to be handled
     */
    protected void handle(IEvent event) {
    }
    
}
