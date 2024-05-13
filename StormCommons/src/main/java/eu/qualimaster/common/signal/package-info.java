/**
 * The low-level pipeline signalling mechanism. Basically, this relies on Apache Curator, but may
 * alternatively be realized through QualiMaster events (see SignalMechanism). It also provides a 
 * transparent way of using signals. Just create a signal and send it via the default send method 
 * or the SignalConnection.
 */
package eu.qualimaster.common.signal;