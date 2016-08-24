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
package tests.eu.qualimaster.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.infrastructure.InfrastructurePart;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.events.MonitoringInformationEvent;
import eu.qualimaster.monitoring.events.ResourceChangedAdaptationEvent;
import eu.qualimaster.monitoring.events.SourceVolumePredictionRequest;
import eu.qualimaster.monitoring.events.SourceVolumePredictionResponse;
import eu.qualimaster.monitoring.events.ViolatingClause;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Further tests for events defined by the monitoring layer.
 * 
 * @author Holger Eichelberger
 */
public class EventTests {

    /**
     * Tests the constraint violation event.
     */
    @Test
    public void testConstraintViolationAdaptationEvent() {
        List<ViolatingClause> violating = new ArrayList<ViolatingClause>();
        ViolatingClause clause = new ViolatingClause(ResourceUsage.CAPACITY, "var", "<", 5.0, 0.5);
        violating.add(clause);
        
        FrozenSystemState state = new FrozenSystemState();
        state.setObservation(FrozenSystemState.MACHINE, "machine", TimeBehavior.ENACTMENT_DELAY, 15.0);
        ConstraintViolationAdaptationEvent sEvent = new ConstraintViolationAdaptationEvent(violating, state);
        Assert.assertEquals(violating.size(), sEvent.getViolatingClauseCount());
        Assert.assertEquals(clause, sEvent.getViolatingClause(0));
        Iterator<ViolatingClause> iter = sEvent.getViolatingClauses();
        Assert.assertTrue(iter.hasNext());
        ViolatingClause testClause = iter.next();
        Assert.assertEquals(clause.getObservable(), testClause.getObservable());
        Assert.assertEquals(clause.getVariable(), testClause.getVariable());
        Assert.assertEquals(clause.getOperation(), testClause.getOperation());
        Assert.assertEquals(clause.getDeviation(), testClause.getDeviation());
        Assert.assertEquals(clause.getDeviationPercentage(), testClause.getDeviationPercentage());
        Assert.assertEquals(state, sEvent.getState());
        Assert.assertEquals(15.0, state.getMachineObservation("machine", TimeBehavior.ENACTMENT_DELAY), 0.01);
    }
    
    /**
     * Tests the resource changed adaptation event.
     */
    @Test
    public void testResourceChangedAdaptationEvent() {
        Map<String, Double> newValues = new HashMap<String, Double>();
        newValues.put("DFEs", 4.0);
        ResourceChangedAdaptationEvent rEvent = new ResourceChangedAdaptationEvent(InfrastructurePart.HARDWARE, 
            "olynthos", null, newValues);
        Assert.assertEquals(InfrastructurePart.HARDWARE, rEvent.getPart());
        Assert.assertEquals("olynthos", rEvent.getName());
        Assert.assertNull(rEvent.getOldValue("DFEs"));
        Assert.assertEquals(4.0, rEvent.getNewValue("DFEs"), 0.5);
    }
    
    /**
     * Adaptation events tests.
     */
    @Test
    public void testAdaptationEvents() {
        // temporary
        Map<String, Double> obs = new HashMap<String, Double>();
        MonitoringInformationEvent mEvent = new MonitoringInformationEvent("type", "part", obs);
        Assert.assertEquals("type", mEvent.getPartType());
        Assert.assertEquals("part", mEvent.getPart());
        Assert.assertEquals(obs, mEvent.getObservations());
    }
    
    /**
     * Tests source volume prediction events.
     */
    @Test
    public void testSourceVolumePredictorRequests() {
        SourceVolumePredictionRequest req = new SourceVolumePredictionRequest("pip", "src", "me");
        Assert.assertEquals("pip", req.getPipeline());
        Assert.assertEquals("src", req.getSource());
        Assert.assertEquals(1, req.getKeywordCount());
        Assert.assertEquals("me", req.getKeyword(0));
        
        List<String> keywords = new ArrayList<String>();
        keywords.add("me");
        keywords.add("you");
        req = new SourceVolumePredictionRequest("pip1", "src1", keywords);
        Assert.assertEquals("pip1", req.getPipeline());
        Assert.assertEquals("src1", req.getSource());
        Assert.assertEquals(keywords.size(), req.getKeywordCount());
        for (int i = 0; i < keywords.size(); i++) {
            Assert.assertEquals(keywords.get(i), req.getKeyword(i));    
        }
        
        req.setMessageId("abba");
        req.setSenderId("here");
        
        Map<String, Double> predictions = new HashMap<String, Double>();
        for (int i = 0; i < keywords.size(); i++) {
            predictions.put(keywords.get(i), (double) i);
        }
        SourceVolumePredictionResponse resp = new SourceVolumePredictionResponse(req, predictions);
        Assert.assertEquals("abba", req.getMessageId());
        Assert.assertEquals("here", req.getSenderId());
        Assert.assertEquals(predictions, resp.getPredictions());
    }
    
}
