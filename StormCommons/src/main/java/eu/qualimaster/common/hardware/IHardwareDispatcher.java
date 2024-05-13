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
package eu.qualimaster.common.hardware;

/**
 * Defines the interface for obtaining responses.
 * 
 * @author Holger Eichelberger
 */
public interface IHardwareDispatcher {

    /**
     * Called when an upload is finished.
     * 
     * @param msg the received message
     */
    public void received(UploadMessageOut msg);

    /**
     * Called when the information about running state of an algorithm is available.
     * 
     * @param msg the received message
     */
    public void received(IsRunningAlgorithmOut msg);
    
    /**
     * Called when the result about stopping an algorithm is available.
     * 
     * @param msg the received message
     */
    public void received(StopMessageOut msg);
    
    /**
     * Called when the server terminated.
     */
    public void serverTerminated();
    
}