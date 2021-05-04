package eu.qualimaster.common.switching.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eu.qualimaster.common.switching.actions.ActionState;
import switching.logging.LogProtocol;
/**
 * Create a map of switch actions equipped with switch states.
 * @author qin
 *
 */
public class SwitchActionMap {
	private Map<ActionState, List<IAction>> actionMap;
	
	/**
	 * Constructor.
	 */
	public SwitchActionMap() {
		actionMap = new HashMap<ActionState, List<IAction>>();
	}
	
	/**
	 * Adds actions into a map that links the action state and the corresponding
	 * actions together.
	 * 
	 * @param state  the action state
	 * @param action the action to be added
	 */
	public void addAction(ActionState state, IAction action) {
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
	 * Executes actions found in the action map.
	 * 
	 * @param state       the action state
	 * @param value       the value to be updated at runtime if given (only for
	 *                    <code>SendSignalAction</code>)
	 * @param logProtocol the log protocol used to write logs to corresponding
	 *                    files.
	 */
	public void executeActions(ActionState state, Serializable value, LogProtocol logProtocol) {
		List<IAction> actionList = new ArrayList<IAction>();
		if (actionMap.containsKey(state)) {
			actionList = actionMap.get(state);
		}
		for (int i = 0; i < actionList.size(); i++) {
			if (null != value && (actionList.get(i) instanceof SendSignalAction)) {
				SendSignalAction action = (SendSignalAction) actionList.get(i);
//                if (null != logProtocol) {
//                    logProtocol.createGENLog("Executing a send signal action with runtime value to be updated, "
//                            + "the signal: " + action.getSignal().getSignalName());
//                }
				action.updateValue(value);
				action.execute();
			} else {
				actionList.get(i).execute();
//                if (null != logProtocol) {
//                    logProtocol.createGENLog("The action is executed.");
//                }
			}
		}
	}

	/**
	 * Executes actions found in the action map.
	 * 
	 * @param state         the action state
	 * @param value         the value to be updated at runtime if given (only for
	 *                      <code>SendSignalAction</code>)
	 * @param useThreadPool whether it uses the thread pool to execute the actions.
	 * @param logProtocol   the log protocol used to write logs to corresponding
	 *                      files.
	 */
	public void executeActions(ActionState state, Serializable value, boolean useThreadPool,
			LogProtocol logProtocol) {
		ExecutorService executor = null;
		List<IAction> actionList = new ArrayList<IAction>();
		if (actionMap.containsKey(state)) {
			actionList = actionMap.get(state);
		}
//		if (useThreadPool) {
			executor = Executors.newFixedThreadPool(10);
//		}
		for (int i = 0; i < actionList.size(); i++) {
			if (null != value && (actionList.get(i) instanceof SendSignalAction)) {
				SendSignalAction action = (SendSignalAction) actionList.get(i);
				action.updateValue(value);
//				if (useThreadPool) {
					executor.execute(new RunnableAction(action));
//				} else {
//					action.execute();
//				}
			} else {
//				if (useThreadPool) {
					executor.execute(new RunnableAction(actionList.get(i)));
//				} else {
//					actionList.get(i).execute();
//				}
			}
			
//			if (null != logProtocol) {
//				logProtocol.createGENLog("Executed actions: " + actionList.get(i));
//			}
		}
	}
	
	/**
	 * Returns the action map.
	 * 
	 * @return the action map
	 */
	protected Map<ActionState, List<IAction>> getActionMap() {
		return this.actionMap;
	}

}
