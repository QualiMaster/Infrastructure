package eu.qualimaster.adaptation.internal;

/**
 * Contains the information to be logged for a single adaptation event (from
 * start to enactment).
 * 
 * @author Andrea Ceroni
 */
public class AdaptationUnit {

    /** The time (milliseconds) when the adaptation starts. */
    private long startTime;

    /** The time when the adaptation ends (i.e. when it has been enacted) */
    private long endTime;

    /** The class name of the adaptation event. */
    private String event;

    /**
     * The condition that triggered the adaptation (needed to define success
     * indicators).
     */
    private String condition;

    /** The name of the performed strategy. */
    private String strategy;

    /** The outcome of the performed strategy. */
    private boolean strategySuccess;

    /** The name of the performed tactic. */
    private String tactic;

    /** The outcome of the performed tactic. */
    private boolean tacticSuccess;

    /** The outcome of the adaptation. */
    private boolean adaptationSuccess;

    /** The message id contained within the Coordination Command. */
    private String message;

    /** Default constructor. */
    public AdaptationUnit() {
        this.startTime = -1;
        this.endTime = -1;
        this.event = null;
        this.condition = null;
        this.strategy = null;
        this.strategySuccess = false;
        this.tactic = null;
        this.tacticSuccess = false;
        this.message = null;
        this.adaptationSuccess = false;
    }

    /** Copy constructor.
     * 
     * @param unit the unit to copy
     */
    public AdaptationUnit(AdaptationUnit unit) {
        this.startTime = unit.getStartTime();
        this.endTime = unit.getEndTime();
        this.event = unit.getEvent();
        this.condition = unit.getCondition();
        this.strategy = unit.getStrategy();
        this.strategySuccess = unit.isStrategySuccess();
        this.tactic = unit.getTactic();
        this.tacticSuccess = unit.isTacticSuccess();
        this.message = unit.getMessage();
        this.adaptationSuccess = unit.isAdaptationSuccess();
    }

    /**
     * Returns the start time.
     * 
     * @return the start time
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Defines the start time.
     * 
     * @param startTime the start time
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the end time.
     * 
     * @return the end time
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Defines the end time.
     * 
     * @param endTime the end time
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * Returns the event.
     * 
     * @return the event
     */
    public String getEvent() {
        return event;
    }

    /**
     * Defines the event.
     * 
     * @param event the event
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * Returns the condition.
     * 
     * @return the condition
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Defines the condition.
     * 
     * @param condition the condition
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    /**
     * Returns the strategy.
     * 
     * @return the strategy
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * Defines the strategy.
     * 
     * @param strategy the strategy
     */
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns whether the strategy was successful.
     * 
     * @return whether the strategy was successful
     */
    public boolean isStrategySuccess() {
        return strategySuccess;
    }

    /**
     * Defines whether the strategy was successful.
     * 
     * @param strategySuccess whether the strategy was successful
     */
    public void setStrategySuccess(boolean strategySuccess) {
        this.strategySuccess = strategySuccess;
    }

    /**
     * Returns the tactic.
     * 
     * @return the tactic
     */
    public String getTactic() {
        return tactic;
    }

    /**
     * Changes the tactic.
     * 
     * @param tactic the tactic
     */
    public void setTactic(String tactic) {
        this.tactic = tactic;
    }

    /**
     * Defines whether the tactic was successful.
     * 
     * @return whether the tactic was successful
     */
    public boolean isTacticSuccess() {
        return tacticSuccess;
    }

    /**
     * Defines whether the tactic was successful.
     * 
     * @param tacticSuccess whether the tactic was successful
     */
    public void setTacticSuccess(boolean tacticSuccess) {
        this.tacticSuccess = tacticSuccess;
    }

    /**
     * Returns whether the adaptation execution was successful.
     * 
     * @return whether the adaptation execution was successful
     */
    public boolean isAdaptationSuccess() {
        return adaptationSuccess;
    }

    /**
     * Defines whether the adaptation execution was successful.
     * 
     * @param adaptationSuccess whether the adaptation execution was successful
     */
    public void setAdaptationSuccess(boolean adaptationSuccess) {
        this.adaptationSuccess = adaptationSuccess;
    }

    /**
     * Returns the (failure) message.
     * 
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Changes the (failure) message.
     * 
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
