package eu.qualimaster.adaptation.reflective;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import eu.qualimaster.adaptation.events.ReflectiveAdaptationRegistrationEvent;
import eu.qualimaster.adaptation.events.ReflectiveAdaptationRequest;
import eu.qualimaster.adaptation.events.ReflectiveAdaptationResponse;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.utils.IScheduler;


/**
 * Entrance point for the reflective adaptation, it handles the events and provides
 * the methods to use the functionalities of the reflective adaptation. It contains
 * a setup-model map to efficiently resolve which model to use based on the setup 
 * (i.e. which pipeline(s), nodes, observables should be included in the adaptation).
 * 
 * @author Andrea Ceroni
 */
public class ReflectiveAdaptationManager {
    
    private static final ReflectiveAdaptationRegistrationEventHandler REFLECTIVE_ADAPTATION_REGISTRATION_EVENT_HANDLER = new ReflectiveAdaptationRegistrationEventHandler();
    private static final ReflectiveAdaptationRequestHandler REFLECTIVE_ADAPTATION_REQUEST_HANDLER = new ReflectiveAdaptationRequestHandler();

    /**
     * Set of available models, one for each different setup.
     */
    private static HashMap<Setup,ReflectiveAdaptation> models = new HashMap<>();

    /**
     * Indicates whether the adaptation is running in "test" mode or not.
     */
    private static boolean test = false;

    /** Indicates the status of the component */
    private static String status = "idle";

    /**
     * Initializes the volume reflective adaptation by loading all the pre-
     * trained models available. All the information describing the supported
     * setups as well as the paths of the models is contained in the input
     * xml file.
     * 
     * @param setupsFile the xml file containing the description of the setups the pre-
     * trained model refer to.
     * @param keepPreviousModels flag to keep previously initialized models.
     */
    public static void initialize(String setupsFile, boolean keepPreviousModels) {
        
        System.out.println("Initializing reflective models from setups in: " + setupsFile);
        
        if(keepPreviousModels)
            System.out.println("Keeping previously initialized models.");
        else{
            System.out.println("Deleting previously initialized models.");
            models.clear();
        }
        
        ArrayList<Setup> setups = readSetups(setupsFile);
        for(Setup s : setups){
            ReflectiveAdaptation model = new ReflectiveAdaptation(s);
            models.put(s, model);
        }
        System.out.println(models.size() + " model(s) initialized");
    }
    
    private static ArrayList<Setup> readSetups(String filePath){
        ArrayList<Setup> setups = new ArrayList<>();
        
        try{
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();
            
            NodeList setupsList = doc.getElementsByTagName("setup");
            for (int temp = 0; temp < setupsList.getLength(); temp++)
            {
                Node nNode = setupsList.item(temp);
                                
                if (nNode.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element eElement = (Element) nNode;

                    NodeList tmpList = eElement.getElementsByTagName("model_path");
                    String modelPath = tmpList.item(0).getTextContent();
                    
                    tmpList = eElement.getElementsByTagName("history_size");
                    int historySize = Integer.valueOf(tmpList.item(0).getTextContent());
                    
                    tmpList = eElement.getElementsByTagName("platform_observables");
                    String platformObservablesStr = tmpList.item(0).getTextContent();
                    ArrayList<String> platformObserbables = new ArrayList<>();
                    for(String s : platformObservablesStr.split(","))
                        platformObserbables.add(s);
                    
                    tmpList = eElement.getElementsByTagName("pipelines_observables");
                    String pipelinesObservablesStr = tmpList.item(0).getTextContent();
                    ArrayList<String> pipelinesObserbables = new ArrayList<>();
                    for(String s : pipelinesObservablesStr.split(","))
                        pipelinesObserbables.add(s);
                    
                    tmpList = eElement.getElementsByTagName("nodes_observables");
                    String nodesObservablesStr = tmpList.item(0).getTextContent();
                    ArrayList<String> nodesObserbables = new ArrayList<>();
                    for(String s : nodesObservablesStr.split(","))
                        nodesObserbables.add(s);
                    
                    HashMap<String, ArrayList<String>> observables = new HashMap<>();
                    observables.put("platform", platformObserbables);
                    observables.put("pipelines", pipelinesObserbables);
                    observables.put("nodes", nodesObserbables);
                
                    ArrayList<String> pipelines = new ArrayList<>();
                    HashMap<String, ArrayList<String>> nodesForPipelines = new HashMap<>();
                    NodeList pipelinesList = eElement.getElementsByTagName("pipeline");
                    for (int temp2 = 0; temp2 < pipelinesList.getLength(); temp2++)
                    {
                        nNode = pipelinesList.item(temp2);
                        String name = nNode.getAttributes().getNamedItem("name").getNodeValue();
                        pipelines.add(name);
                                        
                        if (nNode.getNodeType() == Node.ELEMENT_NODE)
                        {
                            eElement = (Element) nNode;
                            
                            String[] fields = eElement.getElementsByTagName("nodes").item(0).getTextContent().split(",");
                            ArrayList<String> nodes = new ArrayList<>();
                            for(int i = 0; i < fields.length; i++)
                            {
                                nodes.add(fields[i]);
                            }
                            nodesForPipelines.put(name, nodes);
                        }
                    }
                    
                    Setup setup = new Setup();
                    setup.setHistorySize(historySize);
                    setup.setModelPath(modelPath);
                    setup.setObservables(observables);
                    setup.setPipelines(pipelines);
                    setup.setNodes(nodesForPipelines);
                    
                    setups.add(setup);
                }
            }
            
            return setups;
 
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return setups;
        }
    }
    
    /**
     * A handler for initializing reflective adaptation models. If called more than once,
     * there is the possibility of either keeping or deleting the models already available.
     * 
     * @author Andrea Ceroni
     */
    private static class ReflectiveAdaptationRegistrationEventHandler extends
            EventHandler<ReflectiveAdaptationRegistrationEvent> {

        /**
         * Creates an instance.
         */
        protected ReflectiveAdaptationRegistrationEventHandler() {
            super(ReflectiveAdaptationRegistrationEvent.class);
        }

        @Override
        protected void handle(ReflectiveAdaptationRegistrationEvent event) {
            System.out
                    .println("ReflectiveAdaptationRegistrationEvent received...");
            status = "initializing";
            initialize(event.getSetupsPath(), event.isKeepAvailableModels());
            status = "ready";
        }
    }

    /**
     * A handler for reflective adaptation requests. Leads to a
     * {@link ReflectiveAdaptationResponse}.
     * 
     * @author Andrea Ceroni
     */
    private static class ReflectiveAdaptationRequestHandler extends
            EventHandler<ReflectiveAdaptationRequest> {

        /**
         * Creates an instance.
         */
        protected ReflectiveAdaptationRequestHandler() {
            super(ReflectiveAdaptationRequest.class);
        }

        @Override
        protected void handle(ReflectiveAdaptationRequest event) {
            ReflectiveAdaptation involvedModel = models.get(event.getSetup());
            if(involvedModel == null){
                System.out.println("ERROR: there is no reflective model for the required setup");
                return;
            }
            double prediction = involvedModel.predict(event.getHeaders(), event.getLatestMonitoring());

            EventManager.send(new ReflectiveAdaptationResponse(event, prediction));
        }

    }

    /**
     * Called upon startup of the infrastructure.
     * 
     * @param scheduler a scheduler instance for regular tasks
     */
    public static void start(IScheduler scheduler) {
        EventManager.register(REFLECTIVE_ADAPTATION_REGISTRATION_EVENT_HANDLER);
        EventManager.register(REFLECTIVE_ADAPTATION_REQUEST_HANDLER);
    }

    /**
     * Notifies the adaptation about changes in the lifecycle of pipelines.
     * 
     * @param event the lifecycle event
     */
    public static void notifyPipelineLifecycleChange(
            PipelineLifecycleEvent event) {
    }

    /**
     * Called upon shutdown of the infrastructure. Clean up global resources
     * here.
     */
    public static void stop() {
        EventManager.unregister(REFLECTIVE_ADAPTATION_REGISTRATION_EVENT_HANDLER);
        EventManager.unregister(REFLECTIVE_ADAPTATION_REQUEST_HANDLER);
    }

    /**
     * @return the test
     */
    public static boolean isTest() {
        return test;
    }

    /**
     * @param test the test to set
     */
    public static void setTest(boolean t) {
        test = t;
    }

    /**
     * Gets the reflective model corresponding to a given setup
     * 
     * @param setup the setup
     * @return
     */
    public static ReflectiveAdaptation getModel(Setup setup) {
        return models.get(setup);
    }

    /**
     * @return the status
     */
    public static String getStatus() {
        return status;
    }
    
    public static void main(String[] args){
        readSetups("./testdata/reflective/models/setups.xml");
    }
}
