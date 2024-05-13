package eu.qualimaster.adaptation.platform;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.external.ClientEndpoint;
import eu.qualimaster.adaptation.external.DispatcherAdapter;
import eu.qualimaster.adaptation.external.LoggingFilterRequest;
import eu.qualimaster.adaptation.external.LoggingMessage;

/**
 * A simple event client for QM specific logging.
 * 
 * @author Holger Eichelberger
 */
public class LogClient extends DispatcherAdapter {

    private static List<String> filterRegEx = new ArrayList<String>();
    
    /**
     * Listens to events.
     * 
     * @param args the log filtering regular expressions
     * @throws IOException in cases of problems
     */
    public static void main(String[] args) throws IOException {
        if (0 == args.length) {
            System.out.println("At least on logging filtering regular expression must be given. Terminating.");
            System.exit(0);
        }
            
        for (String s : args) {
            try {
                Pattern.compile(s);
                filterRegEx.add(s);
            } catch (PatternSyntaxException e) {
                System.out.println("Filter regEx syntax: " + e.getMessage());
            }
        }
        if (filterRegEx.isEmpty()) {
            System.out.println("No valid filter regEx given. Terminating.");
            System.exit(0);
        }
        Cli.configure();
        InetAddress addr = InetAddress.getByName(AdaptationConfiguration.getEventHost());
        final ClientEndpoint endpoint = new ClientEndpoint(new LogClient(), addr, 
            AdaptationConfiguration.getAdaptationPort());
        endpoint.schedule(new LoggingFilterRequest(filterRegEx, null));
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                if (null != endpoint) {
                    endpoint.schedule(new LoggingFilterRequest(null, filterRegEx));
                    while (endpoint.hasWork()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                        }
                    }
                    endpoint.stop();
                }
            }
        }));
    }

    @Override
    public void handleLoggingMessage(LoggingMessage msg) {
        System.out.println(msg.getLevel() + "[" + msg.getThreadName() + "]" + msg.getMessage());
    }

}
