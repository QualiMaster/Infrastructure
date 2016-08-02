package eu.qualimaster.base.algorithm;

import java.util.List;

/**
 * Implementation of the switch tuple {@link ISwitchTuple}.
 * @author qin
 *
 */
public class SwitchTuple extends GeneralTuple implements ISwitchTuple {
    private long id;
    private List<Object> values;
    /**
     * Creates a switch tuple with id and values.
     * @param id the tuple id
     * @param values the tuple values
     */
    public SwitchTuple(long id, List<Object> values) {
        super(values);
        this.id = id;
    }
    
    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public boolean isGeneralTuple() {
        return false;
    }

}
