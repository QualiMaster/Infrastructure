package eu.qualimaster.base.algorithm;

import java.util.List;

/**
 * Implementation of the switch tuple.
 * @author qin
 *
 */
public class SwitchTuple implements ISwitchTuple {
    private int id;
    private List<Object> values;
    
    /**
     * Creates a switch tuple with id and values.
     * @param id the tuple id
     * @param values the tuple values
     */
    public SwitchTuple(int id, List<Object> values) {
        this.id = id;
        this.values = values;
    }
    
    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setValues(List<Object> values) {
        this.values = values;
    }

    @Override
    public List<Object> getValues() {
        return values;
    }

    @Override
    public Object getValue(int index) {
        return values.get(index);
    }

}
