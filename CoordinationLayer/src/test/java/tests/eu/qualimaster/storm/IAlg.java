package tests.eu.qualimaster.storm;

import java.io.Serializable;

/**
 * Represents an algorithm family (QualiMaster-Style).
 * 
 * @author Holger Eichelberger
 */
public interface IAlg {

    /**
     * Represents the algorithm input.
     * 
     * @author Holger Eichelberger
     */
    public interface IAlgInput extends Serializable {
        
        /**
         * Returns the value to be processed.
         * 
         * @return the value
         */
        public int getValue();
    }

    /**
     * Represents the algorithm output.
     * 
     * @author Holger Eichelberger
     */
    public interface IAlgOutput extends Serializable {

        /**
         * The result value.
         * 
         * @param value the result value
         */
        public void setValue(int value);
    }
    
    /**
     * Processes an input value to an output value.
     * 
     * @param input the input value
     * @param output the output value
     */
    public void process(IAlgInput input, IAlgOutput output);
    
}
