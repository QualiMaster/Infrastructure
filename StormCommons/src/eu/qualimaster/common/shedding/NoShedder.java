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
package eu.qualimaster.common.shedding;

/**
 * A dummy shedder that does no shadding.
 * 
 * @author Holger Eichelberger
 */
public class NoShedder extends LoadShedder<Object> {

    public static final NoShedder INSTANCE = new NoShedder();
    private static final long serialVersionUID = 9191712027878070454L;
    
    /**
     * Creates the shedder.
     */
    private NoShedder() {
        super(DefaultLoadShedders.NO_SHEDDING, Object.class);
    }

    @Override
    protected boolean isEnabledImpl(Object tuple) {
        return true;
    }

    @Override
    public void configure(ILoadShedderConfigurer configurer) {
    }

}
