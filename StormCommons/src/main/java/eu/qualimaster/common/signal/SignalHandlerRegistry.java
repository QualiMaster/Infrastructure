package eu.qualimaster.common.signal;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * A registry for signal handlers.
 * @author Cui Qin
 *
 */
public class SignalHandlerRegistry {
    private static Logger logger = Logger.getLogger(SignalHandlerRegistry.class);
    private static Map<String, Class<? extends ISignalHandler>> handlers = 
            new HashMap<String, Class<? extends ISignalHandler>>();
    
    /**
     * Registers a handler for a given key.
     * @param key the combination of the signal name and the node name 
     * @param handlerCls the class of the signal handler
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized boolean register(String key, Class<? extends ISignalHandler> handlerCls) {
        boolean successful = false;
        if (null != handlerCls) {
            if (null != key) {
                Class<? extends ISignalHandler> registered = handlers.get(key);
                if (null == registered) {
                    handlers.put(key, handlerCls);
                }
                successful = true;
            }
        }
        return successful;
    }
    
    /**
     * Unregisters a handler for a given key.
     * @param key the combination of the signal name and the node name
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized boolean unregister(String key) {
        boolean successful = false;
        if (null != key) {
            Class<? extends ISignalHandler> handlerCls = handlers.get(key);
            if (null == handlerCls) {
                successful = false;
            } else {
                successful = null != handlers.remove(key);
            }
        }
        return successful;
    }
    
    /**
     * Returns a signal handler for <code>key</code>.
     * @param key the key to search for a signal handler
     * @return the signal handler or <b>null</b> if none was found
     */
    public static synchronized Class<? extends ISignalHandler> getSignalHandler(String key) {
        Class<? extends ISignalHandler> result;
        if (null == key) {
            result = null;
        } else {
            result = handlers.get(key);
            if (null == result) {
                logger.warn("No signal handler for key = " + key);
            }
        }
        return result;
    }
}
