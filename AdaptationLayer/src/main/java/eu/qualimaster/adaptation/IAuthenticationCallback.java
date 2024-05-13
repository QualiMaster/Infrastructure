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
package eu.qualimaster.adaptation;

import eu.qualimaster.adaptation.external.RequestMessage;

/**
 * Determines whether a message is originated by an authenticated client.
 * 
 * @author Holger Eichelberger
 */
public interface IAuthenticationCallback {

    public static final IAuthenticationCallback DEFAULT = new IAuthenticationCallback() {
        
        @Override
        public boolean isAuthenticated(RequestMessage message) {
            return false;
        }
    };
    
    /**
     * Returns whether the given message is authenticated.
     * 
     * @param message the message
     * @return <code>true</code> for authenticated, <code>false</code> else
     */
    public boolean isAuthenticated(RequestMessage message);
    
}
