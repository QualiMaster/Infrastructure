package eu.qualimaster.base.algorithm;

import java.util.List;
/**
 * Implementation of a general Tuple {@link IGeneralTuple}.
 * @author Cui Qin
 *
 */
public class GeneralTuple implements IGeneralTuple {
    private List<Object> values;
    /**
     * Creates a general Tuple.
     * @param values the list of values
     */
    public GeneralTuple(List<Object> values) {
        this.values = values;
    }
    /**
     * Creates a general Tuple.
     */
    public GeneralTuple() {
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
    
    @Override
    public boolean isGeneralTuple() {
        return true;
    }

}
