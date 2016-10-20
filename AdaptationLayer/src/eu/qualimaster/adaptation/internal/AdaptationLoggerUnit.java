package eu.qualimaster.adaptation.internal;

/**
 * Contains the information to be logged for a single adaptation event (from start to enactment)
 * 
 * @author  Andrea Ceroni
 */
public class AdaptationLoggerUnit {
	
	/** The time (milliseconds) when the adaptation starts */
	private long startTime;
	
	/** The time when the adaptation ends (i.e. when it has been enacted) */
	private long endTime;
	
	/** The class name of the adaptation event */
	private String event;
	
	/** The condition that triggered the adaptation (needed to define success indicators) */
	private String condition;
	
	/** The name of the performed strategy */
	private String strategy;
	
	/** The outcome of the performed strategy */
	private boolean strategySuccess;
	
	/** The name of the performed tactic */
	private String tactic;
	
	/** The outcome of the performed tactic */
	private boolean tacticSuccess;
	
	/** The outcome of the adaptation */
	private boolean adaptationSuccess;
	
	/** The message id contained within the Coordination Command */
	private String message;
	
	/** Default constructor */
	public AdaptationLoggerUnit(){
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
	
	/** Copy constructor */
	public AdaptationLoggerUnit(AdaptationLoggerUnit unit){
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

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getStrategy() {
		return strategy;
	}

	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}

	public boolean isStrategySuccess() {
		return strategySuccess;
	}

	public void setStrategySuccess(boolean strategySuccess) {
		this.strategySuccess = strategySuccess;
	}

	public String getTactic() {
		return tactic;
	}

	public void setTactic(String tactic) {
		this.tactic = tactic;
	}

	public boolean isTacticSuccess() {
		return tacticSuccess;
	}

	public void setTacticSuccess(boolean tacticSuccess) {
		this.tacticSuccess = tacticSuccess;
	}

	public boolean isAdaptationSuccess() {
		return adaptationSuccess;
	}

	public void setAdaptationSuccess(boolean adaptationSuccess) {
		this.adaptationSuccess = adaptationSuccess;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
