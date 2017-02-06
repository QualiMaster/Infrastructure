package eu.qualimaster.adaptation.reflective;

/**
 * Represents a pattern used within reflective adaptation (set of features + label).
 * 
 * @author  Andrea Ceroni
 */
public class Pattern {
    
    /** The ID of the pattern */
    private String id;
    
    /** The features (from both monitoring and adaptation logs */
    private Features features;
    
    /** The label (representing the optimal threshold for switching from SW to HW) */
    private double label;
    
    /**
     * Default constructor
     */
    public Pattern(){
        this.id = "missing";
        this.features = new Features();
        this.label = -1;
    }
    
    /**
     * Constructor with input features and label.
     * @param features the features of the pattern.
     * @param label the label of the pattern.
     */
    public Pattern(String id, Features features, double label){
        this.id = id;
        this.features = features;
        this.label = label;
    }
    
    /**
     * Copy constructor.
     * @param p the pattern to be copied.
     */
    public Pattern (Pattern p){
        this.id = p.getId();
        this.features = new Features(p.getFeatures());
        this.label = p.getLabel();
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

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }
}
