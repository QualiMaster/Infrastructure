package tests.eu.qualimaster.storm;

/**
 * Represents the second version of the algorithm.
 * 
 * @author Holger Eichelberger
 */
public class Alg2 implements IAlg {

    @Override
    public void process(IAlgInput input, IAlgOutput output) {
        output.setValue(input.getValue() >> 1);
    }

}
