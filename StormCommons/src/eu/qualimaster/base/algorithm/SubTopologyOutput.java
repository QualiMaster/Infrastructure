package eu.qualimaster.base.algorithm;

/**
 * Define a class for the output of a sub-topology carrying the creation results.
 * 
 * @author Cui Qin
 */
public class SubTopologyOutput {
    private String boltName; // the last Bolt name of the sub-topology
    private String streamId; // the streamId emitting from the lastBolt of the
                             // sub-topology
    private int numWorkers;
    private int maxTaskParallelism;

    /**
     * Constructor.
     * 
     * @param boltName
     *            the last Bolt name of the sub-topology
     * @param streamId
     *            the streamId emitting from the lastBolt
     * @param numWorkers
     *            the requested number of workers (to be considered by the main
     *            topology)
     * @param maxTaskParallelism
     *            the requested number of tasks for parallel execution (to be
     *            considered by the main topology)
     */
    public SubTopologyOutput(String boltName, String streamId, int numWorkers, int maxTaskParallelism) {
        this.boltName = boltName;
        this.streamId = streamId;
        this.numWorkers = numWorkers;
        this.maxTaskParallelism = maxTaskParallelism;
    }

    /**
     * Returns the last Bolt name of the sub-topology.
     * 
     * @return the last Bolt name
     */
    public String getBoltName() {
        return boltName;
    }

    /**
     * Returns the streamId emitting from the lastBolt.
     * 
     * @return the streamId
     */
    public String getStreamId() {
        return streamId;
    }

    /**
     * Returns the number of workers in the sub-topology.
     * 
     * @return the number of workers
     */
    public int getNumWorkers() {
        return numWorkers;
    }

    /**
     * Returns the max number of the task parallelism in sub-topology.
     * 
     * @return the max number of the task parallelism
     */
    public int getMaxTaskParallelism() {
        return maxTaskParallelism;
    }

}
