package eu.qualimaster.adaptation.reflective;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import eu.qualimaster.adaptation.internal.AdaptationUnit;

/**
 * Provides methods to read objects from files/strings.
 * 
 * @author Andrea Ceroni
 */
public class ReadUtils {

    private static final String SEPARATOR = "\t";
    private static final String PLATFORM_TAG = "platform:";
    private static final String PIPELINE_TAG = "pipeline:";
    private static final String NODE_TAG = "node:";
    private static final NumberFormat DEFAULT_NUMBER_FORMAT = NumberFormat
            .getNumberInstance(Locale.GERMANY);

    private static final Map<String, HashSet<String>> DEFAULT_MEASURES_TO_IGNORE = Collections.unmodifiableMap(
            new HashMap<String, HashSet<String>>() {{
                put("platform", new HashSet<String>(Arrays.asList("AVAILABLE_DFES", "BANDWIDTH", "USED_DFES")));
                put("pipeline", new HashSet<String>(Arrays.asList("ACCURACY_CONFIDENCE", "ACCURACY_ERROR_RATE", 
                        "IS_ENACTING", "IS_VALID", "USED_CPUS", "USED_DFES", "VARIETY", "VELOCITY", "VOLATILITY", "VOLUME")));
                put("pipeline node", new HashSet<String>(Arrays.asList("ACCURACY_CONFIDENCE", "BELIEVABILITY", 
                        "COMPLETENESS", "IS_ENACTING", "IS_VALID", "USED_CPUS", "USED_DFES", "RELEVANCY", "USED_MEMORY", "VARIETY", 
                        "VELOCITY", "VOLATILITY", "VOLUME")));
            }});
    
    private int counter;
    private NumberFormat numberFormat;
    private HashMap<String, ArrayList<String>> originalHeader;
    private HashMap<String, ArrayList<String>> filteredHeader;
    Map<String, HashSet<String>> measuresToIgnore;

    /**
     * Constructor
     * 
     * @param numberFormat the format used to read numbers
     * @param measuresToIgnore the sets of measures to ignore (one set for each component)
     */
    public ReadUtils(NumberFormat numberFormat, Map<String, HashSet<String>> measuresToIgnore) {
        this.counter = 0;
        this.numberFormat = numberFormat;
        this.originalHeader = new HashMap<>();
        this.filteredHeader = new HashMap<>();
        this.measuresToIgnore = measuresToIgnore;
    }

    /**
     * Default constructor
     */
    public ReadUtils() {
        this(DEFAULT_NUMBER_FORMAT, DEFAULT_MEASURES_TO_IGNORE);
    }
    
    /**
     * Constructor
     * 
     * @param numberFormat the format used to read numbers
     * @param measuresToIgnore the sets of measures to ignore (one set for each component)
     * @param headers the headers indicating the observable for each component
     */
    public ReadUtils(NumberFormat numberFormat, Map<String, HashSet<String>> measuresToIgnore, Map<String, ArrayList<String>> headers) {
        this.counter = 0;
        this.numberFormat = numberFormat;
        this.originalHeader = readHeader(headers);
        this.filteredHeader = readFilteredHeader(headers);
        this.measuresToIgnore = measuresToIgnore;
    }
    
    /**
     * Constructor
     * 
     * @param headers the headers indicating the observable for each component
     */
    public ReadUtils(Map<String, ArrayList<String>> headers) {
        this(DEFAULT_NUMBER_FORMAT, DEFAULT_MEASURES_TO_IGNORE, headers);
    }
    
    /**
     * Parses a whole monitoring log and extracts one monitoring unit from each line.
     * @param filePath The path of the file (monitoring log) to be parsed.
     * @return A list of <code>MonitoringUnit</code> objects.
     */
    public ArrayList<MonitoringUnit> readMonitoringUnits(String filePath){
        ArrayList<MonitoringUnit> units = new ArrayList<>();
        BufferedReader reader = null;
        
        try{
            reader = new BufferedReader(new FileReader(filePath));
            
            // read the headers of each component of the unit
            ArrayList<String> headerLines = new ArrayList<>();
            String line = null;
            while(!(line = reader.readLine()).isEmpty()){
                headerLines.add(line);
            }
            this.originalHeader = readHeader(headerLines);
            this.filteredHeader = readFilteredHeader(headerLines);
            
            // read the units
            while((line = reader.readLine()) != null){
                units.add(readMonitoringUnit(line));
            }
            reader.close();
            
            return units;
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parses a whole adaptation log and extracts one <code>AdaptationUnit</code> from each line.
     * @param filePath The path of the file (adaptation log) to be parsed.
     * @return A list of <code>AdaptationUnit</code> objects.
     */
    public List<AdaptationUnit> readAdaptationUnits(String filePath){
        List<AdaptationUnit> units = new ArrayList<>();
        BufferedReader reader = null;
        
        try{
            reader = new BufferedReader(new FileReader(filePath));
            reader.readLine();
            
            String line = null;
            while((line = reader.readLine()) != null){
                units.add(readAdaptationUnit(line));
            }
            reader.close();
            
            return units;
        }
        catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates an <code>AdaptationUnit</code> object from a string.
     * 
     * @param string the string to be parsed
     * @return the <code>AdaptationUnit</code> object
     */
    public AdaptationUnit readAdaptationUnit(String string) {
        AdaptationUnit unit = new AdaptationUnit();

        try {
            String[] fields = string.split(SEPARATOR, -1);
            
            unit.setStartTime(Long.valueOf(fields[0]));
            unit.setEndTime(Long.valueOf(fields[1]));
            unit.setEvent(fields[2]);
            unit.setCondition(fields[3]);
            unit.setStrategy(fields[4]);
            unit.setStrategySuccess(Boolean.valueOf(fields[5]));
            unit.setTactic(fields[6]);
            unit.setTacticSuccess(Boolean.valueOf(fields[7]));
            unit.setMessage(fields[8]);
            unit.setAdaptationSuccess(Boolean.valueOf(fields[9]));
            
            return unit;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a <code>MonitoringUnit</code> object from a string.
     * 
     * @param string the string to be parsed
     * @return the <code>MonitoringUnit</code> object
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
    
    private HashMap<String, ArrayList<String>> readHeader(ArrayList<String> headerLines){
        HashMap<String, ArrayList<String>> header = new HashMap<>();
        
        for(String line : headerLines){
            String[] fields = line.split("format:");
            String componentName = fields[0].trim();
            String[] valueNamesFields = fields[1].split("\t");
            
            ArrayList<String> valueNames = new ArrayList<>();
            for(int i = 1; i < valueNamesFields.length; i++){
                valueNames.add(valueNamesFields[i]);
            }
            
            header.put(componentName, valueNames);
        }
        
        return header;
    }
    
    private HashMap<String, ArrayList<String>> readHeader(Map<String, ArrayList<String>> headerLines){
        HashMap<String, ArrayList<String>> header = new HashMap<>();
        
        for(String headerName : headerLines.keySet()){
            String[] fields = headerName.split("format:");
            String componentName = fields[0].trim();
            
            header.put(componentName, headerLines.get(headerName));
        }
        
        return header;
    }
    
    private HashMap<String, ArrayList<String>> readFilteredHeader(ArrayList<String> headerLines){
        HashMap<String, ArrayList<String>> header = new HashMap<>();
        
        for(String line : headerLines){
            String[] fields = line.split("format:");
            String componentName = fields[0].trim();
            String[] valueNamesFields = fields[1].split("\t");
            
            HashSet<String> toIgnore = this.measuresToIgnore.get(componentName);
            ArrayList<String> valueNames = new ArrayList<>();
            for(int i = 1; i < valueNamesFields.length; i++){
                if(!toIgnore.contains(valueNamesFields[i])) valueNames.add(valueNamesFields[i]);
            }
            
            header.put(componentName, valueNames);
        }
        
        return header;
    }
    
    private HashMap<String, ArrayList<String>> readFilteredHeader(Map<String, ArrayList<String>> headerLines){
        HashMap<String, ArrayList<String>> header = new HashMap<>();
        
        for(String headerName : headerLines.keySet()){
            String[] fields = headerName.split("format:");
            String componentName = fields[0].trim();
            
            HashSet<String> toIgnore = this.measuresToIgnore.get(componentName);
            ArrayList<String> valueNames = new ArrayList<>();
            for(int i = 1; i < headerLines.get(headerName).size(); i++){
                if(!toIgnore.contains(headerLines.get(headerName).get(i))) valueNames.add(headerLines.get(headerName).get(i));
            }
            
            header.put(componentName, valueNames);
        }
        
        return header;
    }

    private Platform readPlatform(String[] fields) throws ParseException {
        Platform platform = new Platform();
        HashSet<String> toIgnore = this.measuresToIgnore.get(PLATFORM_TAG.replace(":", ""));

        // set platform name and header
        platform.setName(fields[this.counter]);
        platform.setMeasuresNames(this.filteredHeader.get(PLATFORM_TAG.replace(":", "")));

        // set platform measures
        ArrayList<Double> measures = new ArrayList<>();
        ArrayList<String> originalHeader = this.originalHeader.get(PLATFORM_TAG.replace(":", ""));
        int valueIndex = 0;
        while (fields[++this.counter].compareTo(PIPELINE_TAG) != 0) {
            if (fields[this.counter].compareTo("") != 0
                    && fields[this.counter].compareTo(" ") != 0
                    && !toIgnore.contains(originalHeader.get(valueIndex))) {
                double value = this.numberFormat.parse(fields[this.counter])
                        .doubleValue();
                measures.add(value);
            } else {
                if(!toIgnore.contains(originalHeader.get(valueIndex)))
                    measures.add(-1.0);
            }
            valueIndex++;
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
        HashSet<String> toIgnore = this.measuresToIgnore.get(PIPELINE_TAG.replace(":", ""));

        // set pipeline name and header
        pipeline.setName(fields[this.counter]);
        pipeline.setMeasuresNames(this.filteredHeader.get(PIPELINE_TAG.replace(":", "")));

        // set pipeline measures
        ArrayList<Double> measures = new ArrayList<>();
        ArrayList<String> originalHeader = this.originalHeader.get(PIPELINE_TAG.replace(":", ""));
        int valueIndex = 0;
        while (fields[++this.counter].compareTo(NODE_TAG) != 0) {
            if (fields[this.counter].compareTo("") != 0
                    && fields[this.counter].compareTo(" ") != 0
                    && !toIgnore.contains(originalHeader.get(valueIndex))) {
                double value = this.numberFormat.parse(fields[this.counter])
                        .doubleValue();
                measures.add(value);
            } else {
                if(!toIgnore.contains(originalHeader.get(valueIndex)))
                    measures.add(-1.0);
            }
            valueIndex++;
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
        HashSet<String> toIgnore = this.measuresToIgnore.get("pipeline " + NODE_TAG.replace(":", ""));
        
        // set node name and header
        node.setName(fields[this.counter]);
        node.setMeasuresNames(this.filteredHeader.get("pipeline " + NODE_TAG.replace(":", "")));

        // set node measures
        ArrayList<Double> measures = new ArrayList<>();
        ArrayList<String> originalHeader = this.originalHeader.get("pipeline " + NODE_TAG.replace(":", ""));
        int valueIndex = 0;
        while (++this.counter < fields.length - 1
                && fields[this.counter].compareTo(NODE_TAG) != 0
                && fields[this.counter].compareTo(PIPELINE_TAG) != 0) {
            if (fields[this.counter].compareTo("") != 0
                    && fields[this.counter].compareTo(" ") != 0
                    && !toIgnore.contains(originalHeader.get(valueIndex))) {
                double value = this.numberFormat.parse(fields[this.counter])
                        .doubleValue();
                measures.add(value);
            } else {
                if(!toIgnore.contains(originalHeader.get(valueIndex)))
                    measures.add(-1.0);
            }
            valueIndex++;
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
    
    public HashMap<String, ArrayList<String>> getOriginalHeader(){
        return this.originalHeader;
    }
    
    public HashMap<String, ArrayList<String>> getFilteredHeader(){
        return this.filteredHeader;
    }
}
