package eu.qualimaster.coordination.commands;

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;


/**
 * A command to change the parameter of an algorithm family (propagating to the actual algorithm).
 * Please note that abstract names are described here while the CoordinationLayer must translate
 * them to actual Execution System commands. 
 * 
 * @param <T> the type of the parameter (value)
 * @author Holger Eichelberger
 */
public class ParameterChangeCommand<T extends Serializable> extends AbstractPipelineElementCommand {

    private static final long serialVersionUID = -5207837383492321813L;
    private String parameter;
    private T value;
    
    /**
     * Creates a parameter change command.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the name of the pipeline element
     * @param parameter the name of the parameter
     * @param value the desired value
     */
    public ParameterChangeCommand(String pipeline, String pipelineElement, String parameter, T value) {
        super(pipeline, pipelineElement);
        this.parameter = parameter;
        this.value = value;
    }

    /**
     * Returns the addressed parameter.
     * 
     * @return the name of the parameter
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * Returns the intended value of the parameter.
     *  
     * @return the intended value
     */
    public T getValue() {
        return value;
    }

    @QMInternal
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitParameterChangeCommand(this);
    }
    
}
