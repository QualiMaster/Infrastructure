package eu.qualimaster.adaptation.reflective;

/**
 * Represents a pattern used within reflective adaptation (set of features + label).
 * 
 * @author  Andrea Ceroni
 */
public class Pattern {

    /** The features (from both monitoring and adaptation logs */
    private Features features;
    
    /** The label (representing the optimal threshold for switching from SW to HW) */
    private double label;
    
    /**
     * Default constructor
     */
    public Pattern(){
        this.features = new Features();
        this.label = -1;
    }
    
    /**
     * Constructor with input features and label.
     * @param features the features of the pattern.
     * @param label the label of the pattern.
     */
    public Pattern(Features features, double label){
        this.features = features;
        this.label = label;
    }

    /**
     * @return the features
     */
    public Features getFeatures() {
        return features;
    }

    /**
     * @param features the features to set
     */
    public void setFeatures(Features features) {
        this.features = features;
    }

    /**
     * @return the label
     */
    public double getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(double label) {
        this.label = label;
    }
}
