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
 * Just a very primitive and intentionally not secure (hilarious) authentication provider for the beginning.
 * The class name is a remainder of the London Meeting ;) Might be replaced by an authentication against LUH 
 * if possible so that the QM-IConf authentication can be reused.
 * 
 * @author Holger Eichelberger
 */
public class HilariousAuthenticationProvider implements IAuthenticationProvider {

    public static final IAuthenticationProvider INSTANCE = new HilariousAuthenticationProvider();
    
    /**
     * Creates an instance.
     */
    private HilariousAuthenticationProvider() {
    }
    
    @Override
    public boolean authenticate(String user, byte[] passphrase) {
        boolean result = false;
        if (null != user && user.length() > 0 && null != passphrase) {
            String pp = new String(passphrase);
            try {
                int pi = Integer.parseInt(pp); 
                result = user.hashCode() == pi;
            } catch (NumberFormatException e) {
            }
        }
        return result;
    }

}
