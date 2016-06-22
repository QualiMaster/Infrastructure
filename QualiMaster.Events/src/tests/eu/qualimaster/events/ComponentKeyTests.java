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
package tests.eu.qualimaster.events;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.IRemovalSelector;

/**
 * Tests the component key class.
 * 
 * @author Holger Eichelberger
 */
public class ComponentKeyTests {

    /**
     * Tests component key functions.
     */
    @Test
    public void testComponentKey() {
        ComponentKey key = new ComponentKey("local", 1234, 5);
        Assert.assertEquals("local", key.getHostName());
        Assert.assertEquals(1234, key.getPort());
        Assert.assertEquals(5, key.getTaskId());
        Assert.assertEquals(key, key); // equals
        Assert.assertEquals(key.hashCode(), key.hashCode());
        Assert.assertFalse(key.remove(key)); // false as equals
        
        ComponentKey key2 = new ComponentKey("local1", 1234, 5);
        Assert.assertNotEquals(key, key2);
        Assert.assertTrue(key.remove(key2)); //same task, different host
        
        IRemovalSelector sel = key;
        Assert.assertTrue(sel.remove(key2)); //same task, different host
        
        ComponentKey key3 = new ComponentKey("local", 1235, 5);
        Assert.assertNotEquals(key, key3);
        Assert.assertTrue(key.remove(key3)); //same task, different port

        ComponentKey key4 = new ComponentKey("local1", 1235, 5);
        Assert.assertNotEquals(key, key4);
        Assert.assertTrue(key.remove(key4)); //same task, different port and host
        
        ComponentKey key5 = new ComponentKey("local1", 1234, 4);
        Assert.assertFalse(key.remove(key5)); // wrong task
        
        ComponentKey key6 = new ComponentKey(1234, 4);
        Assert.assertEquals(ComponentKey.getLocalHostName(), key6.getHostName());
        
        key6.toString();
        
        ComponentKey key7 = new ComponentKey("local", 1234, 5);
        key7.setThreadId(25);
        Assert.assertEquals(25, key7.getThreadId());
        
        ComponentKey parsed = ComponentKey.valueOf(key7.toString());
        Assert.assertEquals(key7.getHostName(), parsed.getHostName());
        Assert.assertEquals(key7.getPort(), parsed.getPort());
        Assert.assertEquals(key7.getTaskId(), parsed.getTaskId());
        Assert.assertEquals(key7.getThreadId(), parsed.getThreadId());
    }
    
}
