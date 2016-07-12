package eu.qualimaster.coordination.commands;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.pipeline.AlgorithmChangeParameter;

/**
 * A command to change an algorithm in an algorithm family.
 * Please note that abstract names are described here while the CoordinationLayer must translate
 * them to actual Execution System commands. 
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmChangeCommand extends AbstractPipelineElementCommand {

    private static final long serialVersionUID = 79134352686021725L;
    private String algorithm;
    private Map<String, Serializable> parameters = new HashMap<String, Serializable>();
    
    /**
     * Crates an algorithm change command.
     * 
     * @param pipeline the name of the pipeline
     * @param pipelineElement the name of the pipeline element
     * @param algorithm the target name of the algorithm
     */
    public AlgorithmChangeCommand(String pipeline, String pipelineElement, String algorithm) {
        super(pipeline, pipelineElement);
        this.algorithm = algorithm;
    }
    
    /**
     * Sets an integer parameter.
     * 
     * @param param the parameter identifier
     * @param value the value
     * @throws IllegalArgumentException in case that the parameter does not accept an integer (or a string as fallback)
     */
    public void setIntParameter(AlgorithmChangeParameter param, int value) {
        AlgorithmChangeParameter.setIntParameter(parameters, param, value);
    }
    
    /**
     * Returns an integer parameter.
     * 
     * @param param the parameter identifier
     * @param dflt the default value in case that the parameter is not specified or cannot be turned into an integer
     *   (may be <b>null</b>)
     * @return the value of <code>param</code>, <code>dflt</code> if not specified / not an integer value
     */
    public Integer getIntParameter(AlgorithmChangeParameter param, Integer dflt) {
        return AlgorithmChangeParameter.getIntParameter(parameters, param, dflt);
    }

    /**
     * Sets a String parameter.
     * 
     * @param param the parameter identifier
     * @param value the value
     * @throws IllegalArgumentException in case that parameter does not accet a String value
     */
    public void setStringParameter(AlgorithmChangeParameter param, String value) {
        AlgorithmChangeParameter.setStringParameter(parameters, param, value);
    }
    
    /**
     * Returns a String parameter.
     * 
     * @param param the parameter identifier
     * @param dflt the default value in case that the parameter is not specified (may be <b>null</b>)
     * @return the value of <code>param</code>, <code>dflt</code> if not specified
     */
    public String getStringParameter(AlgorithmChangeParameter param, String dflt) {
        return AlgorithmChangeParameter.getStringParameter(parameters, param, dflt);
    }

    /**
     * Returns all defined parameters.
     * 
     * @return all parameters
     */
    @QMInternal
    public Map<AlgorithmChangeParameter, Serializable> getParameters() {
        return AlgorithmChangeParameter.convert(parameters);
    }

    /**
     * Sets all given parameters.
     * 
     * @param parameters the parameters to set
     */
    @QMInternal
    public void setParameters(Map<String, Serializable> parameters) {
        this.parameters.putAll(parameters);
    }
    
    /**
     * Returns the target algorithm name.
     * 
     * @return the name of the target algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitAlgorithmChangeCommand(this);
    }

}
