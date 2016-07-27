package eu.qualimaster.monitoring.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.QMGenerics;
import eu.qualimaster.common.QMInternal;

/**
 * Informs the adaptation layer about violated constraints. 
 *  
 * @author Holger Eichelberger
 */
public class ConstraintViolationAdaptationEvent extends AdaptationEvent {

    private static final long serialVersionUID = 8296772128610605669L;
    private List<ViolatingClause> violating;
    private FrozenSystemState state;
    
    /**
     * Creates a regular adaptation event.
     * 
     * @param violating the violating clauses
     * @param state the system state to perform adaptation on
     */
    @QMInternal
    public ConstraintViolationAdaptationEvent(Collection<ViolatingClause> violating, FrozenSystemState state) {
        this.violating = new ArrayList<ViolatingClause>();
        this.violating.addAll(violating);
        this.state = state;
    }
    
    /**
     * Returns the number of violating clauses.
     * 
     * @return the number of violating clauses
     */
    public int getViolatingClauseCount() {
        return violating.size();
    }
    
    /**
     * Returns the specified violating clause.
     * 
     * @param index the 0-based index
     * @return the specified violating clause
     * @throws IndexOutOfBoundsException in case that 
     *   <code>index &lt;0  || index &gt;={@link #getViolatingClauseCount()}</code>
     */
    public ViolatingClause getViolatingClause(int index) {
        return violating.get(index);
    }
    
    /**
     * Returns all violating clauses.
     * 
     * @return all violating clauses
     */
    @QMGenerics(types = ViolatingClause.class)
    public Iterator<ViolatingClause> getViolatingClauses() {
        return violating.iterator();
    }

    /**
     * Returns the system state.
     * 
     * @return the system state
     */
    @QMInternal
    public FrozenSystemState getState() {
        return state;
    }
    
}
