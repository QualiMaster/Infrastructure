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
package eu.qualimaster.adaptation.external;

/**
 * The base class for privileged messages, i.e., messages requiring an authenticated client connection.
 * As further types of privileged messages may exist, please never use instanceof on the type of this class
 * to determine whether a message requires authentication. Use {@link #requiresAuthentication()} instead. 
 * 
 * @author Holger Eichelberger
 */
public abstract class PrivilegedMessage extends UsualMessage {

    private static final long serialVersionUID = -8928478158493881391L;

    @Override
    public final boolean requiresAuthentication() {
        return true; // privileged messages require always an authenticated connection, no reduction possible
    }

    @Override
    public final boolean passToUnauthenticatedClient() {
        return false; // pass never
    }
    
    @Override
    public final Message elevate() {
        return this; // we are already elevated
    }
}
