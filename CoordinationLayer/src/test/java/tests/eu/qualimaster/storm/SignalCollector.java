package tests.eu.qualimaster.storm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.AlgorithmChangeSignal;
import eu.qualimaster.common.signal.ParameterChangeSignal;
import eu.qualimaster.common.signal.ShutdownSignal;

/**
 * Implements a generic signal collector, i.e., a class which collects only the latest decoded signal payloads. This 
 * class is just intended for testing.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class SignalCollector implements Serializable {

    public static final String NAME_SHUTDOWN = "shutdown";
    private static final Logger LOGGER = Logger.getLogger(SignalCollector.class);
    private File log;
    
    /**
     * Creates a signal collector with a specific file to log to.
     * Please note that this is logging for testing, rather than general logging
     * via a logging framework. Logging information will be appended.
     * 
     * @param log the log file
     */
    public SignalCollector(File log) {
        this.log = log;
    }

    /**
     * Creates and logs an entry.
     * 
     * @param algorithm the algorithm name (optional)
     * @param parameterName the parameter name (optional)
     * @param parameterValue the parameter value (optional)
     * @return the interpreted payload (may be <b>null</b>)
     */
    private synchronized SignalEntry log(String algorithm, String parameterName, String parameterValue) {
        SignalEntry result = null;
        if (null != log && (null != algorithm || null != parameterName || null != parameterValue)) {
            try {
                PrintStream out = new PrintStream(new FileOutputStream(log, true)); // append, clean via Naming in test
                out.println("Entry:");
                out.flush();
                out.println(quote(algorithm));
                out.flush();
                out.println(quote(parameterName));
                out.flush();
                out.println(quote(parameterValue));
                out.flush();
                out.close();
                LOGGER.info("Logged entry");
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            result = new SignalEntry(algorithm, parameterName, parameterValue);
        }
        return result;
    }

    /**
     * Called on an algorithm change.
     * 
     * @param signal the algorithm change signal
     * @return the interpreted payload (may be <b>null</b>)
     */
    public SignalEntry notifyAlgorithmChange(AlgorithmChangeSignal signal) {
        return log(signal.getAlgorithm(), null, null);
    }

    /**
     * Called on an parameter change.
     * 
     * @param signal the parameter change signal
     * @return the interpreted payload (may be <b>null</b>)
     */
    public SignalEntry notifyParameterChange(ParameterChangeSignal signal) {
        return log(null, signal.getChange(0).getName(), String.valueOf(signal.getChange(0).getValue()));
    }

    /**
     * Called on a shutdown signal.
     * 
     * @param signal the shutdown signal
     * @return the interpreted payload (may be <b>null</b>)
     */
    public SignalEntry notifyShutdown(ShutdownSignal signal) {
        return log(NAME_SHUTDOWN, null, null);
    }
    
    /**
     * Quotes the given string.
     * 
     * @param text the string to be quoted
     * @return the quoted string
     */
    private static String quote(String text) {
        String result;
        if (null == text) {
            result = text;
        } else {
            result = "\"" + text + "\"";
        }
        return result;
    }
    
    /**
     * Unquotes a read string.
     * 
     * @param raw the string to be unquoted
     * @return the unquoted string
     */
    private static String unquote(String raw) {
        String result;
        if (null == raw) {
            result = null;
        } else {
            if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
                result = raw.substring(1, raw.length() - 1);
            } else {
                if (raw.equals("null")) {
                    result = null;
                } else {
                    result = raw;
                }
            }
        }
        return result;
    }
    
    /**
     * Reads the logged entries from a file. Warning: This method is rather simple
     * and not very tolerant :o
     * 
     * @param file the file to read
     * @return the read entries (<b>null</b> in case of I/O problems)
     */
    public static final List<SignalEntry> read(File file) {
        List<SignalEntry> result = new ArrayList<SignalEntry>();
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new FileReader(file));
            String line;
            do {
                line = reader.readLine();
                if ("Entry:".equals(line)) {
                    String algorithm = unquote(reader.readLine());
                    String parameterName = unquote(reader.readLine());
                    String parameterValue = unquote(reader.readLine());
                    result.add(new SignalEntry(algorithm, parameterName, parameterValue));
                }
            } while (null != line);
            reader.close();
        } catch (IOException e) {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return result;
    }
    
    /**
     * Captures a signal in terms of the conveyed information.
     * 
     * @author Holger Eichelberger
     */
    public static class SignalEntry implements Serializable {
        private String algorithm;
        private String parameterName;
        private String parameterValue;

        /**
         * Creates a signal entry.
         * 
         * @param algorithm the algorithm (may be <b>null</b>)
         * @param parameterName the parameter name (may be <b>null</b>)
         * @param parameterValue the parameter value (may be <b>null</b>)
         */
        private SignalEntry(String algorithm, String parameterName, String parameterValue) {
            this.algorithm = algorithm;
            this.parameterName = parameterName;
            this.parameterValue = parameterValue;
        }
        
        /**
         * Returns the received algorithm name.
         * 
         * @return the name of the algorithm (may be <b>null</b> if none was received)
         */
        public String getAlgorithm() {
            return algorithm;
        }

        /**
         * Returns the received parameter name.
         * 
         * @return the name of the parameter (may be <b>null</b> if none was received)
         */
        public String getParameterName() {
            return parameterName;
        }

        /**
         * Returns the received parameter value.
         * 
         * @return the value of the parameter (may be <b>null</b> if none was received)
         */
        public String getParameterValue() {
            return parameterValue;
        }

        @Override
        public String toString() {
            return "alg: " + algorithm + " param: " + parameterName + " value: " + parameterValue; 
        }

    }
    
}
