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
package eu.qualimaster.monitoring.volumePrediction;

import java.util.TimerTask;

import org.apache.log4j.LogManager;

/**
 * Preliminary timer task for updating the prediction models.
 * 
 * @author Holger Eichelberger
 */
public class ModelUpdateTask extends TimerTask {

    public final static ModelUpdateTask INSTANCE = new ModelUpdateTask();

    /**
     * Prevent external instantiation.
     */
    private ModelUpdateTask() {
    }
    
    @Override
    public void run() {
        try {
            VolumePredictionManager.updatePredictors();
        } catch (Throwable t) {
            LogManager.getLogger(getClass()).error("While updating source volume prediction models: " 
                + t.getMessage(), t);
        }
    }

}
