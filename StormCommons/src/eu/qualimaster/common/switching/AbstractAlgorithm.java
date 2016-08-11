package eu.qualimaster.common.switching;
/**
 * An abstract algorithm to be switched.
 * @author Cui Qin
 *
 */
public abstract class AbstractAlgorithm {
    private String name;
    /**
     * Creates an algorithm.
     * @param name the algorithm name
     */
    public AbstractAlgorithm(String name) {
        this.name = name;
    }
    /**
     * Returns the algorithm name.
     * @return the algorithm name
     */
    public String getName() {
        return name;
    }
    /**
     * Sets the algorithm name.
     * @param name the algorithm name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    
}
