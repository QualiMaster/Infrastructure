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
package eu.qualimaster.adaptation.internal;

/**
 * A pluggable authentication provider for client communications.
 * 
 * @author Holger Eichelberger
 */
public interface IAuthenticationProvider {
    
    /**
     * Tries to authenticate the given <code>user</code>.
     * 
     * @param user the user name
     * @param passphrase the passphrase (depends on the authentication provider)
     * @return <code>true</code> if authenticated, <code>false</code> else
     */
    public boolean authenticate(String user, byte[] passphrase);
}
