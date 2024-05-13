package eu.qualimaster.monitoring.events;

import eu.qualimaster.common.QMInternal;

/**
 * Notifies the infrastructure about active reconfigurable hardware.
 * This class will be revised according to future needs and is currently
 * intended only for demonstration. The transmitted identification may be
 * used for display or just be ignored.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class HardwareAliveEvent extends MonitoringEvent {

    private static final long serialVersionUID = 6058259030515144142L;
    private String identifier;
    
    /**
     * Creates a hardware alive event.
     * 
     * @param identifier an arbitrary identifier for the hardware (no mapping will happen, 
     * <b>null</b> is turned into an empty string)
     */
    public HardwareAliveEvent(String identifier) {
        this.identifier = identifier == null ? "" : identifier;
    }
    
    /**
     * Returns an identifier for the hardware.
     * 
     * @return the hardware identifier
     */
    public String getIdentifier() {
        return identifier;
    }
    
}
