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

import eu.qualimaster.observables.IObservable;

/**
 * An abstract profile creator with basic implementation.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractAlgorithmProfileCreator implements IAlgorithmProfileCreator {

    @Override
    public File getPredictorPath(String pipeline, String element, String algorithm, String path, 
        IObservable observable) {
        return getStorageStrategy().getPredictorPath(pipeline, element, algorithm, path, observable, this);
    }

    @Override
    public File getPredictorPath(String algorithm, String path, IObservable observable) {
        return getStorageStrategy().getPredictorPath(null, null, algorithm, path, observable, this);
    }

}
