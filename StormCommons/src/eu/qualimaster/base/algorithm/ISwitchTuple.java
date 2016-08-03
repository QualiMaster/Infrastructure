package eu.qualimaster.base.algorithm;
/**
 * A switch Tuple containing an identifier for the algorithm switch.
 * @author qin
 *
 */
public interface ISwitchTuple extends IGeneralTuple {
    /**
     * Sets the tuple id.
     * @param id the tuple id
     */
    public void setId(long id);
    /**
     * Returns the tuple id.
     * @return the tuple id
     */
    public long getId();
}
