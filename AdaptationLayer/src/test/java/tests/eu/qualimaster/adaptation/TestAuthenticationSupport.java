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
package tests.eu.qualimaster.adaptation;

import org.junit.Test;
import org.junit.Assert;

import eu.qualimaster.adaptation.external.AuthenticateMessage;
import eu.qualimaster.adaptation.external.ClientEndpoint;
import eu.qualimaster.adaptation.external.HilariousAuthenticationHelper;
import eu.qualimaster.adaptation.internal.HilariousAuthenticationProvider;
import eu.qualimaster.adaptation.internal.IAuthenticationProvider;

/**
 * Defines the authentication provider for testing and allows to obtain a matching password for an user.
 * 
 * @author Holger Eichelberger
 */
public class TestAuthenticationSupport {
    
    public static final IAuthenticationProvider PROVIDER = HilariousAuthenticationProvider.INSTANCE;
    
    /**
     * Tests the authentication provider.
     */
    @Test
    public void testAuthentication() {
        Assert.assertFalse(PROVIDER.authenticate(null, null));
        Assert.assertFalse(PROVIDER.authenticate(null, "bla".getBytes()));
        Assert.assertFalse(PROVIDER.authenticate("bla", null));
        Assert.assertFalse(PROVIDER.authenticate("bla", "bla".getBytes()));
        Assert.assertFalse(PROVIDER.authenticate("bla", HilariousAuthenticationHelper.obtainPassphrase(null)));
        Assert.assertFalse(PROVIDER.authenticate("bla", HilariousAuthenticationHelper.obtainPassphrase("")));
        Assert.assertTrue(PROVIDER.authenticate("bla", HilariousAuthenticationHelper.obtainPassphrase("bla")));
    }
    
    /**
     * Helper method for authentication.
     * 
     * @param endpoint the client endpoint to authenticate against
     * @param user the user to authenticate for
     * @return the authentication message
     */
    public static AuthenticateMessage authenticate(ClientEndpoint endpoint, String user) {
        AuthenticateMessage msg = new AuthenticateMessage(user, HilariousAuthenticationHelper.obtainPassphrase(user));
        endpoint.schedule(msg);
        return msg;
    }

}
