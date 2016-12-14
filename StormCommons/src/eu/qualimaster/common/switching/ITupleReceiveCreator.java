package eu.qualimaster.common.switching;
/**
 * Tuple receive creator.
 * @author Cui Qin
 *
 */
public interface ITupleReceiveCreator {
    /**
     * Creates a tuple receive handler.
     * @return a tuple receive handler
     */
    public ITupleReceiverHandler create();
}
