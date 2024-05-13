package eu.qualimaster.base.serializer;

import eu.qualimaster.base.algorithm.ISwitchTuple;
/**
 * Serializer for the switch tuple.
 * @author qin
 *
 */
public interface ISwitchTupleSerializer {
    /**
     * Serializes the switch tuple.
     * @param tuple the switch tuple
     * @return the serialized bytes
     */
    public byte[] serialize(ISwitchTuple tuple);
    /**
     * Deserializes the switch tuple.
     * @param ser the switch tuple bytes.
     * @return the switch tuple
     */
    public ISwitchTuple deserialize(byte[] ser);
}
