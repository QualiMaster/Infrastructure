package eu.qualimaster.common.switching.actions;

import eu.qualimaster.common.signal.SignalStates;

/**
 * Control streams via flags, i.e., disabling or enabling data streams by
 * switching off or on flags. In this class we list all the stream flows to be
 * directly controlled by their connected nodes.
 * 
 * @author Cui Qin
 *
 */
public enum ControlStreamFlag {
    PRE_v1 {
        // disable the stream flow PRE -> ORGINT
        @Override
        public void disableStreamFlow() {
            SignalStates.setEmitOrgPRE(false);
        }
        
        // enable the stream flow PRE -> ORGINT
        @Override
        public void enableStreamFlow() {
            SignalStates.setEmitOrgPRE(true);
            
        }
    },

    PRE_v2 {
        // disable the stream flow PRE -> TGTINT
        @Override
        public void disableStreamFlow() {
            SignalStates.setEmitTrgPRE(false);
        }

        // enable the stream flow PRE -> TGTINT
        @Override
        public void enableStreamFlow() {
            SignalStates.setEmitTrgPRE(true);
        }
    },

    ORGINT_v3 {
        // disable the stream flow ORGINT ->Aold
        @Override
        public void disableStreamFlow() {
            SignalStates.setPassivateOrgINT(false);
        }

        // enable the stream flow ORGINT ->Aold
        @Override
        public void enableStreamFlow() {
            SignalStates.setPassivateOrgINT(true);
        }
    },

    TGTINT_v4 {
        // disable the stream flow TGTINT -> Anew
        @Override
        public void disableStreamFlow() {
            SignalStates.setPassivateTrgINT(false);
        }

        // enable the stream flow TGTINT -> Anew
        @Override
        public void enableStreamFlow() {
            SignalStates.setPassivateTrgINT(true);
        }
    },

    ORGEND_v7 {
        // disable the stream flow ORGEND -> SUC
        @Override
        public void disableStreamFlow() {
            SignalStates.setEmitOrgEND(false);
        }

        // enable the stream flow ORGEND -> SUC
        @Override
        public void enableStreamFlow() {
            SignalStates.setEmitOrgEND(true);
        }
    },

    TGTEND_v8 {
        // disable the stream flow TGTEND -> SUC
        @Override
        public void disableStreamFlow() {
            SignalStates.setEmitTrgEND(false);
        }

        // disable the stream flow TGTEND -> SUC
        @Override
        public void enableStreamFlow() {
            SignalStates.setEmitTrgEND(true);
        }
    };

    /**
     * Disable the stream flow.
     */
    public abstract void disableStreamFlow();
    
    /**
     * Enable the stream flow.
     */
    public abstract void enableStreamFlow();
}
