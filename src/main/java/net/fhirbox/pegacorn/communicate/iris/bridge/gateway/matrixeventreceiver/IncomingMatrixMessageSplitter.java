/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.bridge.gateway.matrixeventreceiver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import javax.enterprise.context.ApplicationScoped;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class IncomingMatrixMessageSplitter {

    private static final Logger LOG = LoggerFactory.getLogger(IncomingMatrixMessageSplitter.class);

    public LinkedHashSet<String> splitMessageIntoEvents(String pRoomServerMessage) {
        LOG.debug("splitMessageIntoEvents(): Entry: Message to split -->" + pRoomServerMessage);
        LinkedHashSet<String> eventSet = new LinkedHashSet<String>();
        if (pRoomServerMessage.isEmpty()) {
            LOG.debug("splitMessageIntoEvents(): Exit: Empty message");
            return (eventSet);
        }
        JSONObject localMessageObject = new JSONObject(pRoomServerMessage);
        LOG.trace("splitMessageIntoEvents(): Converted to JSONObject --> " + localMessageObject.toString());
        JSONArray localMessageEvents = localMessageObject.getJSONArray("events");
        LOG.trace("splitMessageIntoEvents(): Converted to JSONArray, number of elements --> " + localMessageEvents.length());
        for( Integer counter = 0; counter < localMessageEvents.length(); counter += 1){
            JSONObject eventInstance = localMessageEvents.getJSONObject(counter);
            LOG.trace("splitMessageIntoEvents(): Exctracted JSONObject --> " + eventInstance.toString());
            eventSet.add(eventInstance.toString());
            LOG.trace("splitMessageIntoEvents(): Added JSONObject to eventSet, count --> " + eventSet.size());
        }
        LOG.debug("splitMessageIntoEvents(): Exit: Event count --> " + eventSet.size());
        return (eventSet);
    }
}
