package net.fhirbox.pegacorn.communicate.iris.wups.matrixeventreceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class IncomingEventListValidator {

    private static final Logger LOG = LoggerFactory.getLogger(IncomingEventListValidator.class);

    public String validateEventSetMessage(String pRoomServerEventMessageSet) {
        LOG.debug("validateEventSetMessage(): Validating message --> " + pRoomServerEventMessageSet);
        if (pRoomServerEventMessageSet.isEmpty()) {
            LOG.debug("validateEventSetMessage(): Empty message");
            return (pRoomServerEventMessageSet);
        }
        JSONObject localMessageEventObject = new JSONObject(pRoomServerEventMessageSet);
        LOG.trace("validateEventSetMessage(): Converted to JSONObject --> " + localMessageEventObject.toString());
        JSONArray localMessageEvents = localMessageEventObject.getJSONArray("events");
        LOG.trace("validateEventSetMessage(): Converted to JSONArray, number of elements --> " + localMessageEvents.length());
        LOG.debug("validateEventSetMessage(): Valid message containing " + localMessageEvents.length() + " events");
        return (pRoomServerEventMessageSet);
    }
}
