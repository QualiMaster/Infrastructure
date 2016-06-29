package eu.qualimaster.base.algorithm;

import java.util.List;
/**
 * Tuple for the algorithm switch.
 * @author qin
 *
 */
public interface ISwitchTuple {
    /**
     * Sets the tuple id.
     * @param id the tuple id
     */
    public void setId(int id);
    /**
     * Returns the tuple id.
     * @return the tuple id
     */
    public int getId();
    /**
     * Sets the values for a tuple.
     * @param values the tuple values
     */
    public void setValues(List<Object> values);
    /**
     * Return the list of values in a tuple.
     * @return the list of values
     */
    public List<Object> getValues();
    /**
     * Return the value located in a specific index of the list of values. 
     * @param index the index in which the value is located
     * @return the value 
     */
    public Object getValue(int index);
}
