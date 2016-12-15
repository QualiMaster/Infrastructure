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
    private ISwitchTupleSerializerCreator swiSer;
    private SynchronizedQueue<IGeneralTuple> syn;
    private SynchronizedQueue<IGeneralTuple> tmpSyn;

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

    /**
     * Creates the creator.
     *
     * @param genSer the serializer for the general tuple {@link IGeneralTuple}
     * @param swiSer the serializer for the switch tuple {@link ISwitchTuple}
     * @param syn the general queue for storing tuples
     * @param tmpSyn the temporary queue for storing tuples
     */
    public TupleReceiverHandlerCreator(IGeneralTupleSerializerCreator genSer, ISwitchTupleSerializerCreator swiSer, 
        SynchronizedQueue<IGeneralTuple> syn, SynchronizedQueue<IGeneralTuple> tmpSyn) {
        this.genSer = genSer;
        this.swiSer = swiSer;
        this.syn = syn;
        this.tmpSyn = tmpSyn;
    }

    @Override
    public ITupleReceiverHandler create(boolean switchHandler) {
        ITupleReceiverHandler result;
        if (switchHandler) {
            result = new TupleReceiverHandler(swiSer.createSwitchTupleSerializer(), syn);
        } else {
            if (null != swiSer) {
                result = new TupleReceiverHandler(genSer.createGeneralTupleSerializer(), 
                    swiSer.createSwitchTupleSerializer(), syn, tmpSyn);
            } else {
                result = new TupleReceiverHandler(genSer.createGeneralTupleSerializer(), syn);
            }
        }
        return result;
    }
 
}
