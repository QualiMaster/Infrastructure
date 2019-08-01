package eu.qualimaster.common.switching;

import java.util.HashMap;
import java.util.Map;

/**
 * A singleton class that stores the instances of all strategies used in the switch.
 * @author Cui Qin
 *
 */
public class SwitchStrategies {
    private static SwitchStrategies switchStrategiesInstance;
    private static Map<String, IStrategy> strategies = new HashMap<String, IStrategy>();
    
    /**
     * Constructor for the class.
     */
    private SwitchStrategies() {}
    
    /**
     * Returns the instance of the singleton class.
     * @return the instance of the singleton class
     */
    public static SwitchStrategies getInstance() {
        if (null == switchStrategiesInstance) {
            switchStrategiesInstance = new SwitchStrategies();
        } 
        return switchStrategiesInstance;
    }
    
    /**
     * Gets the strategy map.
     * @return a strategy map
     */
    public Map<String, IStrategy> getStrategies() {
        return strategies;
    }
}
