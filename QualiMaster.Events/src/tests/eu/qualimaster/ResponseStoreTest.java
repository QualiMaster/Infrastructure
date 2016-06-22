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
package tests.eu.qualimaster;

import org.junit.Test;
import org.junit.Assert;

import eu.qualimaster.events.AbstractResponseEvent;
import eu.qualimaster.events.AbstractReturnableEvent;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.events.IResponseEvent;
import eu.qualimaster.events.IReturnableEvent;
import eu.qualimaster.events.ResponseStore;

/**
 * Tests {@link ResponseStore}.
 * 
 * @author Holger Eichelberger
 */
public class ResponseStoreTest {

    /**
     * Implements a test returnable element.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("serial")
    private static class Returnable extends AbstractReturnableEvent {

        /**
         * Creates a returnable event for testing.
         * 
         * @param messageId the message id
         */
        private Returnable(String messageId) {
            super();
            setMessageId(messageId);
        }
        
    }
    
    /**
     * Implements a response event.
     * 
     * @author Holger Eichelberger
     */
    @SuppressWarnings("serial")
    private static class Response extends AbstractResponseEvent<Returnable> {

        /**
         * Creates a response event for a {@link Returnable}.
         * 
         * @param returnable the causing returnable
         */
        public Response(Returnable returnable) {
            super(returnable);
        }
        
    }
    
    /**
     * Tests the response store.
     * 
     * @throws InterruptedException shall not occur 
     */
    @Test
    public void testResponseStore() throws InterruptedException {
        ResponseStore<IEvent, IReturnableEvent, IResponseEvent> store = ResponseStore.createDefaultStore(10);
        Returnable ret = new Returnable("aabb-ccc");

        store.sent(ret);
        Assert.assertTrue(!store.isEmpty());
        Response res = new Response(ret);
        IReturnableEvent r = store.received(res);
        Assert.assertNotNull(r);
        Assert.assertEquals(ret.getMessageId(), r.getMessageId());
        Assert.assertTrue(store.isEmpty());

        IEvent eRet = ret;
        store.sentEvent(eRet);
        Assert.assertTrue(!store.isEmpty());
        IEvent eRes = res;
        r = store.receivedEvent(eRes);
        Assert.assertNotNull(r);
        Assert.assertEquals(ret.getMessageId(), r.getMessageId());
        Assert.assertTrue(store.isEmpty());
        
        store.sent(ret);
        Assert.assertTrue(!store.isEmpty());
        Thread.sleep(30);
        store.clear();
        Assert.assertTrue(store.isEmpty());

        store.sent(ret);
        Assert.assertTrue(!store.isEmpty());
        store.clearAll();
        Assert.assertTrue(store.isEmpty());
    }

}
