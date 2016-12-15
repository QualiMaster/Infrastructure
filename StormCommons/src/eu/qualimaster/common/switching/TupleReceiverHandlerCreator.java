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
package eu.qualimaster.common.switching;

import eu.qualimaster.base.algorithm.IGeneralTuple;

/**
 * Implements the default tuple receiver creator for {@link TupleReceiverHandler}.
 * 
 * @author Holger Eichelberger
 */
public class TupleReceiverHandlerCreator implements ITupleReceiveCreator {

    private IGeneralTupleSerializerCreator genSer;
    private SynchronizedQueue<IGeneralTuple> syn;

    /**
     * Creates the creator.
     *
     * @param genSer the serializer for the general tuple {@link IGeneralTuple}
     * @param syn the queue for storing tuples
     */
    public TupleReceiverHandlerCreator(IGeneralTupleSerializerCreator genSer, SynchronizedQueue<IGeneralTuple> syn) {
        this.genSer = genSer;
        this.syn = syn;
    }
    
    @Override
    public ITupleReceiverHandler create() {
        return new TupleReceiverHandler(genSer.createGeneralTupleSerializer(), syn);
    }

}
