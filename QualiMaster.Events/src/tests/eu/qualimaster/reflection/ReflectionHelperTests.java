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
package tests.eu.qualimaster.reflection;

import org.junit.Test;

import eu.qualimaster.reflection.ReflectionHelper;
import org.junit.Assert;

/**
 * Tests {@link ReflectionHelper}.
 * 
 * @author Holger Eichelberger
 */
public class ReflectionHelperTests {
    
    /**
     * Tests the instance creation wrapper method.
     * 
     * @throws IllegalAccessException shall not occur
     * @throws InstantiationException shall not occur
     */
    @Test
    public void createInstanceTest() throws IllegalAccessException, InstantiationException {
        Assert.assertNotNull(ReflectionHelper.createInstance(Object.class));
    }

}
