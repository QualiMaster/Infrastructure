package eu.qualimaster.adaptation.platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.ICoordinationCommandVisitor;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ShutdownCommand;
import eu.qualimaster.coordination.commands.UpdateCommand;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.ChangeMonitoringEvent;
import eu.qualimaster.pipeline.AlgorithmChangeParameter;

/**
 * A simple command line interface to the QualiMaster infrastructure. A configuration
 * can be given in the <code>CFG_FILE</code> file either in the local directory or in the 
 * user's home directory. For configuration options see {@link AdaptationConfiguration}. Basically,
 * the CLI aims at sending the commands via the event bus. However, if the Cli shall be used
 * as a standalone tool just working against Storm/Zookeeper, then set "cli.standalone" to
 * true in the configuration. 
 * 
 * @author Holger Eichelberger
 */
public class Cli {
    
    public static final String CLI_STANDALONE = "cli.standalone";  
    public static final String CFG_FILE_OLD = "qm.cfg"; // legacy
    public static final String CFG_FILE = "qm.cli.cfg";
    private static boolean standalone = false;

    /**
     * Executes the command line interface.
     * 
     * @param args command line arguments (see implementation)
     */
    public static void main(String[] args) {
        configure();
        if (0 == args.length) {
            printHelp();
        } else {
            String cmd = args[0];
            ParseResult result = new ParseResult();
            
            switch (cmd) {
            case "start":
                result.parseStart(args);
                break;
            case "stop":
                result.parseStop(args);
                break;
            case "setParam":
                result.parseSetParam(args);
                break;
            case "changeAlgo":
                result.parseChangeAlgorithm(args);
                break;
            case "rebalance":
                result.parseRebalance(args);
                break;
            case "replay":
                result.parseReplay(args);
                break;
            case "shed":
                result.parseShed(args);
                break;
            case "tracing":
                result.parseTracing(args);
                break;
            case "profile":
                result.parseProfile(args);
                break;
            case "update":
                result.parseUpdate(args);
                break;
            case "shutdown":
                result.parseShutdown(args);
                break;
            default:
                result.unknownCmd(cmd);
                break;
            }
            result.validate();
            String error = result.getError();
            if (null != error) {
                System.out.println("Error: " + error);
            } else {
                CoordinationCommand cCmd = result.getCommand();
                if (!standalone) {
                    cCmd.execute(); // implicitly starts local or client event manager
                    EventManager.cleanup();
                    EventManager.stop();
                } else {
                    // just use coordination manager as a library
                    CoordinationManager.execute(cCmd);
                    CoordinationManager.stop();
                }
            }
        }
    }
    
    /**
     * Implements a pseudo monitoring change command in order to utilize the existing infrastructure.
     * Creates a {@link ChangeMonitoringEvent} and sends that instead of this instance as usual. Implements 
     * {@link #accept(ICoordinationCommandVisitor)} empty, i.e., shall not be used with a command visitor.
     * 
     * @author Holger Eichelberger
     */
    private static class ChangeMonitoringCommand extends CoordinationCommand {
        
        private static final long serialVersionUID = 3615872616663719327L;
        private boolean enableAlgorithmTracing;

        /**
         * Creates a monitoring change command.
         * 
         * @param enableAlgorithmTracing whether tracing shall be enabled
         */
        private ChangeMonitoringCommand(boolean enableAlgorithmTracing) {
            this.enableAlgorithmTracing = enableAlgorithmTracing;
        }
        
        @Override
        public void execute() {
            // TODO tracing?
            EventManager.send(new ChangeMonitoringEvent(enableAlgorithmTracing));
        }

        @Override
        public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
            return null; // this is a pseudi command
        }
        
    }
    
    /**
     * Implements a parse result, an error message or a coordination command.
     * 
     * @author Holger Eichelberger
     */
    private static class ParseResult {
        private String error;
        private CoordinationCommand cmd;
        
        /**
         * Creates a parse result instance.
         */
        private ParseResult() {
        }

        /**
         * Parses start pipeline command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseStart(String[] args) {
            if (2 == args.length) {
                cmd = new PipelineCommand(args[1], PipelineCommand.Status.START);
            } 
        }

        /**
         * Parses a stop pipeline command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseStop(String[] args) {
            if (2 == args.length) {
                cmd = new PipelineCommand(args[1], PipelineCommand.Status.STOP);
            } 
        }

        /**
         * Parses a set parameter command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseSetParam(String[] args) {
            if (5 == args.length) {
                cmd = new ParameterChangeCommand<String>(args[1], args[2], args[3], args[4]);
            } 
        }

        /**
         * Composes an error string.
         * 
         * @param err the original string
         * @param text the string to be added
         * @return the composed string
         */
        private String addErrorString(String err, String text) {
            if (err.length() > 0) {
                err += ", ";
            }
            err += text;
            return err;
        }

        /**
         * Parses an algorithm change command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseChangeAlgorithm(String[] args) {
            if (args.length >= 4) {
                String errTmp = "";
                AlgorithmChangeCommand aCmd = new AlgorithmChangeCommand(args[1], args[2], args[3]);
                Map<String, Serializable> params = new HashMap<String, Serializable>();
                for (int p = 4; p < args.length - 1; p++) {
                    if (p + 1 < args.length - 1) {
                        AlgorithmChangeParameter par = AlgorithmChangeParameter.valueOfSafe(args[p]);
                        if (par != null) {
                            try {
                                par.setParameterValue(params, args[p + 1]);
                            } catch (IllegalArgumentException e) {
                                errTmp = addErrorString(errTmp, "Parameter value for " + args[p] 
                                    + " " + e.getMessage());
                            }
                        } else {
                            errTmp = addErrorString(errTmp, "Parameter unknown " + args[p]);
                        }
                    } else {
                        errTmp = addErrorString(errTmp, "No value for parameter " + args[p]);
                    }
                }
                if (errTmp.length() > 0) {
                    error = errTmp;
                }
                aCmd.setParameters(params);
                cmd = aCmd;
            } 
        }

        /**
         * Parses the shed command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseShed(String[] args) {
            if (args.length >= 4) {
                LoadSheddingCommand lCmd = new LoadSheddingCommand(args[1], args[2], args[3]);
                String errTmp = "";
                int i = 4;
                while (i + 1 < args.length) {
                    String paramName = args[i++];
                    String paramValue = args[i++];
                    lCmd.setParameter(paramName, paramValue);
                }
                if (i < args.length) {
                    addErrorString(errTmp, "Unbalanced parameter name/value");
                }
                if (errTmp.length() > 0) {
                    error = errTmp;
                }
                cmd = lCmd;
            }
        }

        /**
         * Parses the replay command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseReplay(String[] args) {
            if (args.length >= 5) {
                String errTmp = "";
                try {
                    int ticket = parseInt(args[3], "ticket");
                    boolean start = Boolean.valueOf(args[4]);
                    ReplayCommand rCmd = new ReplayCommand(args[1], args[2], start, ticket);
                    if (start) {
                        if (args.length >= 9) {
                            Date from = parseDate(args[5], "from");
                            Date to = parseDate(args[6], "to");
                            int speed = parseInt(args[7], "speed");
                            String query = "";
                            for (int i = 8; i < args.length; i++) {
                                if (query.length() > 0) {
                                    query += " ";
                                }
                                query = query + args[i];
                            }
                            rCmd.setReplayStartInfo(from, to, speed, query);
                        } else {
                            addErrorString(errTmp, "Missing arguments");
                        }
                    }
                    cmd = rCmd;
                } catch (NumberFormatException e) {
                    addErrorString(errTmp, e.getMessage());
                }
                if (errTmp.length() > 0) {
                    error = errTmp;
                }
            }
        }

        /**
         * Parses an int value throwing an exception containing <code>argName</code> in case of
         * failures.
         * 
         * @param text the text to parse
         * @param argName the argument name
         * @return the value
         * @throws NumberFormatException in case that parsing is not possible
         */
        private int parseInt(String text, String argName) throws NumberFormatException {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Parsing " + argName + ": " + e.getMessage());
            }
        }

        /**
         * Parses a date value throwing an exception containing <code>argName</code> in case of
         * failures.
         * 
         * @param text the text to parse
         * @param argName the argument name
         * @return the value
         * @throws NumberFormatException in case that parsing is not possible
         */
        private Date parseDate(String text, String argName) throws NumberFormatException {
            Date result;
            if ("null".equals(text)) {
                result = null;
            } else {
                try {
                    result = new Date(Long.parseLong(text));
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("Parsing " + argName + ": " + e.getMessage());
                }
            }
            return result;
        }
        
        /**
         * Parses a rebalance command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseRebalance(String[] args) {
            if (args.length >= 3) {
                int i = 1;
                String pipeline = args[i++];
                try {
                    int numberOfWorkers = Integer.parseInt(args[i++]);
                    Map<String, Integer> executors;
                    if (i + 1 < args.length) { // pair-wise
                        executors = new HashMap<String, Integer>();
                        while (i + 1 < args.length) { // pair-wise
                            executors.put(args[i++], Integer.parseInt(args[i++]));
                        }
                    } else {
                        executors = null;
                    }
                    cmd = new ParallelismChangeCommand(pipeline, numberOfWorkers, executors);
                } catch (NumberFormatException e) {
                    error = "Parameter " + i + " is not a number";
                }
            }
        }
        
        /**
         * Parses the tracing command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseTracing(String[] args) {
            if (args.length >= 2) {
                String tmp = args[1].toUpperCase().trim();
                Boolean enable = null;
                if ("TRUE".equals(tmp)) {
                    enable = true;
                } else if ("FALSE".equals(tmp)) {
                    enable = false;
                } else {
                    error = "Parameter " + 1 + "is not a Boolean value";
                }
                if (null != enable) {
                    cmd = new ChangeMonitoringCommand(enable);
                }
            }
        }

        /**
         * Parses the profile command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseProfile(String[] args) {
            if (args.length >= 3) {
                String family = args[1];
                String algorithm = args[2];
                cmd = new ProfileAlgorithmCommand(family, algorithm);
            }
        }

        /**
         * Parses the update command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseUpdate(String[] args) {
            cmd = new UpdateCommand();
        }

        /**
         * Parses the shutdown command.
         * 
         * @param args the command line arguments (index 0 = command)
         */
        private void parseShutdown(String[] args) {
            cmd = new ShutdownCommand();
        }

        /**
         * Changes the error message to indicate an unknown command.
         *  
         * @param cmd the unknown command
         */
        private void unknownCmd(String cmd) {
            error = "Unknown command: " + cmd;
        }
        
        /**
         * Validates the parse result and potentially changes the error message.
         */
        private void validate() {
            if (null == cmd && null == error) {
                error = "Wrong parameter number!";
            }
        }
        
        /**
         * Returns the resulting command to be executed.
         * 
         * @return the command (may be <b>null</b> in case of parse errors)
         */
        private CoordinationCommand getCommand() {
            return cmd;
        }
        
        /**
         * Returns a detected error (message).
         * 
         * @return the error or <b>null</b> if none was detected
         */
        private String getError() {
            return error;
        }
        
    }

    /**
     * Prints the command line help.
     */
    private static void printHelp() {
        System.out.println("QM infrastructure commandline (" 
            + (standalone ? "standalone mode" : "event client mode") + ")");
        System.out.println("using:");
        System.out.println(" - zookeeper: " + AdaptationConfiguration.getZookeeper() + " @ " 
            + AdaptationConfiguration.getZookeeperPort());
        System.out.println(" - nimbus: " + AdaptationConfiguration.getNimbus() + " @ " 
            + AdaptationConfiguration.getThriftPort());
        System.out.println(" - eventServer: " + AdaptationConfiguration.getEventHost() 
            + " @ " + AdaptationConfiguration.getEventPort());
        System.out.println(" - monitoring log location: " + AdaptationConfiguration.getMonitoringLogLocation());
        System.out.println("commands:");
        System.out.println(" - start <pipeline>");
        System.out.println("   starts the given pipeline");
        System.out.println(" - stop <pipeline>");
        System.out.println("   stops the given pipeline");
        System.out.println(" - changeAlgo <pipeline> <pipelineElement> <algorithm> (param value)*");
        System.out.println("   changes an algorithm at runtime within the given pipeline");
        System.out.println(" - setParam <pipeline> <pipelineElement> <param> <value>");
        String tmp = "";
        for (AlgorithmChangeParameter param : AlgorithmChangeParameter.values()) {
            if (tmp.length() > 0) {
                tmp += ", ";
            }
            tmp += param.name() + " as " + param.getType().getName();
        }
        System.out.println("   param: " + tmp);
        System.out.println(" - rebalance <pipeline> <#workers> (<pipelineElement> <tasks>)*");
        System.out.println("   performs a Storm rebalance operation");
        System.out.println(" - replay <pipeline> <sink> <ticket> <boolean> (<from> <to> <speed> <query>)?");
        System.out.println("   starts/stops replaying data (boolean); if start, the further parameters shall be given");
        System.out.println("   from/to as data longs, speed as int, query as string");
        System.out.println(" - shed <pipeline> <pipelineElement> <shedder> (<paramName> <value>)*");
        System.out.println("   starts load shedding on the given element using the given shedder (class name or id)");
        System.out.println("   with shedder specific parameters");
        System.out.println(" - tracing <boolean>");
        System.out.println("   enables/disables algorithm tracing for new pipelines, requires " 
            + AdaptationConfiguration.MONITORING_LOG_LOCATION);
        System.out.println(" - profile <family> <algorithm>");
        System.out.println("   runs, monitors and profiles the given algorithm");
        System.out.println(" - update");
        System.out.println("   updates the infrastructure model (experimental)");
        System.out.println(" - shutdown");
        System.out.println("   shuts down the infrastructure (may consider Storm depending on the settings)");
    }
    
    /**
     * Reads a given configuration file, either from the current directory or the user's home directory.
     * 
     * @param name the file name
     * @param prop the (incrementally aggregated) properties
     */
    private static final void readCfg(String name, Properties prop) {
        File f = new File(name);
        if (!f.exists()) {
            f = new File(System.getProperty("user.home"), name);
        }
        if (f.exists()) {
            // also obtain properties of the CLI
            try (FileInputStream in = new FileInputStream(f)) {
                prop.load(in);
                in.close();
            } catch (IOException e) {
                System.out.println("While reading " + f.getAbsolutePath() + ":" + e.getMessage());
            }            
        }                
    }
    
    /**
     * Tries configuring from a local file in the same directory.
     */
    public static final void configure() {
        AdaptationConfiguration.configureLocal();
        Properties prop = new Properties();
        readCfg(Main.CFG_FILE, prop);
        readCfg(CFG_FILE, prop);
        readCfg(CFG_FILE_OLD, prop); // legacy
        AdaptationConfiguration.configure(prop);
        standalone = "true".equals(prop.get(CLI_STANDALONE));
    }

}