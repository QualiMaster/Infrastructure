package eu.qualimaster.common.switching.determination;

public abstract class AbstractWindowBasedSwitchPoint implements ISwitchPoint {
	protected abstract long determineWindowEnd();
}
