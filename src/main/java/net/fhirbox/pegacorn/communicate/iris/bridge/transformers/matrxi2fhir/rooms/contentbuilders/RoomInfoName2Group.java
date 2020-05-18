/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.rooms.contentbuilders;

import java.util.Date;
import javax.inject.Inject;

import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MinorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixRoomID2MatrixRoomNameMap;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixRoomID2ResourceReferenceMap;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common.MatrixAttribute2FHIRIdentifierBuilders;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import net.fhirbox.pegacorn.fhir.r4.model.common.GroupPC;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
public class RoomInfoName2Group {

    private static final Logger LOG = LoggerFactory.getLogger(RoomInfoName2Group.class);

    @Inject
    PegacornSystemReference pegacornSystemReference;

    @Inject
    CommunicateProperties communicateProperties;

    @Inject
    MatrixAttribute2FHIRIdentifierBuilders identifierBuilders;

    @Inject
    MatrixRoomID2ResourceReferenceMap roomId2ResourceMap;

    @Inject
    MatrixRoomID2MatrixRoomNameMap roomNameMap;
    
    public Bundle matrixRoomNameEvent2FHIRGroupBundle(String theMessage) throws MinorTransformationException {
        Bundle newBundleElement = new Bundle();
        LOG.debug(".matrixRoomNameEvent2FHIRGroupBundle(): Message In --> " + theMessage);
        GroupPC groupElement = new GroupPC();
        MessageHeader messageHeader = new MessageHeader();
        LOG.trace(".matrixRoomNameEvent2FHIRGroupBundle(): Message to be converted --> " + theMessage);
        try {
            groupElement = roomInfoNameEvent2Group(theMessage);
            messageHeader = matrix2MessageHeader(groupElement, theMessage);
            newBundleElement.setType(Bundle.BundleType.MESSAGE);
            Bundle.BundleEntryComponent bundleEntryForMessageHeaderElement = new Bundle.BundleEntryComponent();
            bundleEntryForMessageHeaderElement.setResource(messageHeader);
            Bundle.BundleEntryComponent bundleEntryForCommunicationElement = new Bundle.BundleEntryComponent();
            Bundle.BundleEntryRequestComponent bundleRequest = new Bundle.BundleEntryRequestComponent();
            bundleRequest.setMethod(Bundle.HTTPVerb.PUT);
            bundleRequest.setUrl("Group");
            bundleEntryForCommunicationElement.setRequest(bundleRequest);
            newBundleElement.addEntry(bundleEntryForMessageHeaderElement);
            newBundleElement.addEntry(bundleEntryForCommunicationElement);
            newBundleElement.setTimestamp(new Date());
            return (newBundleElement);
        } catch (JSONException jsonExtractionError) {
            throw (new MinorTransformationException("matrixRoomNameEvent2FHIRGroupBundle(): Bad JSON Message Structure -> ", jsonExtractionError));
        }
    }
    
    public MessageHeader matrix2MessageHeader(Group theResultantGroupElement, String theMessage) {
        MessageHeader messageHeaderElement = new MessageHeader();
        Coding messageHeaderCode = new Coding();
        messageHeaderCode.setSystem("http://pegacorn.fhirbox.net/pegacorn/R1/message-codes");
        messageHeaderCode.setCode("group-bundle");
        messageHeaderElement.setEvent(messageHeaderCode);
        MessageHeader.MessageSourceComponent messageSource = new MessageHeader.MessageSourceComponent();
        messageSource.setName("Pegacorn Matrix2FHIR Integration Service");
        messageSource.setSoftware("Pegacorn::Communicate::Iris");
        messageSource.setEndpoint(communicateProperties.getIrisEndPointForIncomingGroupBundle());
        return (messageHeaderElement);
    }

    public GroupPC roomInfoNameEvent2Group(String theMessage) throws MinorTransformationException {
        LOG.debug(".roomInfoNameEvent2Group(): Message In --> " + theMessage);
        GroupPC newGroup;
        LOG.trace(".roomInfoNameEvent2Group(): Message to be converted --> " + theMessage);
        try {
            JSONObject roomStatusEvent = new JSONObject(theMessage);
            newGroup = buildGroupEntityFromRoomNameEvent(roomStatusEvent);
        } catch (Exception Ex) {
            GroupPC emptyGroup = new GroupPC();
            return (emptyGroup);
        }
        return (newGroup);
    }

    /**
     * This method constructs a basic FHIR::Group entity and then calls a the
     * other methods within this class to populate the relevant attributes.
     *
     * @param pMessageObject A Matrix(R) "m.room.create" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#m-room-create)
     * @return Communication A FHIR::Communication resource (see
     * https://www.hl7.org/fhir/group.html)
     */
    private GroupPC buildGroupEntityFromRoomNameEvent(JSONObject pRoomServerEvent) throws MinorTransformationException{
        LOG.debug(".buildGroupEntityFromRoomNameEvent() for Event --> " + pRoomServerEvent);
        if( !pRoomServerEvent.has("content") ){
            throw(new MinorTransformationException("m.room.name event has no -content-"));
        }
        JSONObject roomServerEventContent = pRoomServerEvent.getJSONObject("content");
        if( !roomServerEventContent.has("name")){
            throw(new MinorTransformationException("m.room.name event has no -name-"));
        }
        // Create the empty FHIR::Group entity.
        GroupPC newGroup = new GroupPC();
        // Add the FHIR::Group.Identifier (type = FHIR::Identifier) Set
        newGroup.addIdentifier(this.buildGroupIdentifier(pRoomServerEvent));
        Extension localRoomPriorityValue = new Extension();
        newGroup.setGroupPriority(50);
        newGroup.setActive(true);
        newGroup.setType(Group.GroupType.PRACTITIONER);
        newGroup.setActual(true);
        newGroup.setName(roomServerEventContent.getString("name"));
        if(roomNameMap.getName(pRoomServerEvent.getString("room_id")) == null){
            LOG.trace("buildGroupEntityFromRoomNameEvent(): No existing name in RoomID2RoomNameReferenceMap, so adding");
            roomNameMap.setName(pRoomServerEvent.getString("room_id"), roomServerEventContent.getString("name"));
        } else {
            LOG.trace("buildGroupEntityFromRoomNameEvent(): An existing name in RoomID2RoomNameReferenceMap, so modifying");
            roomNameMap.modifyName(pRoomServerEvent.getString("room_id"), roomServerEventContent.getString("name"));
        }
        LOG.debug(".buildGroupEntityFromRoomNameEvent(): Created Identifier --> " + newGroup.toString());
        return (newGroup);
    }

    /**
     * This method constructs a basic FHIR::Identifier entity from
     * the given message. Typically, there will be at least 2 FHIR::Identifiers
     * for a Group (where a group is used within the Pegacorn::Communicate
     * system), one being the base RoomServer ID and the other being canonical
     * defined within Pegacorn::Ladon
     *
     * @param pRoomEventMessage A Matrix(R) "m.room.name" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#m-room-name)
     * @return Identifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/datatypes.html#Identifier)
     */
    private Identifier buildGroupIdentifier(JSONObject pRoomEventMessage) {
        if ((pRoomEventMessage == null) || pRoomEventMessage.isEmpty()) {
            LOG.debug("buildGroupIdentifier(): Room Event Message is Empty");
            return (null);
        }
        String localRoomID = pRoomEventMessage.getString("room_id");
        if (localRoomID.isEmpty()) {
            LOG.debug("buildGroupIdentifier(): Room ID from RoomServer is Empty");
            return (null);
        }
        LOG.trace(".buildGroupIdentifier(): for Event --> " + localRoomID);
        Long localGroupAge;
        if (pRoomEventMessage.has("origin_server_ts")) {
            localGroupAge = pRoomEventMessage.getLong("origin_server_ts");
        } else {
            localGroupAge = 0L;
        }
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = this.identifierBuilders.buildFHIRGroupIdentifierFromMatrixRoomID(localRoomID, localGroupAge);
        LOG.debug(".buildGroupIdentifier(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localResourceIdentifier);
    }
}
