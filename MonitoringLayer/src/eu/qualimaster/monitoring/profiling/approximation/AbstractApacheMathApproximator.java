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
package eu.qualimaster.monitoring.profiling.approximation;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.file.Utils;
import eu.qualimaster.monitoring.profiling.Constants;
import eu.qualimaster.observables.IObservable;

/**
 * An abstract approximator based on Apache Commons Math.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractApacheMathApproximator extends AbstractApproximator {

    private static final String SEPARATOR = "\t";
    private LastRecentWeightedObservedPoints obs;
    private AbstractCurveFitter fitter = createFitter();
    private ParametricUnivariateFunction function = createFunction();

    /**
     * Creates an abstract approximator.
     * 
     * @param path the path to load a persisted version from
     * @param parameterName the parameter name
     * @param observable the observable this approximator is handling
     */
    protected AbstractApacheMathApproximator(File path, Object parameterName, IObservable observable) {
        super(path, parameterName, observable);
    }
    
    @Override
    protected void initialize() {
        obs = new LastRecentWeightedObservedPoints();
    }

    @Override
    public void update(int paramValue, double value, boolean measured) {
        obs.add(paramValue, value);
    }
    
    /**
     * Returns the fitter.
     * 
     * @return the fitter
     */
    protected abstract AbstractCurveFitter createFitter();
    
    /**
     * Returns the univariate function matching {@link #createFitter()}.
     * 
     * @return the function
     */
    protected abstract ParametricUnivariateFunction createFunction();
    
    /**
     * Returns the min sample size.
     * 
     * @return the min sampe size
     */
    protected abstract int getMinSampleSize(); 

    // checkstyle: stop exception type check
    
    @Override
    public double approximate(int paramValue) {
        double result = Constants.NO_APPROXIMATION;
        List<WeightedObservedPoint> points = obs.toList();
        if (points.size() > getMinSampleSize()) {
            try {
                double[] coeff = fitter.fit(obs.toList());
                result = function.value(paramValue, coeff);
            } catch (Throwable t) {
                LogManager.getLogger(getClass()).warn("During approximation: " + t.getMessage());
            }
        }
        return result;
    }

    // checkstyle: resume exception type check

    @Override
    public File store(File folder) {
        File file = getFile(folder);
        Utils.mkdirs(file.getParentFile());
        try (PrintWriter out = new PrintWriter(Utils.createFileWriter(file))) {
            List<WeightedObservedPoint> points = obs.toList();
            int size = points.size();
            for (int i = 0; i < size; i++) {
                WeightedObservedPoint pt = points.get(i);
                out.println(pt.getX() + SEPARATOR + pt.getY() + SEPARATOR + pt.getWeight());
            }
            out.close();
        } catch (IOException e) {
            getLogger().warn("While storing approximator: " + e.getMessage());
        }
        return file;
    }

    @Override
    protected void load(File folder) {
        File file = getFile(folder);
        if (file.exists()) {
            try (LineNumberReader in = new LineNumberReader(new FileReader(file))) {
                String line;
                do {
                    line = in.readLine();
                    if (null != line) {
                        WeightedObservedPoint pt = readPoint(file, line);
                        if (null != pt) {
                            obs.add(pt);
                        }
                    }
                } while (null != line);
                in.close();
            } catch (IOException e) {
                getLogger().warn("While loading approximator: " + e.getMessage());
            }
        }
    }
    
    /**
     * Reads a point from a line in file.
     * 
     * @param file the file (just for logging)
     * @param line the line to read
     * @return the weighted point, <b>null</b> in case of errors
     */
    private static WeightedObservedPoint readPoint(File file, String line) {
        WeightedObservedPoint result = null;
        String[] parts = line.split(SEPARATOR);
        try {
            if (null != parts && 3 == parts.length) {
                result = new WeightedObservedPoint(Double.parseDouble(parts[2]), Double.parseDouble(parts[0]), 
                    Double.parseDouble(parts[1]));
            } else {
                throw new NumberFormatException("Format does not match in line " + line);
            }
        } catch (NumberFormatException e) {
            getLogger().warn("While reading " + file + ": " + e.getMessage());
        }
        return result;
    }
    
    /**
     * Returns the file name for this approximator.
     * 
     * @param folder the base folder
     * @return the file name
     */
    private File getFile(File folder) {
        String name = getParameterName() + "-" + getObservable().name() + ".approx";
        name = Constants.toFileName(name);
        return new File(folder, name);
    }
    
    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(AbstractApacheMathApproximator.class);
    }
    
    @Override
    public boolean containsSameData(IApproximator approx) {
        boolean result = false;
        if (approx instanceof AbstractApacheMathApproximator) {
            AbstractApacheMathApproximator a = (AbstractApacheMathApproximator) approx;
            result = obs.containsSameData(a.obs);
        }
        return result;
    }
    
}
