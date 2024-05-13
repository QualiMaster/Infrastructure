package eu.qualimaster.base.serializer;

/**
 * Defines the parameter interface.
 * @author qin
 *
 * @param <T>
 */
public interface IParameter<T> {
    /**
     * Sets the parameter name.
     * @param name the parameter name
     */
    public void setName(String name);
    /**
     * Returns the parameter name.
     * @return the parameter name
     */
    public String getName();
    /**
     * Sets the parameter value.
     * @param value the parameter value
     */
    public void setValue(T value);
    /**
     * Returns the parameter value.
     * @return the parameter value
     */
    public T getValue();
    /**
     * Shows the integer parameter information as String.
     * @return string of the integer parameter
     */
    public String toString();
}
