package eu.qualimaster.base.serializer;

import eu.qualimaster.base.algorithm.IGeneralTuple;
/**
 * Serializer for the general Tuple {@link IGeneralTuple}.
 * @author Cui Qin
 *
 */
public interface IGeneralTupleSerializer {
    /**
     * Serializes the general tuple {@link IGeneralTuple}.
     * @param tuple the general tuple
     * @return the serialized bytes
     */
    public byte[] serialize(IGeneralTuple tuple);
    /**
     * Deserializes the general tuple {@link IGeneralTuple}.
     * @param ser the general tuple bytes.
     * @return the general tuple
     */
    public IGeneralTuple deserialize(byte[] ser);
}
