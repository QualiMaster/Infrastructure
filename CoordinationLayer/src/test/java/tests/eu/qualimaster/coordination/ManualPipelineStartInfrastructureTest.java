/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.coordination;

import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.events.EventManager;

/**
 * Just a manual test as the {@link eu.qualimaster.infrastructure.PipelineOptions} cause problems. Execute 
 * {@link ManualPipelineStartTest} in parallel.
 * 
 * @author Holger Eichelberger
 */
public class ManualPipelineStartInfrastructureTest {

    /**
     * Starts up a portion of the QM infrastructure and waits.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        CoordinationConfiguration.configureLocal();
        EventManager.start(false, true);
        CoordinationManager.start();
        System.out.println("Waiting...");
        // and wait
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }
    
}
