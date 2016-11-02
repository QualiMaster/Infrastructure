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
package eu.qualimaster.coordination.profiling;

import static eu.qualimaster.easy.extension.QmConstants.*;
import static eu.qualimaster.easy.extension.internal.Utils.*;
import static eu.qualimaster.coordination.profiling.Constants.*;
import static eu.qualimaster.coordination.profiling.SerializationHelpers.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.dataManagement.serialization.StringDataInput;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import net.ssehub.easy.instantiation.core.model.common.VilException;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.Project;
import net.ssehub.easy.varModel.model.datatypes.Compound;

/**
 * Implements a simple text-file parser.
 * 
 * @author Holger Eichelberger
 */
class SimpleParser implements IProfileControlParser {

    /**
     * Prevents package-external instantiation.
     */
    SimpleParser() {
    }

    /**
     * Parses the control file.
     * 
     * @param file the file to read
     * @param profile the profile to be built
     * @return the actual data file (from {@link IProfile#getDataFile()} if not changed by imports)
     * @throws IOException if loading/parsing the control file fails
     */
    @Override
    public ParseResult parseControlFile(File file, IProfile profile) throws IOException {
        return parseControlFile(file, true, profile);
    }
    
    /**
     * Parses the control file.
     * 
     * @param file the file to read
     * @param considerImport whether imports shall be considered or ignored
     * @param profile the profile to be built
     * @return the actual data file (from {@link IProfile#getDataFile()} if not changed by imports)
     * @throws IOException if loading/parsing the control file fails
     */
    private ParseResult parseControlFile(File file, boolean considerImport, IProfile profile) throws IOException {
        ParseResult result = new ParseResult();
        List<Integer> tmpTasks = new ArrayList<Integer>();
        List<Integer> tmpExecutors = new ArrayList<Integer>();
        List<Integer> tmpWorkers = new ArrayList<Integer>();
        result.addDataFile(profile.getDataFile());
        addDataFiles(profile.getDataFile().getParentFile(), result, profile, false);
        if (file.exists()) {
            LineNumberReader reader = new LineNumberReader(new FileReader(file));
            String line;
            do {
                line = reader.readLine();
                if (null != line) {
                    line = line.trim();
                    if (line.startsWith(IMPORT)) {
                        if (considerImport) {
                            handleImport(line, result, profile);
                        }
                    } else if (line.startsWith(PROCESSING)) {
                        line = line.substring(PROCESSING.length()).trim();
                        parseList(line, TASKS, tmpTasks, INT_HELPER);
                        parseList(line, EXECUTORS, tmpExecutors, INT_HELPER);
                        parseList(line, WORKERS, tmpWorkers, INT_HELPER);
                    } else if (line.startsWith(PARAMETER)) {
                        line = line.substring(PARAMETER.length()).trim();
                        parseParameter(line, profile, result);
                    }
                }
            } while (null != line);
            reader.close();
        }
        result.merge(tmpTasks, tmpExecutors, tmpWorkers);
        return result;
    }


    /**
     * Handles an import command.
     * 
     * @param line the line to be parsed
     * @param result the parse result to be modified as a side effect
     * @param profile the profile data
     * @throws IOException if parsing fails
     */
    private void handleImport(String line, ParseResult result, IProfile profile) throws IOException {
        boolean dataOnly = false;
        String artifact = line.substring(IMPORT.length(), line.length()).trim();
        if (artifact.startsWith(DATA + " ")) {
            dataOnly = true;
            artifact = artifact.substring(DATA.length(), artifact.length()).trim();
        }
        File base = Files.createTempDirectory("qmProfiling").toFile();
        try {
            AlgorithmProfileHelper.extractProfilingArtifact(artifact, profile.getAlgorithmName(), base);
            File cf = AlgorithmProfileHelper.getControlFile(base);
            getLogger().info("Considering imported control file " + cf);
            if (cf.exists()) {
                getLogger().info("Parsing imported control file " + cf);
                ParseResult pResult = parseControlFile(cf, false, profile);
                if (!dataOnly) {
                    result.merge(pResult, false);
                }
                addDataFiles(base, result, profile, true);
            }
        } catch (VilException e) {
            throw new IOException(e);
        }
        base.delete();
    }

    /**
     * Adds data files.
     * 
     * @param base the base file
     * @param result the parse result to be modified
     * @param profile the profile
     * @param copy copy new files or just add them
     * @throws IOException in case of I/O problems
     */
    private void addDataFiles(File base, ParseResult result, IProfile profile, boolean copy) throws IOException {
        File baseDf = AlgorithmProfileHelper.getDataFile(base);
        int i = 0;
        do {
            File df = getInstanceFile(baseDf, i);
            if (df.exists()) {
                if (copy) {
                    File dataFile = result.getDataFile(df);
                    if (null == dataFile) {
                        dataFile = getInstanceFile(profile.getDataFile(), i); // assume base
                    }
                    getLogger().info("Imported data file " + df + " exists " + df.exists() + " original " 
                        + dataFile + " exists " + dataFile.exists());
                    if (!dataFile.exists()) {
                        getLogger().info("Copying and taking over imported data file " + df + " to " 
                            + dataFile);
                        FileUtils.copyFile(df, dataFile);
                        result.addDataFile(dataFile);
                    }
                } else {
                    result.addDataFile(df);
                }
            } else {
                break;
            }
            i++;
        } while (i > 0);
    }
    
    /**
     * Returns the instance file of <code>base</code> by adding <code>index</code> if <code>index &gt; 0</code>.
     * 
     * @param base the base file
     * @param index the index number
     * @return the instance file
     */
    private static File getInstanceFile(File base, int index) {
        File result = base;
        if (index > 0) {
            String name = base.getName();
            int pos = name.lastIndexOf('.');
            if (pos >= 0) {
                name = name.substring(0, pos) + "-" + index + name.substring(pos, name.length());
            }
            result = new File(base.getParentFile(), name);
        }
        return result;
    }

    /**
     * Parses a list from a line if <code>keyword</code> is given at the beginning of <code>line</code>.
     * 
     * @param <T> the element type
     * @param line the partial line
     * @param keyword the keyword
     * @param data the data to read into (may be <b>null</b>)
     * @param helper the helper for reading data
     * @return <code>data</code> or a new list
     * @throws IOException in case that values cannot be parsed
     */
    private <T extends Serializable> List<T> parseList(String line, String keyword, List<T> data, 
        ISerializationHelper<T> helper) throws IOException {
        int pos = line.indexOf('=');
        if (pos > 0 && line.substring(0, pos).trim().equals(keyword)) {
            String remainder = line.substring(pos + 1).trim();
            data = readList(remainder, data, helper);
        }
        return data;
    }

    /**
     * Reads a list of heterogeneous data from <code>text</code> into <code>data</code> via <code>helper</code>.
     * 
     * @param <T> the element type
     * @param text the text to read from
     * @param data the data to read into (may be <b>null</b>)
     * @param helper the helper for reading data
     * @return <code>data</code> or a new list
     * @throws IOException in case that values cannot be parsed
     */
    private <T extends Serializable> List<T> readList(String text, List<T> data, ISerializationHelper<T> helper) 
        throws IOException {
        StringDataInput input = new StringDataInput(text, ','); // remainder
        if (null == data) {
            data = new ArrayList<T>();
        }
        while (!input.isEOD()) {
            data.add(helper.next(input));
        }
        return data;
    }

    /**
     * Parses parameter settings from <code>line</code>.
     * 
     * @param line the partial line
     * @param profile the profile to be built
     * @param result the result object to be modified as a side effect
     * @throws IOException in case that the parameter values cannot be parsed 
     */
    private void parseParameter(String line, IProfile profile, ParseResult result) throws IOException {
        int pos = line.indexOf('=');
        if (pos > 0) {
            String paramName = line.substring(0, pos).trim();
            String remainder = line.substring(pos + 1).trim();
            // search parameter, find type, identify helper

            ISerializationHelper<? extends Serializable> helper = null;
            Configuration config = profile.getConfiguration();
            Project cfgProject = config.getProject();
            try {
                Compound familyType = findCompound(cfgProject, TYPE_FAMILY);
                IDecisionVariable family = findNamedVariable(config, familyType, profile.getFamilyName());
                IDecisionVariable algorithm = Configuration.dereference(findAlgorithm(family, 
                    profile.getAlgorithmName(), true));
                IDecisionVariable params = algorithm.getNestedElement(SLOT_ALGORITHM_PARAMETERS);
                if (null != params) {
                    for (int p = 0; null == helper && p < params.getNestedElementsCount(); p++) {
                        IDecisionVariable param = Configuration.dereference(params.getNestedElement(p));
                        if (VariableHelper.hasName(param, paramName)) {
                            helper = SerializationHelpers.getHelper(param.getDeclaration().getType());
                        }
                    }
                    if (null != helper) {
                        StringDataInput input = new StringDataInput(remainder, ','); // remainder
                        while (!input.isEOD()) {
                            result.addParameter(paramName, helper.next(input));
                        }
                    }
                } else {
                    throw new IOException("No parameter slot defined on algorithm '" 
                        + profile.getAlgorithmName() + "'");
                }
            } catch (ModelQueryException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    private Logger getLogger() {
        return LogManager.getLogger(getClass());
    }

}
