package eu.qualimaster.adaptation.reflective;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Provides methods to read objects from files/strings.
 * 
 * @author Andrea Ceroni
 */
public class ReadingUtils {

    private static final String SEPARATOR = "\t";
    private static final String PIPELINE_TAG = "pipeline:";
    private static final String NODE_TAG = "node:";
    private static final NumberFormat DEFAULT_NUMBER_FORMAT = NumberFormat
            .getNumberInstance(Locale.GERMANY);

    private int counter;
    private NumberFormat numberFormat;

    /**
     * Constructor
     * 
     * @param numberFormat the format used to read numbers
     */
    public ReadingUtils(NumberFormat numberFormat) {
        this.counter = 0;
        this.numberFormat = numberFormat;
    }

    /**
     * Default constructor
     */
    public ReadingUtils() {
        this(DEFAULT_NUMBER_FORMAT);
    }

    /**
     * Create a <code>Platform</code> object from a string.
     * 
     * @param string the string to be parsed
     * @return the <code>Platform</code> object
     */
    public MonitoringUnit readMonitoringUnit(String string) {
        MonitoringUnit unit = new MonitoringUnit();

        try {
            String[] fields = string.split(SEPARATOR, -1);
            this.counter = 2;
            unit.setTimestamp(Long.valueOf(fields[0]));
            unit.setPlatform(readPlatform(fields));
            this.counter = 0;

            return unit;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Platform readPlatform(String[] fields) throws ParseException {
        Platform platform = new Platform();

        // set platform name
        platform.setName(fields[this.counter]);

        // set platform measures
        ArrayList<Double> measures = new ArrayList<>();
        while (fields[++this.counter].compareTo(PIPELINE_TAG) != 0) {
            if (fields[this.counter].compareTo("") != 0
                    && fields[this.counter].compareTo(" ") != 0) {
                double value = this.numberFormat.parse(fields[this.counter])
                        .doubleValue();
                measures.add(value);
            } else {
                measures.add(-1.0);
            }
        }
        platform.setMeasures(measures);

        // set platform pipelines
        ArrayList<Pipeline> pipelines = new ArrayList<>();
        while (this.counter < fields.length - 1) {
            this.counter++;
            pipelines.add(readPipeline(fields));
        }
        platform.setPipelines(pipelines);

        return platform;
    }

    private Pipeline readPipeline(String[] fields) throws ParseException {
        Pipeline pipeline = new Pipeline();

        // set pipeline name
        pipeline.setName(fields[this.counter]);

        // set pipeline measures
        ArrayList<Double> measures = new ArrayList<>();
        while (fields[++this.counter].compareTo(NODE_TAG) != 0) {
            if (fields[this.counter].compareTo("") != 0
                    && fields[this.counter].compareTo(" ") != 0) {
                double value = this.numberFormat.parse(fields[this.counter])
                        .doubleValue();
                measures.add(value);
            } else {
                measures.add(-1.0);
            }
        }
        pipeline.setMeasures(measures);

        // set pipeline nodes
        ArrayList<Node> nodes = new ArrayList<>();
        while (this.counter < fields.length - 1
                && fields[this.counter].compareTo(PIPELINE_TAG) != 0) {
            this.counter++;
            nodes.add(readNode(fields));
        }
        pipeline.setNodes(nodes);

        return pipeline;
    }

    private Node readNode(String[] fields) throws ParseException {
        Node node = new Node();

        // set node name
        node.setName(fields[this.counter]);

        // set node measures
        ArrayList<Double> measures = new ArrayList<>();
        while (++this.counter < fields.length - 1
                && fields[this.counter].compareTo(NODE_TAG) != 0
                && fields[this.counter].compareTo(PIPELINE_TAG) != 0) {
            if (fields[this.counter].compareTo("") != 0
                    && fields[this.counter].compareTo(" ") != 0) {
                double value = this.numberFormat.parse(fields[this.counter])
                        .doubleValue();
                measures.add(value);
            } else {
                measures.add(-1.0);
            }
        }
        node.setMeasures(measures);

        return node;
    }

    /**
     * @return the numberFormat
     */
    public NumberFormat getNumberFormat() {
        return numberFormat;
    }
}
