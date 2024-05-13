package tests.eu.qualimaster.storm;

import org.slf4j.LoggerFactory;

/**
 * Represents the first version of the algorithm.
 * 
 * @author Holger Eichelberger
 */
public class Alg1 implements IAlg {

    @Override
    public void process(IAlgInput input, IAlgOutput output) {
        output.setValue(input.getValue() * 2);
        LoggerFactory.getLogger(Alg1.class).info("Alg 1 input " + input.getValue());
    }

}
