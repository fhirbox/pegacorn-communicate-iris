/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.helpers;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
public class RoomServerMessageSplitter {

    private static final Logger LOG = LoggerFactory.getLogger(RoomServerMessageSplitter.class);

    public List<String> splitMessageIntoEvents(String pRoomServerMessage) {
        LOG.debug("splitMessageIntoEvents(): Entry: Message to split -->" + pRoomServerMessage);
        ArrayList<String> eventSet = new ArrayList<String>();
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
