package eu.qualimaster.common.switching.actions;

/**
 * Control streams via flags, i.e., disabling or enabling data streams by
 * switching off or on flags. In this class we list all the stream flows to be
 * directly controlled by their connected nodes.
 * 
 * @author Cui Qin
 *
 */
public enum StreamFlowFlag {
    PRE_v1 {
        // disable the stream flow PRE -> ORGINT
        @Override
        public void disableStreamFlow() {
            SwitchStates.setEmitOrgPRE(false);
        }
        
        // enable the stream flow PRE -> ORGINT
        @Override
        public void enableStreamFlow() {
            SwitchStates.setEmitOrgPRE(true);
            
        }
    },

    PRE_v2 {
        // disable the stream flow PRE -> TGTINT
        @Override
        public void disableStreamFlow() {
            SwitchStates.setEmitTrgPRE(false);
        }

        // enable the stream flow PRE -> TGTINT
        @Override
        public void enableStreamFlow() {
            SwitchStates.setEmitTrgPRE(true);
        }
    },

    ORGINT_v3 {
        // disable the stream flow ORGINT ->Aold
        @Override
        public void disableStreamFlow() {
            SwitchStates.setPassivateOrgINT(true);
        }

        // enable the stream flow ORGINT ->Aold
        @Override
        public void enableStreamFlow() {
            SwitchStates.setPassivateOrgINT(false);
        }
    },

    TGTINT_v4 {
        // disable the stream flow TGTINT -> Anew
        @Override
        public void disableStreamFlow() {
            SwitchStates.setPassivateTrgINT(true);
        }

        // enable the stream flow TGTINT -> Anew
        @Override
        public void enableStreamFlow() {
            SwitchStates.setPassivateTrgINT(false);
        }
    },

    ORGEND_v7 {
        // disable the stream flow ORGEND -> SUC
        @Override
        public void disableStreamFlow() {
            SwitchStates.setEmitOrgEND(false);
        }

        // enable the stream flow ORGEND -> SUC
        @Override
        public void enableStreamFlow() {
            SwitchStates.setEmitOrgEND(true);
        }
    },

    TGTEND_v8 {
        // disable the stream flow TGTEND -> SUC
        @Override
        public void disableStreamFlow() {
            SwitchStates.setEmitTrgEND(false);
        }

        // disable the stream flow TGTEND -> SUC
        @Override
        public void enableStreamFlow() {
            SwitchStates.setEmitTrgEND(true);
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
