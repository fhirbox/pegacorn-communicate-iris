/* 
 * Copyright 2020 ACT Health
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * <h1>Transform Room Status Events to a (set of) FHIR Resource(s)</h1>
 * <p>
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020-01-20
 *
 */
package net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import net.fhirbox.pegacorn.referencevalues.communication.PegacornCommunicateValueReferences;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;

import net.fhirbox.pegacorn.communicate.iris.transformers.common.helpers.IdentifierBuilders;
import net.fhirbox.pegacorn.communicate.iris.transformers.TransformErrorException;
import net.fhirbox.pegacorn.communicate.iris.transformers.TransformErrorException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONException;

public class RoomCreate2GroupCreate {

    private static final Logger LOG = LoggerFactory.getLogger(RoomCreate2GroupCreate.class);

    PegacornSystemReference pegacornSystemReference = new PegacornSystemReference();
    CommunicateProperties communicateProperties = new CommunicateProperties();
    
    @Inject
    IdentifierBuilders identifierBuilders;
    
    PegacornCommunicateValueReferences pegacornCommunicateValueReferences = new PegacornCommunicateValueReferences();
    
     public Bundle matrixRoomCreateEvent2FHIRGroupBundle(String theMessage) throws TransformErrorException {
        Bundle newBundleElement = new Bundle();
        LOG.debug(".matrixRoomCreateEvent2FHIRGroupBundle(): Message In --> " + theMessage);
        Group groupElement = new Group();
        MessageHeader messageHeader = new MessageHeader();
        LOG.trace(".matrixRoomCreateEvent2FHIRGroupBundle(): Message to be converted --> " + theMessage);
        try {
            groupElement = roomCreateEvent2Group(theMessage);
            messageHeader = matrix2MessageHeader(groupElement, theMessage);
            newBundleElement.setType(Bundle.BundleType.MESSAGE);
            Bundle.BundleEntryComponent bundleEntryForMessageHeaderElement = new Bundle.BundleEntryComponent();
            bundleEntryForMessageHeaderElement.setResource(messageHeader);
            Bundle.BundleEntryComponent bundleEntryForCommunicationElement = new Bundle.BundleEntryComponent();
            Bundle.BundleEntryRequestComponent bundleRequest = new Bundle.BundleEntryRequestComponent();
            bundleRequest.setMethod(Bundle.HTTPVerb.POST);
            bundleRequest.setUrl("Group");
            bundleEntryForCommunicationElement.setRequest(bundleRequest);
            newBundleElement.addEntry(bundleEntryForMessageHeaderElement);
            newBundleElement.addEntry(bundleEntryForCommunicationElement);
            newBundleElement.setTimestamp(new Date());
            return (newBundleElement);
        } catch (JSONException jsonExtractionError) {
            throw (new TransformErrorException("matrixRoomCreateEvent2FHIRGroupBundle(): Bad JSON Message Structure -> ", jsonExtractionError));
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

    public Group roomCreateEvent2Group(String theMessage) throws TransformErrorException {
        LOG.debug(".doTransform(): Message In --> " + theMessage);
        Group localGroupElement = new Group();
        LOG.trace("Message to be converted --> " + theMessage);
        try {
            JSONObject roomStatusEvent = new JSONObject(theMessage);
            localGroupElement = buildGroupEntity(roomStatusEvent);
        } catch (Exception Ex) {
            Group emptyGroup = new Group();
            return (emptyGroup);
        }
        return (localGroupElement);
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
    private Group buildGroupEntity(JSONObject pRoomServerEvent) {
        LOG.debug(".buildDefaultGroupElement() for Event --> " + pRoomServerEvent);
        // Create the empty FHIR::Group entity.
        Group localGroupEntity = new Group();
        // Add the FHIR::Group.Identifier (type = FHIR::Identifier) Set
        localGroupEntity.addIdentifier(this.buildGroupIdentifier(pRoomServerEvent));
        Extension localRoomPriorityValue = new Extension();
        localRoomPriorityValue.setValue(new IntegerType(50));
        localRoomPriorityValue.setUrl(pegacornCommunicateValueReferences.getGroupPriorityExensionMeaning());
        localGroupEntity.addExtension(localRoomPriorityValue);
        Extension localRoomVersion = new Extension();
        Extension localRoomIsFederated = new Extension();
        Extension localRoomCreator = new Extension();
        Extension localRoomPredecessor = new Extension();
        Extension localRoomPredecessorMessage = new Extension();
        Reference localCreatorReference = null;
        if (pRoomServerEvent.has("content")) {
            JSONObject localEventContent = pRoomServerEvent.getJSONObject("content");
            if (localEventContent.has("creator")) {
                localRoomCreator.setUrl(pegacornCommunicateValueReferences.getGroupCreatorExtensionMeaning());
                localRoomCreator.setValue(new StringType(localEventContent.getString("creator")));
                localGroupEntity.addExtension(localRoomCreator);
                Identifier localCreatorIdentifier = identifierBuilders.buildPractitionerIdentifierFromSender(localEventContent.getString("creator"));
                localCreatorReference = new Reference();
                localCreatorReference.setIdentifier(localCreatorIdentifier);
                localCreatorReference.setType("Practitioner");
                localCreatorReference.setDisplay("Practitioner = " + localEventContent.getString("creator"));
            }
            if (localEventContent.has("m.federate")) {
                localRoomIsFederated.setUrl(pegacornCommunicateValueReferences.getGroupFederationStatusMeaning());
                localRoomIsFederated.setValue(new BooleanType(localEventContent.getBoolean("m.federate")));
                localGroupEntity.addExtension(localRoomIsFederated);
            }
            if (localEventContent.has("predecessor")) {
                JSONObject localPredecessor = localEventContent.getJSONObject("predecessor");
                if (localPredecessor.has("event_id")) {
                    localRoomPredecessorMessage.setUrl(pegacornCommunicateValueReferences.getGroupPredecessorRoomLastMessageExtensionMeaning());
                    localRoomPredecessorMessage.setValue(new StringType(localPredecessor.getString("event_id")));
                    localGroupEntity.addExtension(localRoomPredecessorMessage);
                }
                if (localPredecessor.has("room_id")) {
                    localRoomPredecessor.setUrl(pegacornCommunicateValueReferences.getGroupPredecssorRoomExtensionMeaning());
                    localRoomPredecessor.setValue(new StringType(localPredecessor.getString("room_id")));
                    localGroupEntity.addExtension(localRoomPredecessor);
                }
            }
            if (localEventContent.has("room_version")) {
                localRoomVersion.setUrl(pegacornCommunicateValueReferences.getGroupRoomVersionExtensionMeaning());
                localRoomVersion.setValue(new StringType(localEventContent.getString("room_version")));
                localGroupEntity.addExtension(localRoomVersion);
            }
        }
        localGroupEntity.setActive(true);
        localGroupEntity.setType(Group.GroupType.PRACTITIONER);
        localGroupEntity.setActual(true);
        localGroupEntity.setName(pRoomServerEvent.getString("room_id"));
        if (localCreatorReference != null) {
            GroupMemberComponent newGroupMember = new GroupMemberComponent();
            newGroupMember.setEntity(localCreatorReference);
            newGroupMember.setInactive(false);
            Period newMemberGroupPeriod = new Period();
            if (pRoomServerEvent.has("origin_server_ts")) {
                newMemberGroupPeriod.setStart(new Date(pRoomServerEvent.getLong("origin_server_ts")));
            } else {
                newMemberGroupPeriod.setStart(new Date());
            }
            localGroupEntity.addMember(newGroupMember);
        }
        LOG.debug(".buildDefaultGroupElement(): Created Identifier --> " + localGroupEntity.toString());
        return (localGroupEntity);
    }

    /**
     * This method constructs a basic (temporary) FHIR::Identifier entity from
     * the given message. Typically, there will be at least 2 FHIR::Identifiers
     * for a Group (where a group is used within the Pegacorn::Communicate
     * system), one being the base RoomServer ID and the other being canonical
     * defined within Pegacorn::Ladon
     *
     * @param pRoomEventMessage A Matrix(R) "m.room.create" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#m-room-create)
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
        Identifier localResourceIdentifier = this.identifierBuilders.buildGroupIdentifierFromRoomID(localRoomID, localGroupAge);
        LOG.debug(".buildGroupIdentifier(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localResourceIdentifier);
    }
}
