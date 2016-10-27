package eu.qualimaster.monitoring.volumePrediction;

/**
 * Simple class representing the source monitored by the volume prediction. It
 * contains the name of the source (either stock or hashtag) and its volume
 * threshold
 * 
 * @author Andrea Ceroni
 */
public class Source {

    /** The name of the source (either stock or hashtag) */
    private String name;

    /** The threshold used to identify critical volume values */
    private Long threshold;

    public Source(String name, Long threshold) {
        this.name = name;
        this.threshold = threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (this.getClass() != o.getClass())
            return false;

        Source src = (Source) o;
        if (this.name.compareTo(src.getName()) == 0)
            return true;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the threshold
     */
    public Long getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(Long threshold) {
        this.threshold = threshold;
    }
}
