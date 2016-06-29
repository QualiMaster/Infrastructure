package eu.qualimaster.base.serializer;

/**
 * Deinfes an abstract parameter.
 * @author qin
 *
 * @param <T>
 */
public abstract class AbstractParameter<T> implements IParameter<T> {
    private String name;
    private T value;
    /**
     * Returns the parameter name.
     * @return name the parameter name
     */
    public String getName() {
        return name;
    }
    /**
     * Sets the parameter name.
     * @param name the parameter name
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * Returns the parameter value.
     * @return value the parameter value
     */
    public T getValue() {
        return value;
    }
    /**
     * Sets the parameter value.
     * @param value the parameter value
     */
    public void setValue(T value) {
        this.value = value;
    }
    /**
     * Shows the integer parameter information as String.
     * @return string of the integer parameter
     */
    public String toString() {
        return "Parameter: " + this.getName() + ", " + this.value;
    }
}
