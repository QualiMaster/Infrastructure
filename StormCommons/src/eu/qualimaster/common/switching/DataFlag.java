package eu.qualimaster.common.switching;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.algorithm.ISwitchTuple;
/**
 * Defines the data flag indicating the tuple type, i.e., {@link IGeneralTuple} and {@link ISwitchTuple}, 
 * as well as the queue to be used for storing received data.
 * @author Cui Qin
 *
 */
public class DataFlag {
    /**
     * The flag indicating the {@link ISwitchTuple} data type.
     */
    public static final String SWITCH_TUPLE_FLAG = "swiTuple";
    /**
     * The flag indicating the {@link IGeneralTuple} data type.
     */
    public static final String GENERAL_TUPLE_FLAG = "genTuple";
    /**
     * The flag indicating the general queue shall be used.
     */
    public static final String GENERAL_QUEUE_FLAG = "genQueue";
    /**
     * The flag indicating the switch queue shall be used.
     */
    public static final String TEMPORARY_QUEUE_FLAG = "tmpQueue";
}
