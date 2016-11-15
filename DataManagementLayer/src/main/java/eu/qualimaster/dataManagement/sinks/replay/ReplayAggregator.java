package eu.qualimaster.dataManagement.sinks.replay;

import org.apache.hadoop.hbase.client.Result;

/**
 * Implement different aggregator strategies at the client side
 * to complement the server-side (Co-processor) aggregation
 * Created by tuan on 15/11/16.
 */
public interface ReplayAggregator {

    /** upon receiving a new message from the stream, update the
     * aggregation and gives back the answer */
    public Result aggregate(String[] key, Result item);
}

