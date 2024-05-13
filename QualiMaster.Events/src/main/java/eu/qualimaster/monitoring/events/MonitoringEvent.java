package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractEvent;

/**
 * The QualiMaster monitoring event base class.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public abstract class MonitoringEvent extends AbstractEvent {

    private static final long serialVersionUID = 8592210215205919237L;
    private static final String NULL_KEY = new String();
    
    /**
     * Quotes a null reference to an internal object value.
     * 
     * @param string the string to be quoted
     * @return the quoted string
     */
    public static String quoteNull(String string) {
        return string == null ? NULL_KEY : string;
    }

    /**
     * Unquotes a null reference from an internal object value.
     * 
     * @param string the string to be unquoted
     * @return the unquoted string
     */
    public static String unquoteNull(String string) {
        return NULL_KEY.equals(string) ? null : string; // indeed... reference comparison, react on NULL_KEY only
    }

}
