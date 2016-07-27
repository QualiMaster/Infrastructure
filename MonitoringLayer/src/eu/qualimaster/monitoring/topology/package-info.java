/**
 * Implements a data structure representing the full topology of a pipeline, i.e., 
 * an overlay of configured, running (Storm) and virtual (TCP) connections. This
 * structure can be used to determine the predecessor nodes for traces or the 
 * latency / throughput of (sub-)topologies/(sub-)pipelines.
 * 
 * @author Holger Eichelberger
 */
package eu.qualimaster.monitoring.topology;