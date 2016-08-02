package eu.qualimaster.base.serializer;

import java.io.IOException;
import java.util.Map;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import backtype.storm.serialization.KryoValuesDeserializer;
import backtype.storm.serialization.KryoValuesSerializer;
import eu.qualimaster.base.algorithm.GeneralTuple;
import eu.qualimaster.base.algorithm.IGeneralTuple;
/**
 * Implementation of the kryo serializer of the general Tuple {@link IGeneralTupleSerializer}.
 * @author Cui Qin
 *
 */
public class KryoGeneralTupleSerializer implements IGeneralTupleSerializer {
    private KryoValuesSerializer valuesSer;
    private KryoValuesDeserializer valuesDeser;
    private Output kryoOut;
    private Input kryoIn;
    /**
     * Creates the kryo serializer for the general tuple.
     * @param conf the Storm configuration file
     */
    public KryoGeneralTupleSerializer(final Map conf) {
        this.valuesSer = new KryoValuesSerializer(conf);
        this.valuesDeser = new KryoValuesDeserializer(conf);
        this.kryoIn = new Input(1);
        this.kryoOut = new Output(2000, 2000000000);
    }
    @Override
    public byte[] serialize(IGeneralTuple tuple) {
        kryoOut.clear();
        try {
            valuesSer.serializeInto(tuple.getValues(), kryoOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return kryoOut.toBytes();
    }

    @Override
    public IGeneralTuple deserialize(byte[] ser) {
        kryoIn.setBuffer(ser);
        IGeneralTuple tuple = new GeneralTuple(valuesDeser.deserializeFrom(kryoIn));
        return tuple;
    }

}
