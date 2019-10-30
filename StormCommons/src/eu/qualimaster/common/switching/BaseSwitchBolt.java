package eu.qualimaster.common.switching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.common.signal.BaseSignalBolt;
import eu.qualimaster.common.switching.actions.IAction;
import eu.qualimaster.common.switching.actions.SwitchStates.ActionState;
/**
 * Implements a basic switching Bolt, carrying the common parts for all switching-related bolts.
 * 
 * @author Cui Qin
 *
 */
@SuppressWarnings("serial")
public abstract class BaseSwitchBolt extends BaseSignalBolt {
    private Map<ActionState, List<IAction>> actionMap;
    /**
     * Creates a switch Bolt.
     * 
     * @param name
     *            the name of the Bolt
     * @param pipeline
     *            the pipeline, namely the name of the pipeline which the Bolt belongs to
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public BaseSwitchBolt(String name, String pipeline, boolean sendRegular) {
        super(name, pipeline, sendRegular);
        actionMap = new HashMap<ActionState, List<IAction>>();
    }
    
    /**
     * Adds actions into a map that links the action state and the corresponding actions together.
     * @param state the action state
     * @param action the action to be added
     */
    protected void addAction(ActionState state, IAction action) {
        if (actionMap.containsKey(state)) {
            List<IAction> list = actionMap.get(state);
            list.add(action);
            actionMap.put(state, list);
        } else {
            List<IAction> listNew = new ArrayList<IAction>();
            listNew.add(action);
            actionMap.put(state, listNew);
        }
    }
    
    /**
     * Returns the action map.
     * @return the action map
     */
    protected Map<ActionState, List<IAction>> getActionMap() {
        return this.actionMap;
    }
}
