package eu.qualimaster.base.algorithm;

import java.util.List;
/**
 * A general Tuple containing a list of object. 
 * @author Cui Qin
 *
 */
public interface IGeneralTuple {
    /**
     * Sets the values for a tuple.
     * @param values the tuple values
     */
    public void setValues(List<Object> values);
    /**
     * Returns the list of values in a tuple.
     * @return the list of values
     */
    public List<Object> getValues();
    /**
     * Returns the value located in a specific index of the list of values. 
     * @param index the index in which the value is located
     * @return the value 
     */
    public Object getValue(int index);
    /**
     * Returns whether it is a general tuple.
     * @return a boolean value indicating whether it is a general tuple
     */
    public boolean isGeneralTuple();
}
