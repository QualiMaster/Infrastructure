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
 * Indicates a change of a resource (admin event).
 * 
 * @author Holger Eichelberger
 */
public class ResourceChangeRequest extends RequestMessage {

    private static final long serialVersionUID = -9177806481463539059L;
    private String resource;
    private Status status;
    
    /**
     * Defines the target status.
     * 
     * @author Holger Eichelberger
     */
    public enum Status {
        ENABLED,
        DISABLED,
        ADDED,
        REMOVED;
    }

    /**
     * Creates a resource change message.
     * 
     * @param resource the name of the resource
     * @param status the target status
     */
    public ResourceChangeRequest(String resource, Status status) {
        this.resource = resource;
        this.status = status;
    }

    /**
     * Returns the resource name.
     * 
     * @return the resource name
     */
    public String getResource() {
        return resource;
    }
    
    /**
     * Returns the intended status.
     * 
     * @return the status
     */
    public Status getStatus() {
        return status;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleResourceChangeMessage(this);
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(getResource()) + Utils.hashCode(getStatus());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof ResourceChangeRequest) {
            ResourceChangeRequest msg = (ResourceChangeRequest) obj;
            equals = Utils.equals(getResource(), msg.getResource());
            equals &= Utils.equals(getStatus(), msg.getStatus());
        }
        return equals;
    }
    
    @Override
    public Message toInformation() {
        return new InformationMessage(resource, null, status.name().toLowerCase() , null);
    }

    @Override
    public String toString() {
        return "ResourceChangeMessage " + resource + " " + status;
    }

    @Override
    public Message elevate() {
        return new ElevatedResourceChangeMessage(this);
    }

    /**
     * Implements an elevated change parameter request.
     * 
     * @author Holger Eichelberger
     */
    private static class ElevatedResourceChangeMessage extends ResourceChangeRequest {

        private static final long serialVersionUID = -6686358436811866848L;

        /**
         * Creates an elevated resource change request.
         * 
         * @param request the original request
         */
        private ElevatedResourceChangeMessage(ResourceChangeRequest request) {
            super(request.getResource(), request.getStatus());
            setMessageId(request.getMessageId());
            setClientId(request.getClientId());
        }
        
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
    
}
