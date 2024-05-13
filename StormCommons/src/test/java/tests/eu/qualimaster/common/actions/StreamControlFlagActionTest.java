package tests.eu.qualimaster.common.actions;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.signal.SignalStates;
import eu.qualimaster.common.switching.actions.StreamFlowFlag;
import eu.qualimaster.common.switching.actions.DisableFlagAction;
import eu.qualimaster.common.switching.actions.EnableFlagAction;
/**
 * Tests for disabling actions.
 * @author Cui Qin
 *
 */
public class StreamControlFlagActionTest {
    /**
     * Tests the disable flag action.
     */
    @Test
    public void testDisableFlagAction() {
        DisableFlagAction flagAction = new DisableFlagAction(StreamFlowFlag.PRE_v1);
        flagAction.execute();
        //Assert.assertEquals(SignalStates.isEmitOrgPRE(), false);
    }
    
    /**
     * Tests the enable flag action.
     */
    @Test
    public void testEnableFlagAction() {
        EnableFlagAction flagAction = new EnableFlagAction(StreamFlowFlag.PRE_v1);
        flagAction.execute();
        Assert.assertEquals(SignalStates.isEmitOrgPRE(), true);
    }
}
