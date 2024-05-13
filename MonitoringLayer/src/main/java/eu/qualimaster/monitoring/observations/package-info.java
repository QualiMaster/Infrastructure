/**
 * Observations, i.e., instances realizing the observation of individual observables. Observations can consist
 * of a simple single value, but, due to the distributed nature of the QM execution systems also be composed of 
 * multiple individual values identified by a key. In the latter case,t he actual observation is then computed
 * from the individual values and can even take the topology structure into account.
 * 
 * Observations are created via the {@link eu.qualimaster.monitoring.observations.ObservationFactory observation 
 * factory}.
 */
package eu.qualimaster.monitoring.observations;