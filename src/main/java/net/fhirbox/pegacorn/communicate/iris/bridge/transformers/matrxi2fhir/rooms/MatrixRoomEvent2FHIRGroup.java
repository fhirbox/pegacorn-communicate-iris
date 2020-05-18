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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.rooms;

import java.util.Date;
import java.util.Iterator;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.StringType;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MinorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common.MatrixAttribute2FHIRIdentifierBuilders;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.fhir.r4.model.common.GroupPC;
import net.fhirbox.pegacorn.fhir.r4.model.common.helpers.GroupJoinRuleStatusEnum;
import net.fhirbox.pegacorn.fhir.r4.model.common.helpers.IdentifierExtensionMeanings;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.json.JSONArray;
import org.json.JSONException;

public class MatrixRoomEvent2FHIRGroup
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomEvent2FHIRGroup.class);

    @Inject
    PegacornSystemReference pegacornSystemReference;

    @Inject
    CommunicateProperties communicateProperties;

    @Inject
    MatrixAttribute2FHIRIdentifierBuilders identifierBuilders;

    private MatrixRoomEvent2FHIRGroupAttributeBuilders groupAttributeBuilders = new MatrixRoomEvent2FHIRGroupAttributeBuilders();

    public Bundle matrixRoomCreateEvent2FHIRGroupBundle(String theMessage) throws MinorTransformationException
    {
        LOG.debug(".matrixRoomCreateEvent2FHIRGroupBundle(): Message In --> " + theMessage);
        Bundle newBundleElement = new Bundle();
        GroupPC groupElement = new GroupPC();
        MessageHeader messageHeader = new MessageHeader();
        LOG.trace(".matrixRoomCreateEvent2FHIRGroupBundle(): Message to be converted --> " + theMessage);
        try {
            groupElement = roomCreateEvent2Group(theMessage);
            if (groupElement == null) {
                LOG.debug(".matrixRoomCreateEvent2FHIRGroupBundle(): Exit, empty message, typically because there is nothing of interest for Ladon");
                return (null);
            }
            LOG.trace("matrixRoomCreateEvent2FHIRGroupBundle(): Created GroupPER element, now build MessageHeader");
            messageHeader = matrix2MessageHeader(groupElement, theMessage);
            LOG.trace("matrixRoomCreateEvent2FHIRGroupBundle(): Built MessageHeader, now build the Bundle");
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
            LOG.debug(".matrixRoomCreateEvent2FHIRGroupBundle(): Exit, returning new GroupPER Bundle --> {}", newBundleElement);
            return (newBundleElement);
        } catch (JSONException jsonExtractionError) {
            throw (new MinorTransformationException("matrixRoomCreateEvent2FHIRGroupBundle(): Bad JSON Message Structure -> ", jsonExtractionError));
        }
    }

    public MessageHeader matrix2MessageHeader(Group theResultantGroupElement, String theMessage)
    {
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

    public GroupPC roomCreateEvent2Group(String theMessage) throws MinorTransformationException
    {
        LOG.debug(".doTransform(): Message In --> " + theMessage);
        GroupPC localGroupElement = new GroupPC();
        LOG.trace("Message to be converted --> " + theMessage);
        try {
            JSONObject roomStatusEvent = new JSONObject(theMessage);
            localGroupElement = buildFHIRGroupFromMatrixRoomEvent(roomStatusEvent);
        } catch (Exception Ex) {
            GroupPC emptyGroup = new GroupPC();
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
    private GroupPC buildFHIRGroupFromMatrixRoomEvent(JSONObject roomEvent)
    {
        LOG.debug(".buildDefaultGroupElement() for Event --> " + roomEvent);
        // Create the empty Pegacorn::FHIR::R4::Group entity.
        GroupPC theTargetGroup = new GroupPC();
        // Add the FHIR::Group.Identifier (type = FHIR::Identifier) Set
        theTargetGroup.addIdentifier(this.groupAttributeBuilders.buildGroupIdentifier(roomEvent.getString("room_id")));
        // Set the group type --> PRACTITIONER (all our groups are based on Practitioners)
        theTargetGroup.setType(Group.GroupType.PRACTITIONER);
        // The group is active
        theTargetGroup.setActual(true);

        LOG.trace("buildGroupEntity(): Extracting -content- subfield set");
        JSONObject roomCreateContent = roomEvent.getJSONObject("content");

        switch (roomEvent.getString("type")) {
            case "m.room.create":
                LOG.trace("buildGroupEntity(): Is a m.room.create event");
                Reference roomManager = this.groupAttributeBuilders.buildGroupManagerReference(roomCreateContent);
                LOG.trace("buildGroupEntity(): Adding the Group Manager --> {} ", roomManager);
                theTargetGroup.setManagingEntity(roomManager);
                if (roomCreateContent.has("m.federate")) {
                    LOG.trace("buildGroupEntity(): Setting the Federated Flag Extension");
                    theTargetGroup.setFederationStatus(roomCreateContent.getBoolean("m.federate"));
                }
                if (roomCreateContent.has("room_version")) {
                    LOG.trace("buildGroupEntity(): Setting the Room Version Extension");
                    theTargetGroup.setChatGroupVersion(roomCreateContent.getInt("room_version"));
                }
                break;
            case "m.room.join_rules":
                LOG.trace("buildGroupEntity(): Is a m.room.join_rules event");
                if (roomCreateContent.has("join_rule")) {
                    LOG.trace("buildGroupEntity(): Setting the Join Rule Extension");
                    switch (roomCreateContent.getString("join_rule")) {
                        case "public":
                            LOG.trace("buildGroupEntity(): Setting Group -join_rule- to --> {}", GroupJoinRuleStatusEnum.JOINRULE_STATUS_PUBLIC);
                            theTargetGroup.setJoinRule(GroupJoinRuleStatusEnum.JOINRULE_STATUS_PUBLIC);
                            break;
                        case "knock":
                            LOG.trace("buildGroupEntity(): Setting Group -join_rule- to --> {}", GroupJoinRuleStatusEnum.JOINRULE_STATUS_KNOCK);
                            theTargetGroup.setJoinRule(GroupJoinRuleStatusEnum.JOINRULE_STATUS_KNOCK);
                            break;
                        case "invite":
                            LOG.trace("buildGroupEntity(): Setting Group -join_rule- to --> {}", GroupJoinRuleStatusEnum.JOINRULE_STATUS_INVITE);
                            theTargetGroup.setJoinRule(GroupJoinRuleStatusEnum.JOINRULE_STATUS_INVITE);
                            break;
                        case "private":
                        default:
                            LOG.trace("buildGroupEntity(): Setting Group -join_rule- to --> {}", GroupJoinRuleStatusEnum.JOINRULE_STATUS_PRIVATE);
                            theTargetGroup.setJoinRule(GroupJoinRuleStatusEnum.JOINRULE_STATUS_PRIVATE);
                            break;
                    }
                }
                break;
            case "m.room.canonical_alias":
                LOG.trace("buildGroupEntity(): Is a m.room.canonical_alias event");
                if (roomCreateContent.has("alias")) {
                    LOG.trace("buildGroupEntity(): Adding {} as the Canonical Alias Extension + adding it as another Identifier", roomCreateContent.get("alias"));
                    Identifier additionalIdentifier = this.groupAttributeBuilders.buildGroupIdentifier(roomCreateContent.getString("alias"));
                    theTargetGroup.addIdentifier(additionalIdentifier);
                    theTargetGroup.setCanonicalAlias(additionalIdentifier);
                }
                break;
            case "m.room.aliases":
                LOG.trace("buildGroupEntity(): Is a m.room.aliases event");
                if (roomCreateContent.has("aliases")) {
                    LOG.trace("buildGroupEntity(): Adding {} as the Alias to room (i.e. a new Identifier for each Alias) --> {}", roomCreateContent.get("aliases"));
                    JSONArray aliasSet = roomCreateContent.getJSONArray("aliases");
                    LOG.trace("buildGroupEntity(): There are {} new aliases, now creating the Iterator", aliasSet.length());
                    Iterator aliasSetIterator = aliasSet.iterator();
                    while (aliasSetIterator.hasNext()) {
                        String newAlias = aliasSetIterator.next().toString();
                        LOG.trace("buildGroupEntity(): Adding Alias --> {}", newAlias);
                        Identifier additionalIdentifier = this.groupAttributeBuilders.buildGroupIdentifier(newAlias);
                        theTargetGroup.addIdentifier(additionalIdentifier);
                    }
                }
                break;
            case "m.room.member":
                LOG.trace("buildGroupEntity(): is a m.room.member event");
                Group.GroupMemberComponent newMembershipComponent = this.groupAttributeBuilders.buildMembershipComponent(roomEvent);
                theTargetGroup.addMember(newMembershipComponent);
                break;
            case "m.room.redaction":
                LOG.trace("buildGroupEntity(): is a m.room.redaction event");
                LOG.debug("buildGroupEntity(): Exit, if the event is a m.room.redaction, we ignore it (so returning null)!");
                return (null);
            case "m.room.power_levels":
                LOG.trace("buildGroupEntity(): is a m.room.power_levels event");
                LOG.debug("buildGroupEntity(): Exit, if the event is a m.room.power_levels, we ignore it (so returning null)!");
                return (null);
            default:
                LOG.trace("buildGroupEntity(): default for room event type, do nothing");
                LOG.debug("buildGroupEntity(): Exit, if the event is not of interested, we ignore it (so returning null)!");
                return (null);
        }
        LOG.debug(".buildDefaultGroupElement(): Created Identifier --> " + theTargetGroup.toString());
        return (theTargetGroup);
    }

}
