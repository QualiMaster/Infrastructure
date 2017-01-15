/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.monitoring.tracing;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictionManager;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Observables;

/**
 * CSV log reader in Monitoring Layer format with format information in the first two lines.
 * 
 * @author Holger Eichelberger
 */
public class TraceReader {

    private List<IObservable> pipelineObservables = new ArrayList<IObservable>();
    private List<IObservable> nodeObservables = new ArrayList<IObservable>();
    
    /**
     * Creates a reader instance.
     */
    public TraceReader() {
    }
    
    /**
     * Reads a CSV file.
     * 
     * @param file the file to read
     * @return the pipeline entries
     * @throws IOException in case of I/O problems
     */
    public List<PipelineEntry> readCsv(File file) throws IOException {
        List<PipelineEntry> result = new ArrayList<PipelineEntry>();
        LineNumberReader lnr = new LineNumberReader(new FileReader(file));
        String line;
        do {
            line = lnr.readLine();
            if (line != null) {
                line = line.trim();
                if (1 == lnr.getLineNumber()) {
                    parseObservables(line);
                } else if (2 == lnr.getLineNumber()) {
                    parseNodeFormat(line);
                } else if (line.length() > 0) {
                    PipelineEntry pipeline = parseLine(line, lnr.getLineNumber());
                    if (null != pipeline) {
                        result.add(pipeline);
                    }
                }
            }
        } while (null != line);
        lnr.close();
        return result;
    }
    
    /**
     * Clears the reader for reuse.
     */
    public void clear() {
        pipelineObservables.clear();
        nodeObservables.clear();
    }

    /**
     * Parses the observables format description.
     * 
     * @param line the line
     * @throws IOException in case of I/O problems
     */
    private void parseObservables(String line) throws IOException {
        parseObservables(consume(line, "pipeline format:"), pipelineObservables);
    }
    
    /**
     * Parses the node format description.
     * 
     * @param line the line
     * @throws IOException in case of I/O problems
     */
    private void parseNodeFormat(String line) throws IOException {
        parseObservables(consume(line, "pipeline node format:"), nodeObservables);
    }

    /**
     * The line parts as parsing states.
     * 
     * @author Holger Eichelberger
     */
    private enum Part {
        LEAD_IN,
        PIPELINE,
        NODE,
        LEAD_OUT;
    }
    
    /**
     * Parses a single content line.
     * 
     * @param line the line to read
     * @param lineNr the number of the line in the file
     * @return the parsed pipeline entry, may be <b>null</b>
     * @throws IOException in case of I/O problems
     */
    private PipelineEntry parseLine(String line, int lineNr) throws IOException {
        int last = 0;
        int here = 0;
        int pos = 0;
        int entryPos = 0;
        PipelineEntry pipeline = null;
        Entry entry = null;
        long timestamp = -1;
        
        Part part = Part.LEAD_IN; 
        while (here < line.length()) {
            here = line.indexOf('\t', last);
            if (here < 0) {
                here = line.length();
            }
            String token = line.substring(last, here);
            if (0 == pos) {
                try {
                    timestamp = Long.parseLong(token);
                } catch (NumberFormatException e) {
                    System.out.println("[Error] Timestamp in line nr " + lineNr + ": " + e.getMessage());
                    break;
                }
                // timestamp
            } else if (1 == pos) {
                if (!"pipeline:".equals(token)) {
                    System.out.println("[Error] Missing leading pipeline tag in line nr " + lineNr);
                    break;
                }
                part = Part.PIPELINE;
                entryPos = 0;
            } else if (!"pipeline/".equals(token)) {
                if (0 == entryPos) {
                    if (Part.PIPELINE == part) {
                        pipeline = new PipelineEntry(timestamp, token);
                    } else {
                        entry = new Entry(token);
                        pipeline.addNodeEntry(entry);
                    }
                } else {
                    boolean end = false;
                    try {
                        if (Part.PIPELINE == part) {
                            end = readObservation(pipeline, token, pipelineObservables, entryPos);
                        } else if (Part.NODE == part) {
                            end = readObservation(entry, token, nodeObservables, entryPos);
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                        break;
                    }
                    if (end) {
                        if (Part.PIPELINE == part) {
                            part = Part.NODE;
                        }
                        entryPos = -1; // reset and ++ below
                    }
                }
                entryPos++;
            } else {
                part = Part.LEAD_OUT; 
            }
            pos++;
            last = here + 1;
        }
        return pipeline;
    }
    
    /**
     * A log entry as part of a {@link PipelineEntry}.
     * 
     * @author Holger Eichelberger
     */
    public static class Entry {
        
        private String name;
        private Map<IObservable, Double> observations = new HashMap<IObservable, Double>();
        
        /**
         * Creates an entry.
         * 
         * @param name the name of the pipeline
         */
        private Entry(String name) {
            this.name = name;
        }
        
        /**
         * Adds an observation.
         * 
         * @param observable the observable
         * @param value the value of the observation
         */
        private void addObservation(IObservable observable, Double value) {
            if (null != observable) {
                observations.put(observable, value);
            }
        }

        /**
         * Returns the name of the entry.
         * 
         * @return the name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Returns the value of an observation.
         * 
         * @param observable the observable to return the observation for
         * @return the value, <b>null</b> if no such value vas recorded
         */
        public Double getObservation(IObservable observable) {
            Double result = null;
            if (null != observable) {
                result = observations.get(observable);
            }
            return result;
        }
        
        /**
         * Returns all recorded observables.
         * 
         * @return the observables
         */
        public Collection<IObservable> observables() {
            return observations.keySet();
        }
        
        @Override
        public String toString() {
            return name + " observations " + observations;
        }
        
    }
    
    /**
     * Represents a log entry for a pipeline.
     * 
     * @author Holger Eichelberger
     */
    public static class PipelineEntry extends Entry {
        
        private long timestamp;
        private Map<String, Entry> nodeEntries = new HashMap<String, Entry>();
        
        /**
         * Creates a pipeline entry.
         * 
         * @param timestamp the timestamp the pipeline was monitored
         * @param name the name of the pipeline
         */
        private PipelineEntry(long timestamp, String name) {
            super(name);
            this.timestamp = timestamp;
        }

        /**
         * Returns the timestamp the pipeline was monitored.
         * 
         * @return the timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Adds a node entry.
         * 
         * @param entry the entry (ignored if <b>null</b> or the name of the entry is <b>null</b>)
         */
        private void addNodeEntry(Entry entry) {
            if (null != entry && null != entry.getName()) {
                nodeEntries.put(entry.getName(), entry);
            }
        }

        /**
         * Returns all recorded node names.
         * 
         * @return the node names
         */
        public Collection<String> nodes() {
            return nodeEntries.keySet();
        }
        
        /**
         * Returns an entry representing a pipeline node.
         * 
         * @param name the name of the node
         * @return the node (<b>null</b> if no such node exists)
         */
        public Entry getNodeEntry(String name) {
            Entry result = null;
            if (null != name) {
                result = nodeEntries.get(name);
            }
            return result;
        }

        @Override
        public String toString() {
            return timestamp + " " + super.toString() + " nodes " + nodeEntries;
        }

    }

    /**
     * Consumes the given line.
     * 
     * @param line the line to read
     * @param prefix the prefix to consume
     * @return <code>line</code> without prefix
     */
    private String consume(String line, String prefix) {
        String result;
        if (line.startsWith(prefix)) {
            result = line.substring(prefix.length(), line.length()).trim();
        } else {
            result = line;
        }
        return result;
    }

    /**
     * Parses the given line into a set of observables.
     * 
     * @param line the line
     * @param observables the observables to be modified as a side effec
     * @throws IOException in case of I/O problems
     */
    private void parseObservables(String line, List<IObservable> observables) throws IOException {
        StringTokenizer tokens = new StringTokenizer(line, "\t");
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            IObservable obs = Observables.valueOf(token);
            if (null == obs) {
                throw new IOException("No observable registered for " + token);
            }
            observables.add(obs);
        }
    }

    /**
     * Reads an observation.
     * 
     * @param entry the entry to modify
     * @param token the token representing the observation
     * @param observables the observables in sequence
     * @param entryPos the position of the entry/observable
     * @return <code>true</code> if the next <code>entryPos</code> is out of range of <code>observables</code>, 
     *     <code>false</code> else
     * @throws IOException in case of reading errors
     */
    private boolean readObservation(Entry entry, String token, List<IObservable> observables, int entryPos) 
        throws IOException {
        int pos = entryPos - 1; // -1 is name
        if (pos < observables.size()) {
            if (!token.isEmpty()) {
                IObservable obs = observables.get(pos);
                try {
                    entry.addObservation(obs, Double.valueOf(token.replace(',', '.'))); // Excel back-mapping
                } catch (NumberFormatException e) {
                    throw new IOException("[Error] Reading observation entry nr " + entryPos + ": " + e.getMessage());
                }
            }
        }
        return pos + 1 >= observables.size();
    }

    /**
     * Reads a CSV file.
     * 
     * @param args ignored
     * @throws IOException in case of I/O problems
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("expects the file name of the CSV file to read");
            System.exit(0);
        }
        File file = new File(args[0]);
        TraceReader reader = new TraceReader();
        reader.readCsv(file);
        AlgorithmProfilePredictionManager.start();
        AlgorithmProfilePredictionManager.stop();
    }

}
