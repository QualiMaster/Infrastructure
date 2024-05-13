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
package eu.qualimaster.adaptation.external;

/**
 * Helps authenticating with the hilarious authentication. Don't use this with the QM external service!
 * 
 * @author Holger Eichelberger
 */
public class HilariousAuthenticationHelper {

    /**
     * Returns a valid passphrase for <code>user</code>.
     * 
     * @param user the user name (may be <b>null</b> or empty, no guarantee for the correctness of the output)
     * @return the passphrase
     */
    public static byte[] obtainPassphrase(String user) {
        int hash = 0;
        if (null != user) {
            hash = user.hashCode();
        }
        String tmp = String.valueOf(hash);
        return tmp.getBytes();
    }
    
}
