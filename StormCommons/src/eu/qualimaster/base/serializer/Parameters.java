package eu.qualimaster.base.serializer;

import eu.qualimaster.dataManagement.serialization.SerializerRegistry;

/**
 * Define a parameter class.
 * @author qin
 *
 */
public class Parameters {
    /**
     * Define a integer parameter class.
     * @author qin
     *
     */
    public static class IntegerParameter extends AbstractParameter<Integer> {
        static {
            SerializerRegistry.register("IntegerParameter", ParameterSerializers.IntegerParameterSerializer.class);
        }
    }
    /**
     * Define a string parameter class.
     * @author qin
     *
     */
    public static class StringParameter extends AbstractParameter<String> {
        static {
            SerializerRegistry.register("StringParameter", ParameterSerializers.StringParameterSerializer.class);
        }
    }
    /**
     * Define a boolean parameter class.
     * @author qin
     *
     */
    public static class BooleanParameter extends AbstractParameter<Boolean> { 
        static {
            SerializerRegistry.register("BooleanParameter", ParameterSerializers.BooleanParameterSerializer.class);
        }
    }
    /**
     * Define a long parameter class.
     * @author qin
     *
     */
    public static class LongParameter extends AbstractParameter<Long> {
        static {
            SerializerRegistry.register("LongParameter", ParameterSerializers.LongParameterSerializer.class);
        }
    }
    /**
     * Define a real parameter class.
     * @author qin
     *
     */
    public static class RealParameter extends AbstractParameter<Double> {
        static {
            SerializerRegistry.register("RealParameter", ParameterSerializers.RealParameterSerializer.class);
        }
    }
}
