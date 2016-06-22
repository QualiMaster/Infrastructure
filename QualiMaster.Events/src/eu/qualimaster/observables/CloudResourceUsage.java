package eu.qualimaster.observables;

/**
 * Cloud observables.
 * 
 * @author Bendix Harries
 */
public enum CloudResourceUsage implements IObservable {

    // partly overlaps with ResourceUsage, but we accept this for now to avoid overlaps
    
    PING,
    BANDWIDTH,
    USED_HARDDISC_MEM,
    USED_PROCESSORS,
    USED_WORKING_STORAGE;

    @Override
    public boolean isInternal() {
        return false;
    }

}
