package eu.qualimaster.dataManagement.sinks.replay;

import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.common.replay.ReplayUtils;
import eu.qualimaster.dataManagement.common.replay.Tuple;
import eu.qualimaster.dataManagement.serialization.ISerializer;
import eu.qualimaster.dataManagement.serialization.SerializerRegistry;
import eu.qualimaster.dataManagement.storage.AbstractStorageTable;
import eu.qualimaster.dataManagement.storage.support.IStorageSupport;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import org.apache.hadoop.hbase.client.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simulate the results from the Replay Store (currently HBase) as stream items
 * // TODO 26/10/16: Optimize the easy-checking anti-pattern with native callback readers-writers
 * @author tuan
 * @since 03/06/16.
 */
public class ReplayStreamer<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ReplayStreamer.class);

    /** Speed factor */
    private float speedFactor = -1;
    private Date startDate, endDate;

    /** the query as passed from the client. So far we only process
     * key query (point or range) */
    private String query;
    private String prefix;
    
    /** Control the thread of fetching data from HBase */
    private volatile boolean stopFetchingDataThread = false;

    /**
     * Timeout 50 miliseconds for stop querying the database to have
     * a pseudo-non-blocking call to the getData()
     */
    private static final int TIMEOUT = 50;
    private int counter = 0;
    
    private LinkedBlockingQueue<T> buffer;

    private ISerializer<T> serializer;
    private ReplayDataInput resultWrapper;
    private ExecutorService fetcherThread;
    private ReplayAggregator aggregator;

    public ReplayStreamer(Class<T> cls, Tuple schema, String location, IStorageStrategyDescriptor d) {
        AbstractStorageTable table = DataManager.REPLAY_STORAGE_MANAGER.getTable(location, schema.getName(), d);
        IStorageSupport storage = table.getStorageSupport();
        resultWrapper = new ReplayDataInput(schema, storage);
        this.serializer = SerializerRegistry.getSerializer(cls.getSimpleName(), cls);
        buffer = new LinkedBlockingQueue<>(100);
        fetcherThread = Executors.newSingleThreadExecutor();
        fetcherThread.submit(new DataFetcher());
        aggregator = new LastItemAggregator();
    }

    /* Need to synchronize the speed set with other thread as well ? */
    public void setSpeed(float speed) {
        stopFetchingDataThread = true;
        buffer.clear();
        speedFactor = speed;
        // updateQuery();
        stopFetchingDataThread = false;
    }

    public void setStart(Date date) {
    	stopFetchingDataThread = true;

        // the internal fullyLock() method is called inside BlockingQueue's
        // clear() method code so no race condition here
        buffer.clear();
        startDate = date;
        updateQuery();
        stopFetchingDataThread = false;
    }

    public void setEnd(Date date) {
    	stopFetchingDataThread = true;
        buffer.clear();
        endDate = date;
        updateQuery();
        stopFetchingDataThread = false;
    }

    public void setQuery(String query) {
    	stopFetchingDataThread = true;
        buffer.clear();
        this.query = query;
        updateQuery();
        stopFetchingDataThread = false;
    }

    /** Note: Mock the test of serializer */
    public void setSerializer(ISerializer s) {
        this.serializer = s;
    }

    /**
     * Get the data of the current parameters. Null value means two things:
     * - some error happens (server error, parsing of schema, timeout...),
     *  the call still moves on
     * - the current stream is all finished
     */
    public T getData() {
        try {
            T result = null;
            // This is probably very rare: We call getData() right in the
            // moment between the first and the last lines of code in the
            // set-parameter methods
            // We return null
            // LOG.info("Fetching flag: " + stopFetchingDataThread);
            while (stopFetchingDataThread && counter < TIMEOUT) {
                counter++;
                Thread.sleep(1);
            }
            if (counter < TIMEOUT) {
                LOG.info("Connect buffer to check for available data");
                result = buffer.poll(10, TimeUnit.SECONDS);

                /** 2016-10-19 Tuan: Test random sink - Note */
                if (result != null)
                    LOG.info("Get available data from the buffer: " + result.getClass().getName());
                else
                    LOG.info("No available data from the buffer");
                /** End note */
            }
            else {
                LOG.info("Timeout querying database.. Reset clock");
                counter = 0;
            }
            return result;
		} catch (InterruptedException e) {
            LOG.error("Reading-data thread is interrupted while fetching" +
                    " from buffer (query " + query + "). Return null");
            return null;
        }
    }

    private void updateQuery() {
        if (startDate == null || endDate == null ||query == null ||
                query.isEmpty() || speedFactor == -1) {
            LOG.warn("The streamer is not ready. Set the parameters first !!");
            return;
        }
        resultWrapper.updateQuery(query, startDate, endDate, aggregator);
    }

    /* It's dangerous to override this class */
    private final class DataFetcher implements Runnable {

		@Override
		public void run() {
            LOG.info("Start Runnable of DataFetcher");
            try {
                while (true) {
                    if (stopFetchingDataThread) {
                        LOG.info("The parameters are probably being updated. Wait 10 ms");
                        Thread.sleep(10);
                    }

                    while (!resultWrapper.isEOD()) {
                        try {
                            LOG.info("Received 1 data item. Will be serialized using " + serializer);
                            T data = serializer.deserializeFrom(resultWrapper);
                            LOG.info("Serializer passed");
                            if (data != null) {
                                LOG.info("Serialized 1 data item: " + data.toString());
                                buffer.put(data);
                            } else {
                                LOG.info("Cannot serialize data");
                            }
                        } catch (IOException e) {
                            LOG.error("Error getting data from HBase for the query " + query, e);
                        } catch (Exception e) {
                            LOG.error("ERROR: ", e);
                            if (e instanceof InterruptedException) throw new RuntimeException(e);
                        }
                    }

                    // Potential gotcha here, since resultWrapper is not thread-safe
                    if (resultWrapper.isEOD()) {
                        // LOG.info("The result wrapper is empty, wait 90 mi secs before the next ping");
                        // Need to tune this according to TSI performance
                        Thread.sleep(100);
                    }
                    else {
                        // Relax the loading on the TSI cluster
                        LOG.info("Wait 10 misecs before the next request to the result wrapper");
                        Thread.sleep(10);
                    }
                }
            }
            catch (InterruptedException e) {
                LOG.warn("The deading-data thread is interrupted or closed");
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The current aggregation strategy is removing all
     * but the last item in the stream
     */
    private class LastItemAggregator implements ReplayAggregator {

        private long prevTimestamp = 0L;

        @Override
        public Result aggregate(String[] key, Result item) {
            Result result = null;
            long timestamp = ReplayUtils.getTimestampFromResult(key);
            if (prevTimestamp == 0)
                result = item;
            else {
                // get the timestamp from the key
                long dev = (timestamp - prevTimestamp) / 1000;

                // Strange case: The items are emitted not in chronological order
                if (dev < 0) {
                    LOG.warn("The message are not come in order: "
                             + timestamp + " < " + prevTimestamp);
                }
                if (dev >= speedFactor) {
                    result = item;
                }
            }
            prevTimestamp = timestamp;
            return result;
        }
    }


    /** This method is to checked that the null return values of the getData() is caused
     * by some internal error, or by the complete of the data fetch */
    public boolean isEOD() {
        return (buffer.isEmpty() || resultWrapper.isEOD());
    }

    public void close() throws IOException {
        resultWrapper.close();
        stopFetchingDataThread = true;

        // Do we really shutdown the streamer when close() is called ?
        try {
            LOG.info("attempt to shutdown the data fetching");
            fetcherThread.shutdown();
            fetcherThread.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            LOG.error("tasks interrupted", e);
        }
        finally {
            if (!fetcherThread.isTerminated()) {
                LOG.warn("cancel non-finished tasks");
            }
            fetcherThread.shutdownNow();
            LOG.info("shutdown finished");
        }
    }
}