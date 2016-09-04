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
package eu.qualimaster.monitoring.profiling;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * An abstract predictor for matrix-based predictors.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractMatrixPredictor implements IAlgorithmProfilePredictor {

    /** 
     * Generates a String representation of a {@link IAlgorithmProfilePredictor} instance.
     * 
     * @return a properties object with the instance data
     */
    protected abstract Properties toProperties();
    
    /**
     * Sets the internal structures based on a string representation. Must fit the structure of 
     * {@link #toStringArrayList()}.
     * 
     * @param data the data to set
     */
    protected abstract void setProperties(Properties data);
    
    @Override
    public void store(File file, String identifier) throws IOException {
        Utils.store(file, toProperties());
    }

    @Override
    public void load(File file, String key) throws IOException {
        Properties prop = new Properties();
        Utils.load(file, prop);
        setProperties(prop);
    }
    
}
