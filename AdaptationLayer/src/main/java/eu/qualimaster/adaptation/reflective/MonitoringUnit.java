package eu.qualimaster.adaptation.reflective;

/**
 * Contains the information present in the monitoring log.
 * 
 * @author Andrea Ceroni
 */
public class MonitoringUnit {

    /** The timestamp of the monitoring */
    private long timestamp;

    /** The monitored platform */
    private Platform platform;

    /**
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the platform
     */
    public Platform getPlatform() {
        return platform;
    }

    /**
     * @param platform the platform to set
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
}
