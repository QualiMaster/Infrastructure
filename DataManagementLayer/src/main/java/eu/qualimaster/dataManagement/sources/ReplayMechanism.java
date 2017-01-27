package eu.qualimaster.dataManagement.sources;

import eu.qualimaster.dataManagement.sources.replay.DateTimeTimestampParser;
import eu.qualimaster.dataManagement.sources.replay.FileSource;
import eu.qualimaster.dataManagement.sources.replay.HdfsSource;
import eu.qualimaster.dataManagement.sources.replay.IDataManipulator;
import eu.qualimaster.dataManagement.sources.replay.IReplaySource;
import eu.qualimaster.dataManagement.sources.replay.ITimestampParser;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.pipeline.DefaultModeException;

import org.joda.time.DateTime;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * A generic data replay mechanism. For using HSDFS, please set {@link DataManagementConfiguration#URL_HDFS} properly.
 * This class is intentional a generic {@link IDataSource} with a set of parameters. Please do not change the public
 * methods as the generated profiling pipelines relay on them.
 * 
 * Do not move / rename this class as code is generated against!
 * 
 * @author Nikolaos Pavlakis on 1/13/15.
 * @author Holger Eichelberger
 */
public class ReplayMechanism implements IDataSource {

    private Logger logger = Logger.getLogger(ReplayMechanism.class);

    private IStorageStrategyDescriptor strategy;

    private char separator;
    private boolean separatorDefined = false;
    private IReplaySource source;
    private ITimestampParser timestampParser = DateTimeTimestampParser.INSTANCE;
    private IDataManipulator manipulator;
    private BufferedReader brForData;
    private boolean endOfData = false;
    private long offsetInMillis; // Offset in milliseconds between first timestamp and now
    private long prevTimeStampNow;
    private boolean shallConnect;
    private boolean selfConnect = true; // legacy setting

    // Throughput measurement
    private long monitoringTimestamp;
    private long throughput;
    private int measurementDuration; // seconds
    
    private long start = 0;
    private long record = 0;
    private boolean init = false;
    private int timeInterval = 1000; //1s    

    /**
     * Creates a new replay mechanism without explicit source, manipulator and default timestamp parser.
     */
    public ReplayMechanism() {
        this(null, null, null);
    }

    /**
     * Creates a new replay mechanism with no manipulator and default timestamp parser.
     * 
     * @param source the physical source (may be <b>null</b> but must be set before calling {@link #connect()}.
     */
    public ReplayMechanism(IReplaySource source) {
        this(source, null, null);
    }

    /**
     * Creates a new replay mechanism with no source, no manipulator and given timestamp parser.
     * 
     * @param timestampParser the timestamp parser (the default {@link DateTimeTimestampParser} if <b>null</b>)
     */
    public ReplayMechanism(ITimestampParser timestampParser) {
        this(null, null, timestampParser);
    }

    /**
     * Creates a new replay mechanism with source, no manipulator and given timestamp parser.
     * 
     * @param timestampParser the timestamp parser (the default {@link DateTimeTimestampParser} if <b>null</b>)
     */
    public ReplayMechanism(IReplaySource source, ITimestampParser timestampParser) {
        this(source, null, timestampParser);
    }

    /**
     * Creates a new replay mechanism.
     * 
     * @param source the physical source (may be <b>null</b> but must be set before calling {@link #connect()}.
     * @param manipulator an optional data manipulator applied before returning replay data
     * @param timestampParser the timestamp parser (the default {@link DateTimeTimestampParser} if <b>null</b>)
     */
    public ReplayMechanism(IReplaySource source, IDataManipulator manipulator, ITimestampParser timestampParser) {
        monitoringTimestamp = 0L;
        throughput = 0L;
        measurementDuration = 1 * 60;
        this.source = source;
        if (null != timestampParser) {
            this.timestampParser = timestampParser;
        }
        this.manipulator = manipulator;
    }
    
    /**
     * Defines the replay source. Has only effect before {@link #connect()}.
     * 
     * @param source the data source (ignored if <b>null</b>)
     */
    public void setSource(IReplaySource source) {
        if (null != source) {
            this.source = source;
            logger.info("Defined source " + source.getClass());
            if (shallConnect && selfConnect) {
                logger.info("Self-connect from set source");
                connect();
            }
        }
    }

    /**
     * Forces the replay to not try connecting itself. Required for profiling in DML autoconnect mode.
     */
    public void forceAutoconnect() {
        this.selfConnect = false;
    }
    
    /**
     * Consumes whitespaces. As tabs may be used as separator, we consider here plain whitspaces only.
     * 
     * @param line the line to consume the whitespaces within
     * @param pos the actual position within <code>line</code>
     * @return the next non whitespace character in <code>line</code> after <code>pos</code>, may be <code>pos</code>
     */
    private static int consumeWhitespace(String line, int pos) {
        while (pos < line.length() && ' ' == line.charAt(pos)) {
            pos++;
        }
        return pos;
    }
    
    /**
     * Adjusts the timestamp based on the current time.
     * @param timestamp the timestamp in the dataset
     * @return the new timestamp
     */
    private long newTimestamp(long timestamp) {
        return timestamp + offsetInMillis;
    }
    
    private void updateOffset(long timestamp) {
        offsetInMillis = System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Reads out the next line and adjusts the date.
     * 
     * @param line the actual input line
     * @param consider this line to calculate the initial offset, i.e., if <code>true</code> this is the first line
     * @return the next data line
     */
    private String newlineWithDateToNow(String line, boolean calcOffset) {
        String newline = null;
        if (null != manipulator) {
            line = manipulator.changeInput(line, calcOffset);
        }
        int timestampEnd = timestampParser.consumeTimestamp(line);
        if (timestampEnd > 0) {
            // split data
            String timestamp = line.substring(0, timestampEnd);
            try {
                long symbolTimeStamp = timestampParser.parseTimestamp(timestamp);
                if (calcOffset) {
                    DateTime now = new DateTime();
                    offsetInMillis = now.getMillis() - symbolTimeStamp;
                } else {
                    prevTimeStampNow = symbolTimeStamp + offsetInMillis;
                }
                int separatorPos = consumeWhitespace(line, timestampEnd);
                int payloadStartPos = consumeWhitespace(line, separatorPos + 1);
                if (separatorPos < payloadStartPos && payloadStartPos < line.length()) {
                    if (!separatorDefined) {
                        separator = line.charAt(separatorPos);
                    }
                    String payload = line.substring(payloadStartPos);
                    if (null != manipulator) {
                        newline = manipulator.composeData(prevTimeStampNow, line);
                    } else {
                        newline = payload;
                    }
                }
            } catch (ParseException e) {
                logger.error("Simulator Error : " + e.getMessage());
            }
        }
        return newline;
    }
    
    /**
     * Returns the separator between individual data fields.
     * 
     * @return the separator char
     */
    public char getSeparator() {
        return separator;
    }
    
    /**
     * Returns whether we are (at the current position) at the end-of-data.
     * This information is only valid after calling {@link #getNext()}.
     * 
     * @return <code>true</code> for end of data, <code>false</code> else
     */
    public boolean isEOD() {
        return endOfData;
    }
    
    /**
     * Creates an item containing the timestamp and a list of data item instances in the profiling queue.
     * @author Cui Qin
     *
     * @param <T> the target class type of the data item
     */
    public static class ProfilingQueueItem<T> {
        long timestamp;
        T item;
        
        /**
         * Creates an item for the profiling queue.
         * @param timestamp the timestamp
         * @param item the data item
         */
        public ProfilingQueueItem(long timestamp, T item) {
            this.timestamp = timestamp;
            this.item = item;
        }
        /**
         * Returns the timestamp.
         * @return the timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
        /**
         * Returns a list of data items.
         * @return a list of data items
         */
        public T getItem() {
            return item;
        }        
        
    }
    /**
     * Reads profiling data into queues.
     * @param handler the source handler
     * @param size the queue size 
     * @return queueList the queue list
     * @throws IOException IO Exception
     */    
    public void readProfilingData(GenericMultiSourceHandler handler, int size, List<DataQueueDescriptor<?>> queueList) throws IOException {
        DataQueueDescriptor<?> queueDes;
        int queueCounter = 0;
        long timestamp = 0;
        while (!isEOD() && queueCounter < size) {
            String genericInput = getNext(false);
            if (null != genericInput) {
                char separator = getSeparator();
                String tupleId = handler.nextId(genericInput, separator, false);
                queueDes = getQueueDescriptor(tupleId, queueList);
                
                //parse the data input to instance
                Object item = handler.next(tupleId, queueDes.getCls(), genericInput, separator, false, true);
                
                //get the corresponding timestamp
                timestamp = handler.nextTimestamp(genericInput, separator, false);
                queueDes.add(timestamp, item);
            }
            
            queueCounter++;
        }
    }
    /**
     * Returns the queue descriptor based on the given id.
     * @param id the given id
     * @param queueList the queue list
     * @return the queue descriptor
     */
    public DataQueueDescriptor<?> getQueueDescriptor(String id, List<DataQueueDescriptor<?>> queueList) {
        DataQueueDescriptor<?> result = null;
        java.util.Iterator<DataQueueDescriptor<?>> it = queueList.iterator();
        DataQueueDescriptor<?> des;
        while(it.hasNext()) {
            des = it.next();
            if (id.equals(des.getId())) {
                //
                result = des;
                break;
            }
        }
        return result;
    }
    
    /**
     * Returns the next item considering the time interval based on the timestamp.
     * @param queue the queue to read data
     * @return the next item
     * @throws InterruptedException the interrupted exception
     */
    public <T> T getNextItem(Queue<ProfilingQueueItem<T>> queue) throws InterruptedException {
        T item = null;              
      
        long newTimestamp = 0;        
        ProfilingQueueItem<T> queueItem = queue.poll();
        long timestamp = queueItem.getTimestamp(); 
        long now = System.currentTimeMillis();
        if(!init) {
            init = true;
            record = newTimestamp;
            start = now;
            updateOffset(timestamp);
        } 
        newTimestamp = newTimestamp(timestamp);
        
        if (record == newTimestamp) {//within the same batch
            if (now - start <= timeInterval) {
                item = queueItem.getItem();
            } else {
                while (newTimestamp == record) {//skip rest of data with old timestamp
                    queueItem = queue.poll();
                    item = queueItem.getItem();
                    timestamp = queueItem.getTimestamp();
                    newTimestamp = newTimestamp(timestamp);
                    now = System.currentTimeMillis();
                }
                start = now;
                record = newTimestamp;
            }
        } else {//next batch starts
            if (now - start <= timeInterval) {
                //wait until it reaches 1s
                while (now - start <= timeInterval) {
                    Thread.sleep(1);
                    now = System.currentTimeMillis();
                }
            }
            item = queueItem.getItem();
            record = newTimestamp;
            start = now;
        }

        return item;
    }
    /**
     * Returns the next line payload considering the timestamp.
     * 
     * @param control <b>true</b> consider the timestamp, otherwise return the next line immediately
     * @return the next line, <b>null</b> if there is none
     * @throws DefaultModeException in case of illegal data switching the calling pipeline into default mode
     */
    public String getNext(boolean control) throws DefaultModeException {
        String line = null;
        try {
            if (null != brForData && (line = brForData.readLine()) != null) {
                if(control) {
                    if (timestampParser.skipParsing(line)) {
                        return null;
                    }
                    String newline = newlineWithDateToNow(line, false);
                    try {
                        DateTime now = new DateTime();
                        while (prevTimeStampNow > now.getMillis()) {
                            // TODO this is original code, increases the response time and capacity. Better return null
                            Thread.sleep(1);
                            now = new DateTime();
                        }
                        // Thread.sleep(diff > 0 ? diff : 0);git
                    } catch (InterruptedException e) {
                        logger.error("Simulator Error : " + e.getMessage());
                    }
    
                    // Throughput measurement
                    monitorMe();
                    return newline;
                } else {
                    /*
                     * return payload only
                    String payload = null;
                    int timestampEnd = timestampParser.consumeTimestamp(line);
                    int separatorPos = consumeWhitespace(line, timestampEnd);
                    int payloadStartPos = consumeWhitespace(line, separatorPos + 1);
                    if (separatorPos < payloadStartPos && payloadStartPos < line.length()) {                        
                        payload = line.substring(payloadStartPos);
                    }
                    return payload;
                    */
                    return line;
                }
            } else {
                if (selfConnect || null != brForData) {
                    // self connect and no data *or* not auto-connect and connected but no data
                    endOfData = true;
                }
            }
        } catch (IOException e) {
            logger.error("Simulator Error : " + e.getMessage());
            throw new DefaultModeException("Simulator Error : " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Changes the data file to be used for replay. Has only effect if called before {@link #connect()}.
     * Alternative to {@link #setParameterHdfsDataFile(String)}.
     * 
     * @param fileForData the data file name
     */
    public void setParameterDataFile(String fileForData) {
        if (null != fileForData && fileForData.length() > 0) {
            logger.info("Received Data file path " + fileForData);
            setSource(new FileSource(fileForData));
        }
    }

    /**
     * Changes the HDFS data file to be used for replay. Has only effect if called before {@link #connect()}. 
     * Alternative to {@link #setParameterDataFile(String)}.
     * 
     * @param value the HDFS data file path
     */
    public void setParameterHdfsDataFile(String hdfsPathToData) {
        if (null != hdfsPathToData && hdfsPathToData.length() > 0) {
            logger.info("Received HDFS Data file path " + hdfsPathToData);
            setSource(new HdfsSource(hdfsPathToData));
        }
    }

    /**
     * Changes the replay speed.
     * 
     * @param value the new replay speed
     */
    public void setParameterReplaySpeed(int value) {
        //TODO @TSI, further parameters follow this schema, but need to be considered by the profile pipeline generation
    }

    @Override
    public void connect() throws DefaultModeException {
        if (null != source) {
            try {
                brForData = source.open();
            } catch (IOException e) {
                logger.error("Simulator Error : " + e.getMessage());
                // so far only for FNF of File input
                throw new DefaultModeException("Simulator Error : " + e.getMessage());
            }
            String line;
    
            // Read first line from data file to get the timestamp offset, the separator and throw away the data
            try {
                if ((line = brForData.readLine()) != null) {
                    newlineWithDateToNow(line, true); 
                } else {
                    endOfData = true;
                }
            } catch (IOException e) {
                logger.error("Simulator Error : " + e.getMessage());
                throw new DefaultModeException("Simulator Error : " + e.getMessage());
            }
            logger.info("Connected.");
        } else {
            shallConnect = true;
            logger.info("Switching to shall connect " + shallConnect);
        }
    }

    @Override
    public void disconnect() {
        logger.info("Trying to disconnect " + brForData);
        if (null != brForData) {
            try {
                brForData.close();
            } catch (IOException e) {
                logger.error("Simulator Error : " + e.getMessage());
            }
        }
        brForData = null;
        shallConnect = false;
        logger.info("Disconnected");
    }

    @Override
    public void setStrategy(IStorageStrategyDescriptor strategy) {
        this.strategy = strategy;
    }

    @Override
    public IStorageStrategyDescriptor getStrategy() {
        return strategy;
    }

    @Override
    public Double getMeasurement(IObservable iObservable) {
        return null;
    }

    /**
     * Performs internal monitoring for data source control.
     */
    private void monitorMe() {
        if (monitoringTimestamp == 0) {
            monitoringTimestamp = new Date().getTime();
            ++throughput;
        } else {
            long now = new Date().getTime();
            if (now - monitoringTimestamp < measurementDuration * 1000) {
                ++throughput;
            } else {
                logger.info("Pipeline input throughput: " 
                    + ((double) throughput / (double) measurementDuration) + " tuples/sec");
                monitoringTimestamp = now;
                throughput = 1;
            }
        }
    }
    
    @Override
    public IHistoricalDataProvider getHistoricalDataProvider() {
        return null;
    }
    
    @Override
    public Map<String, String> getIdsNamesMap() {
    	return null;
    }

    @Override
    public void setDataSourceListener(IDataSourceListener listener) {
        // no mapping, no listener needed
    }
    
}