/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.plugins.testPlugins;

import eu.qualimaster.plugins.ILayerDescriptor;
import eu.qualimaster.plugins.IPlugin;
import tests.eu.qualimaster.plugins.TestPhases;

/**
 * A simple test plugin with lifecycle.
 * 
 * @author Holger Eichelberger
 */
public class LifecyclePlugin implements IPlugin {

    // we do not have a no-arg constructur
    
    @Override
    public ILayerDescriptor assignedTo(Action action) {
        ILayerDescriptor result;
        switch (action) {
        case START: 
            result = TestPhases.TEST2;
            break;
        case SHUTDOWN: 
            result = TestPhases.TEST3;
            break;
        default:
            result = null;
            break;
        }
        return result;
    }

    @Override
    public void execute(Action action) {
        System.out.println("Lifecycle plugin: " + action);
    }

}
