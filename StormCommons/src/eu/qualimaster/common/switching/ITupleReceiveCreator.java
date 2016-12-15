package eu.qualimaster.common.switching;
/**
 * Tuple receive creator.
 * @author Cui Qin
 *
 */
public interface ITupleReceiveCreator {
    /**
     * Creates a tuple receive handler.
     * 
     * @param switchHandler whether a switch or a general tuple handler shall be created
     * @return a tuple receive handler
     */
    public ITupleReceiverHandler create(boolean switchHandler);

}
