package eu.qualimaster.common.switching.actions;

/**
 * List the stream flows to be controlled via signal.
 * @author qin
 *
 */
public enum StreamFlowSignal {
	PRE_v3, PRE_v7, PRE_v4, ORGINT_v1, ORGINT_v2, ORGINT_v7, //used in disabling stream flows
	PRE_v8, ORGINT_v8, TGTINT_v2, TGTINT_v8 //used in enabling stream flows
}
