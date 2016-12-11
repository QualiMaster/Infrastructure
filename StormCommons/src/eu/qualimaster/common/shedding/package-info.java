/**
 * Generic load schedding. Shedders must have a public no-argument constructor to be created by the 
 * {@link eu.qualimaster.common.shedding.LoadShedderFactory}. Default shedders shall provide a 
 * {@link eu.qualimaster.common.shedding.ILoadShedderDescriptor} and may be configured via parameters, in particular
 * {@link eu.qualimaster.common.shedding.DefaultLoadSheddingParameter default parameters}.
 */
package eu.qualimaster.common.shedding;