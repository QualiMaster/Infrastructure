/**
 * The QualiMaster monitoring layer.
 * 
 * <ol>
 *   <li>Simple Java algorithms are represented by the algorithm. The pipeline node propagates the monitored values
 *       into the active algorithm.</li>
 *   <li>Complex algorithms are represented as sub-nodes of the algorithm. Sub-nodes and algorithm are linked, sub-nodes
 *       are monitored themselves. Pipelines and pipeline nodes on top-level receive actual values through topology
 *       aggregation.</li>
 * </ol>
 */
package eu.qualimaster.monitoring;