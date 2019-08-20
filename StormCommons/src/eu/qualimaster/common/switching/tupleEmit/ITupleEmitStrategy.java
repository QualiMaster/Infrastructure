package eu.qualimaster.common.switching.tupleEmit;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.switching.IStrategy;

/**
 * An interface of the tuple emit strategy.
 * @author Cui Qin
 *
 */
public interface ITupleEmitStrategy extends IStrategy {
    /**
     * Return the next tuple to be emitted.
     * @return the next tuple to be emitted
     */
    public ISwitchTuple nextEmittedTuple();
}
