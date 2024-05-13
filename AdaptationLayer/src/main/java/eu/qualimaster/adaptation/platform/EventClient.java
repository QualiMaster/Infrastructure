package eu.qualimaster.adaptation.platform;

import java.io.IOException;
import java.net.InetAddress;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.external.AlgorithmChangedMessage;
import eu.qualimaster.adaptation.external.ChangeParameterRequest;
import eu.qualimaster.adaptation.external.ClientEndpoint;
import eu.qualimaster.adaptation.external.DispatcherAdapter;
import eu.qualimaster.adaptation.external.HardwareAliveMessage;
import eu.qualimaster.adaptation.external.LoggingMessage;
import eu.qualimaster.adaptation.external.MonitoringDataMessage;
import eu.qualimaster.adaptation.external.ReplayMessage;

/**
 * A simple event client for debugging.
 * 
 * @author Holger Eichelberger
 */
public class EventClient extends DispatcherAdapter {

    /**
     * Listens to events.
     * 
     * @param args ignored
     * @throws IOException in cases of problems
     */
    public static void main(String[] args) throws IOException {
        Cli.configure();
        InetAddress addr = InetAddress.getByName(AdaptationConfiguration.getEventHost());
        final ClientEndpoint endpoint = new ClientEndpoint(new EventClient(), addr, 
            AdaptationConfiguration.getAdaptationPort());
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                if (null != endpoint) {
                    endpoint.stop();
                }
            }
        }));
    }

    @Override
    public void handleMonitoringDataMessage(MonitoringDataMessage msg) {
        System.out.println("Monitoring: " + msg.getPart() + " " + msg.getObservations());
    }

    @Override
    public void handleAlgorithmChangedMessage(AlgorithmChangedMessage msg) {
        System.out.println("Alg changed: " + msg.getPipeline() + "/" + msg.getPipelineElement() + " to " 
            + msg.getAlgorithm());
    }

    @Override
    public void handleChangeParameterRequest(ChangeParameterRequest<?> msg) {
        System.out.println("Param changed: " + msg.getPipeline() + "/" + msg.getPipelineElement() + "/" 
            + msg.getPipelineElement() + " value " + msg.getValue());
    }
    
    @Override
    public void handleReplayMessage(ReplayMessage msg) {
        System.out.print("replay message " + msg.getPipeline() + " #" + msg.getTicket());
        if (msg.getStartReplay()) {
            System.out.println("start " + msg.getStart() + " -> " + msg.getEnd() + " @" + msg.getSpeed() 
                + " " + msg.getQuery());
        } else {
            System.out.println("stop");
        }
    }

    @Override
    public void handleHardwareAliveMessage(HardwareAliveMessage msg) {
        System.out.println("Hardware alive: " + msg.getIdentifier());
    }

    @Override
    public void handleLoggingMessage(LoggingMessage msg) {
        System.out.println("Logging message: " + msg.getMessage());
    }

}
