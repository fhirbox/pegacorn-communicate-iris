/* 
 * Copyright 2020 Mark A. Hunter (ACT Health).
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
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import org.json.JSONObject;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Communication.CommunicationPayloadComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.MessageHeader.MessageSourceComponent;

import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import net.fhirbox.pegacorn.communicate.iris.wups.common.TransformErrorException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps.MatrixRoomID2ResourceReferenceMap;
import net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps.MatrxUserID2PractitionerIDMap;
import net.fhirbox.pegacorn.communicate.iris.wups.common.helpers.IdentifierBuilders;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;


/**
 * <h1>Transform Room Based Message to a FHIR::Communication Resource</h1>
 * <p>
 * This class is used to perform the transformation of a Room Message
 * (encapsulated as a Matrix(R) room_message_event and convert it to a
 * FHIR::Communication resource.
 * <p>
 * To do this, the code needs to construct the apropriate references (for the
 * FHIR::Communication.Identifier of the message, the
 * FHIR::Communication.Sender, the FHIR::Communication.Recipient and the
 * FHIR::Communication.Subject) - it then needs to extract the content type and
 * encapsulate it into the FHIR::Communication.payload attribute.
 * <p>
 The Reference for the FHIR::Communication.Subject and
 FHIR::Communication.Recipient are extracted from a RoomID-ReferenceMap
 maintained in the AppServers shared memory cache (see
 MatrixRoomID2ResourceReferenceMap.java).
 <p>
 * <b> Note: </b> If the content within the message is video ("m.video"), audio
 * ("m.audio") or image ("m.image") the a discrete FHIR::Media resource is
 * created and the FHIR::Communication.payload attribute is set to point (via a
 * FHIR::Reference) to the FHIR::Media element.
 *
 * <b> Note: </b> the following configuration details need to be loaded into the
 * Wildfly Application Server configuration file (standalone-ha.xml) {@code
 * <cache-container name="pegacorn-communicate" default-cache=
 * "general" module="org.wildfly.clustering.server">
 * <transport lock-timeout="15000" />
 * <replicated-cache name="general">
 * <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
 * </replicated-cache>
 * <replicated-cache name="room2resource_id_map">
 * <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
 * </replicated-cache>
 * <replicated-cache name="user2practitioner_id_map">
 * <transaction locking="OPTIMISTIC" mode="FULL_XA"/>
 * </replicated-cache>>
 * </cache-container>}
 *
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020-01-20
 *
 */
public class RoomMessage2Communication {

    private static final Logger LOG = LoggerFactory.getLogger(RoomMessage2Communication.class);

    PegacornSystemReference pegacornSystemReference = new PegacornSystemReference();
    CommunicateProperties communicateProperties = new CommunicateProperties();
    

    @Inject
    IdentifierBuilders identifierBuilders;
    @Inject
    protected MatrixRoomID2ResourceReferenceMap theRoom2ReferenceIDMap;
    @Inject
    protected MatrxUserID2PractitionerIDMap theUserID2PractitionerIDMap;

    public Bundle matrix2CommunicationBundle(String theMessage) throws TransformErrorException {
        Bundle newBundleElement = new Bundle();
        LOG.debug(".matrix2CommunicationBundle(): Message In --> " + theMessage);
        FhirContext fhirContextHandle = FhirContext.forR4();
        IParser fhirResourceParser = fhirContextHandle.newJsonParser();
        fhirResourceParser.setPrettyPrint(true);
        Communication communicationElement = new Communication();
        MessageHeader messageHeader = new MessageHeader();
        LOG.trace(".matrix2CommunicationBundle(): Message to be converted --> " + theMessage);
        try {
            communicationElement = matrix2Communication(theMessage);
            messageHeader = matrix2MessageHeader(communicationElement, theMessage);
            newBundleElement.setType(Bundle.BundleType.MESSAGE);
            BundleEntryComponent bundleEntryForMessageHeaderElement = new BundleEntryComponent();
            bundleEntryForMessageHeaderElement.setResource(messageHeader);
            BundleEntryComponent bundleEntryForCommunicationElement = new BundleEntryComponent();
            BundleEntryRequestComponent bundleRequest = new BundleEntryRequestComponent();
            bundleRequest.setMethod(Bundle.HTTPVerb.POST);
            bundleRequest.setUrl("Communication");
            bundleEntryForCommunicationElement.setRequest(bundleRequest);
            bundleEntryForCommunicationElement.setResource(communicationElement);
            newBundleElement.addEntry(bundleEntryForMessageHeaderElement);
            newBundleElement.addEntry(bundleEntryForCommunicationElement);
            newBundleElement.setTimestamp(new Date());
            LOG.trace(".matrix2CommunicationBundle(): Created Bundle --> " + fhirResourceParser.encodeResourceToString(newBundleElement));
            return (newBundleElement);
        } catch (JSONException jsonExtractionError) {
            throw (new TransformErrorException("matrix2CommunicationBundle(): Bad JSON Message Structure -> ", jsonExtractionError));
        }
    }
    
    

    public MessageHeader matrix2MessageHeader(Communication theResultantCommunicationElement, String theMessage) {
        MessageHeader messageHeaderElement = new MessageHeader();
        Coding messageHeaderCode = new Coding();
        messageHeaderCode.setSystem("http://pegacorn.fhirbox.net/pegacorn/R1/message-codes");
        messageHeaderCode.setCode("communication-bundle");
        messageHeaderElement.setEvent(messageHeaderCode);
        MessageSourceComponent messageSource = new MessageSourceComponent();
        messageSource.setName("Pegacorn Matrix2FHIR Integration Service");
        messageSource.setSoftware("Pegacorn::Communicate::Iris");
        messageSource.setEndpoint(communicateProperties.getIrisEndPointForIncomingCommunicationBundle());
        return (messageHeaderElement);
    }

    /**
     * The method is the primary (exposed) method for performing the entity
     * transformation. It incorporates a switch statement to derive the nature
     * of the "payload" transformation (re-encapsulation) to be performed.
     *
     * @param theMessage A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return Communication A FHIR::Communication resource (see
     * https://www.hl7.org/fhir/communication.html)
     * @throws TransformErrorException
     */
    public Communication matrix2Communication(String theMessage) throws TransformErrorException {
        LOG.debug(".matrix2Communication(): Message In --> " + theMessage);
        Communication localCommunicationEvent = new Communication();
        LOG.trace(".matrix2Communication(): Message to be converted --> " + theMessage);
        try {
            JSONObject localMessageEvent = new JSONObject(theMessage);
            LOG.trace(".matrix2Communication(): Extracted Message from Events" + localMessageEvent.toString(2));
            JSONObject localMessageContent = localMessageEvent.getJSONObject("content");
            LOG.trace(".matrix2Communication(): Extracted Content from Message" + localMessageContent.toString(2));
            localCommunicationEvent = buildDefaultCommunicationEntity(localMessageEvent);
            LOG.trace(".matrix2Communication(): Built default Communication entity");
            switch (localMessageContent.getString("msgtype")) {
                case "m.audio": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> m.audio");
                    List<CommunicationPayloadComponent> localPayloadList = new ArrayList<CommunicationPayloadComponent>();
                    CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                    List<Reference> localMediaEntityReferences = this.buildMediaReference(localMessageEvent);
                    for (Integer localCounter = 0; localCounter < localMediaEntityReferences.size(); localCounter += 1) {
                        localPayloadList.add(localPayload.setContent(localMediaEntityReferences.get(localCounter)));
                    }
                    localCommunicationEvent.setPayload(localPayloadList);
                    break;
                }
                case "m.emote": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> m.emote");
                    break;
                }
                case "m.file": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> m.file");
                    break;
                }
                case "m.image": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype --> m.image");
                    List<CommunicationPayloadComponent> localPayloadList = new ArrayList<CommunicationPayloadComponent>();
                    CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                    List<Reference> localMediaEntityReferences = this.buildMediaReference(localMessageEvent);
                    for (Integer localCounter = 0; localCounter < localMediaEntityReferences.size(); localCounter += 1) {
                        localPayloadList.add(localPayload.setContent(localMediaEntityReferences.get(localCounter)));
                    }
                    localCommunicationEvent.setPayload(localPayloadList);
                    break;
                }
                case "m.location": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> m.location");
                    break;
                }
                case "m.notice": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> m.notice");
                    break;
                }
                case "m.server_notice": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> m.server_notice");
                    break;
                }
                case "m.text": {
                    LOG.trace(".matrix2Communication(): Case --> Message Type (msgtype) == m.text: Start");
                    List<CommunicationPayloadComponent> localPayloadList = this.buildMTextPayload(localMessageContent);
                    localCommunicationEvent.setPayload(localPayloadList);
                    Reference referredToCommunicationEvent = this.buildInResponseTo(localMessageContent);
                    if (referredToCommunicationEvent != null) {
                        localCommunicationEvent.addInResponseTo(referredToCommunicationEvent);
                    }
                    LOG.trace(".matrix2Communication(): Case --> Message Type (msgtype) == m.text: Finished");
                    break;
                }
                case "m.video": {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> m.video");
                    List<CommunicationPayloadComponent> localPayloadList = new ArrayList<CommunicationPayloadComponent>();
                    CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                    List<Reference> localMediaEntityReferences = this.buildMediaReference(localMessageEvent);
                    for (Integer localCounter = 0; localCounter < localMediaEntityReferences.size(); localCounter += 1) {
                        localPayloadList.add(localPayload.setContent(localMediaEntityReferences.get(localCounter)));
                    }
                    localCommunicationEvent.setPayload(localPayloadList);
                    break;
                }
                default: {
                    LOG.trace(".matrix2Communication(): Message Type (msgtype) --> unknown");
                    throw (new TransformErrorException("Unknown Message Type"));
                }
            }
            LOG.debug(".matrix2Communication(): Created Communication Message --> " + localCommunicationEvent.toString());
            return (localCommunicationEvent);
        } catch (JSONException jsonExtractionError) {
            throw (new TransformErrorException("matrix2Communication(): Bad JSON Message Structure -> ", jsonExtractionError));
        }
    }

    private List<CommunicationPayloadComponent> buildMTextPayload(JSONObject pRoomMessageContent) {
        LOG.debug("buildMTextPayload(): Entry, pRoomMessage.content --> " + pRoomMessageContent.toString());
        ArrayList<CommunicationPayloadComponent> payLoadList = new ArrayList<CommunicationPayloadComponent>();
        JSONObject normalPayload = new JSONObject();
        JSONObject formattedPayload = new JSONObject();
        if (pRoomMessageContent.has("format")) {
            LOG.trace("buildMTextPayload(): format == " + pRoomMessageContent.getString("format"));
            CharSequence formatType = "org.matrix.custom.html";
            if (pRoomMessageContent.getString("format").contains(formatType)) {
                LOG.trace("buildMTextPayload(): Creating a new Formated Payload Component");
                formattedPayload.put("format", pRoomMessageContent.get("format"));
                formattedPayload.put("formatted_body", pRoomMessageContent.getString("formatted_body"));
                CommunicationPayloadComponent formattedPayloadComponent = new CommunicationPayloadComponent();
                formattedPayloadComponent.setContent(new StringType(formattedPayload.toString()));
                payLoadList.add(formattedPayloadComponent);
            }
        }
        normalPayload.put("format", "text");
        normalPayload.put("formatted_body", pRoomMessageContent.getString("body"));
        CommunicationPayloadComponent normalPayloadComponent = new CommunicationPayloadComponent();
        normalPayloadComponent.setContent(new StringType(normalPayload.toString()));
        payLoadList.add(normalPayloadComponent);
        LOG.debug("buildMTextPayload(): Exit, Number of PayLoadComponents = " + payLoadList.size());
        return (payLoadList);
    }

    private Reference buildInResponseTo(JSONObject pRoomMessageContent) {
        LOG.debug(".buildInResponseTo(): Entry, for Event --> " + pRoomMessageContent.toString());
        if (!(pRoomMessageContent.has("m.relates_to"))) {
            return (null);
        }
        JSONObject referredToMessageContent = pRoomMessageContent.getJSONObject("m.relates_to");
        if (!(referredToMessageContent.has("m.in_reply_to"))) {
            return (null);
        }
        JSONObject referredToMessage = referredToMessageContent.getJSONObject("m.in_reply_to");
        if (!(referredToMessage.has("event_id"))) {
            return (null);
        }
        Reference referredCommunicationMessage = new Reference();
        LOG.trace(".buildInResponseTo(): Create the empty FHIR::Identifier element");
        Identifier localResourceIdentifier = new Identifier();
        LOG.trace(".buildInResponseTo(): Set the FHIR::Identifier.Use to -OFFICIAL- (we are the source of truth for this)");
        localResourceIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        LOG.trace(".buildInResponseTo(): Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)");
        localResourceIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        LOG.trace(".buildInResponseTo(): Set the FHIR::Identifier.Value to the -event_id- from the RoomServer system" + referredToMessage.getString("event_id"));
        localResourceIdentifier.setValue(referredToMessage.getString("event_id"));
        LOG.trace(".buildInResponseTo(): Identifier added to Reference --> " + localResourceIdentifier.toString());
        referredCommunicationMessage.setIdentifier(localResourceIdentifier);
        LOG.trace(".buildInResponseTo(): Add type to the Reference");
        referredCommunicationMessage.setType("Communication");
        LOG.debug(".buildInResponseTo(): Exit, created Reference --> " + referredCommunicationMessage.toString());
        return (referredCommunicationMessage);
    }

    /**
     * This method constructs a basic FHIR::Communication entity and then calls
     * a the other methods within this class to populate the relevant
     * attributes.
     *
     * @param pMessageObject A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return Communication A FHIR::Communication resource (see
     * https://www.hl7.org/fhir/communication.html)
     */
    private Communication buildDefaultCommunicationEntity(JSONObject pMessageObject) {
        LOG.debug(".buildDefaultCommunicationMessage(): for Event --> " + pMessageObject);
        LOG.trace(".buildDefaultCommunicationMessage(): Create the empty FHIR::Communication entity");
        Communication localComMsg = new Communication();
        LOG.trace(".buildDefaultCommunicationMessage(): Add the FHIR::Communication.Identifier (type = FHIR::Identifier) Set");
        localComMsg.addIdentifier(this.buildCommunicationIdentifier(pMessageObject));
        LOG.trace(".buildDefaultCommunicationMessage(): Add Id value (from the m.room.message::event_id");
        localComMsg.setId(pMessageObject.getString("event_id"));
        LOG.trace(".buildDefaultCommunicationMessage(): Add narrative of Communication Entity");
        Narrative elementNarrative = new Narrative();
        elementNarrative.setStatus(Narrative.NarrativeStatus.GENERATED);
        XhtmlNode elementDiv = new XhtmlNode();
//        elementDiv.addDocType("xmlns=\\\"http://www.w3.org/1999/xhtml\"");
//        elementDiv.addText("<p> A message generate on the Pegacorn::Communicate::RoomServer platform </p>");
//        LOG.trace(".buildDefaultCommunicationMessage(): Narrative added --> " + elementDiv.getContent());
//        elementNarrative.setDiv(elementDiv);
//        localComMsg.setText(elementNarrative);
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.CommunicationStatus to COMPLETED (we don't chain)");
        localComMsg.setStatus(Communication.CommunicationStatus.COMPLETED);
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.CommunicationPriority to ROUTINE (we make no distinction - all are real-time)");
        localComMsg.setPriority(Communication.CommunicationPriority.ROUTINE);
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::COmmunication.Set to when the person sent the message");
        localComMsg.setSent(new Date(pMessageObject.getLong("origin_server_ts")));
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Sender to the person who sent the message");
        localComMsg.setSender(this.buildSenderReference(pMessageObject));
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Subject to the appropriate FHIR element");
        localComMsg.setSubject(this.buildSubjectReference(pMessageObject));
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Recipient to the appropriate FHIR element (normally only one)");
        localComMsg.setRecipient(this.buildRecipientReferenceSet(pMessageObject));
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Recepient to the appropriate Category (Set)");
        localComMsg.setCategory(this.buildCommunicationCategory(pMessageObject));
        LOG.debug(".buildDefaultCommunicationMessage(): Created Identifier --> " + localComMsg.toString());
        return (localComMsg);
    }

    /**
     * This method constructs a basic FHIR::Identifier entity for the given
     * message. Typically, there is only one FHIR::Identifier for the message
     * within the Pegacorn system. The source system's message identifier will
     * always be used as an identifier value - with the appropriate System set
     * for the Codeable concept.
     *
     * @param pRoomServerMessage A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return Identifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/datatypes.html#Identifier)
     */
    private Identifier buildCommunicationIdentifier(JSONObject pRoomServerMessage) {
        LOG.debug(".buildCommunicationIdentifier(): for Event --> " + pRoomServerMessage.getString("event_id"));
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "OFFICIAL" (we are the source of truth for
        // this)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "event_id" from the RoomServer system
        localResourceIdentifier.setValue(pRoomServerMessage.getString("event_id"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(new Date(pRoomServerMessage.getLong("origin_server_ts")));
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have
        // not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        LOG.debug(".buildCommunicationIdentifier(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localResourceIdentifier);
    }

    /**
     * This method constructs a basic FHIR::Reference for the Sender of the
     * message. It does so by extracting - from the
     * UserID2PractitionerReferenceMap cache map - the FHIR::Identifier for the
     * given RoomServer.UserID embedded in the MessageObject. If no Identifier
     * is found, then a non-canonical one is constructed.
     *
     * @param pRoomServerMessage A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return Reference The FHIR::Reference for the Sender (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    private Reference buildSenderReference(JSONObject pRoomServerMessage) {
        LOG.debug(new StringBuilder().append(".buildSenderReference(): for Sender RoomServer User ID --> ")
                .append(pRoomServerMessage.getString("sender")).toString());
        if (pRoomServerMessage.getString("sender") == null) {
            LOG.debug("buildSenderReference(): No Sender found original message, returning null");
            return (null);
        }
        // Create the empty FHIR::Reference element
        Reference localSenderReference = new Reference();
        // Get the associated Practitioner::Identifier from the RoomServer.UserID
        // (sender)
        Identifier localSenderIdentifier = this.theUserID2PractitionerIDMap.getPractitionerIDFromUserName(pRoomServerMessage.getString("sender"));
        // If there is no associated Practitioner::Identifier, create one.
        if (localSenderIdentifier == null) {
            LOG.trace("buildSenderReference(): No Mapped Identifier, creating temporary one");
            // Create an empty FHIR::Identifier element
            localSenderIdentifier = this.identifierBuilders.buildPractitionerIdentifierFromSender(pRoomServerMessage.getString("sender"));
            LOG.trace("buildSenderReference(): Created Identifier --> " + localSenderIdentifier.toString());
            // Add the FHIR::Identifier to the FHIR::Reference.Identifier
            localSenderReference.setIdentifier(localSenderIdentifier);
            // Set the FHIR::Reference.type to "Practitioner" (we only receive events from
            // Practitioners - for now)
            localSenderReference.setType("Practitioner");
        } else {
            LOG.trace("buildSenderReference(): Mapped Identifier exists, adding it to Reference");
            LOG.trace("buildSenderReference(): Mapped Identifier --> " + localSenderIdentifier.toString());
            // Add the FHIR::Identifier to the FHIR::Reference.Identifier
            localSenderReference.setIdentifier(localSenderIdentifier);
            // Set the FHIR::Reference.type to "Practitioner" (we only receive events from
            // Practitioners - for now)
            localSenderReference.setType("Practitioner");
        }
        LOG.debug(".buildSenderReference(): Created Reference --> " + localSenderReference.toString());
        return (localSenderReference);
    }

    /**
     * This method constructs a set of FHIR::Reference entities for the
     * Recipients of the message based on the RoomServer.RoomID (i.e.
     * "room_id").
     * <p>
     * In this release, only a single Recipient is expected and managed.
     * <p>
 The method extracts the RoomServer.RoomID from the RoomServer.RoomMessage
 and attempts to find the corresponding FHIR::Reference in the
 MatrixRoomID2ResourceReferenceMap cache map.
 <p>
     * The resulting single FHIR::Reference is then added to a
     * List<FHIR::Reference>
     * and returned. If no Reference is found, then an empty set is returned.
     *
     * @param pRoomServerMessage A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return Reference The List<FHIR::Reference> for the Recipients (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    private List<Reference> buildRecipientReferenceSet(JSONObject pRoomServerMessage) {
        LOG.debug(".buildRecipientReference(): for Event --> " + pRoomServerMessage.getString("room_id"));
        // Create the empty List<Reference> set
        List<Reference> localRecipientReferenceSet = new ArrayList<Reference>();
        // Get the associated Reference from the RoomServer.RoomID ("room_id")
        Reference localRecipientReference = theRoom2ReferenceIDMap.getFHIRResourceReferenceFromRoomID(pRoomServerMessage.getString("room_id"));
        // If there are no References associated to the RoomServer.RoomID, return an
        // empty set.
        if (localRecipientReference == null) {
            LOG.debug("buildRecipientReferenceSet(): No mapped Room-to-FHIR::Resource, returning null");
            return (localRecipientReferenceSet);
        }
        // Add the FHIR::Reference to the Recipient Reference Set (there is only one).
        localRecipientReferenceSet.add(localRecipientReference);
        LOG.debug(".buildRecipientReferenceSet(): Created Reference --> " + localRecipientReference.toString());
        return (localRecipientReferenceSet);
    }

    /**
     * This method constructs a FHIR::Reference entity for the Subject of the
     * message based on the RoomServer.RoomID (i.e. "room_id").
     * <p>
     * There is only a single Subject (which may be a FHIR::Group).
     * <p>
 The method extracts the RoomMessage.RoomID (i.e. "room_id") and attempts
 to find the corresponding FHIR::Reference in the
 MatrixRoomID2ResourceReferenceMap cache map.
 <p>
     * The resulting single FHIR::Reference is then returned. If no Reference is
     * found, then an new (non-conanical) one is created that points to a
     * FHIR::Group.
     *
     * @param pRoomServerMessage A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return The FHIR::Reference for the subject (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    private Reference buildSubjectReference(JSONObject pRoomServerMessage) {
        LOG.debug(".buildSubjectReference(): for Room ID --> " + pRoomServerMessage.getString("room_id"));
        // Get the associated Reference from the RoomServer.RoomID ("room_id")
        Reference localSubjectReference = theRoom2ReferenceIDMap.getFHIRResourceReferenceFromRoomID(pRoomServerMessage.getString("room_id"));
        // If there is a References associated to the RoomServer.RoomID, return it.
        if (localSubjectReference != null) {
            LOG.debug("buildSubjectReference(): Mapped Reference Found --> " + localSubjectReference.toString());
            return (localSubjectReference);
        }
        // One didn't exist, so we'll create one and it will map to a FHIR::Group
        // Create the empty FHIR::Reference element
        localSubjectReference = new Reference();
        // Create an empty FHIR::Identifier element
        Identifier localSubjectIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
        localSubjectIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localSubjectIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "sender" from the RoomServer system
        localSubjectIdentifier.setValue(pRoomServerMessage.getString("room_id"));
        // Add the FHIR::Identifier to the FHIR::Reference.Identifier
        localSubjectReference.setIdentifier(localSubjectIdentifier);
        // Set the FHIR::Reference.type to "Group"
        localSubjectReference.setType("Group");
        LOG.debug(".buildSubjectReference(): Created Reference --> " + localSubjectReference.toString());
        return (localSubjectReference);
    }

    /**
     * This method constructs a FHIR::CodeableConcept to describe the "Category"
     * of the new FHIR::Communication element being constructed from the
     * RoomServer message.
     * <p>
     * One of the CodeableConcept elements will refer to a default HL7(R)
     * communication-category set to "notification". see the following link
     * (http://terminology.hl7.org/CodeSystem/communication-category) for
     * alternatives.
     * <p>
     * The 2nd CodeableConcept element will map to the "msgtype" extracted from
     * the actual RoomServer message itself and will may to a coding system of
     * "https://matrix.org/docs/spec/client_server/).
     *
     * @param pRoomMessageString A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return A list of FHIR::CodeableConcept elements (see
     * https://www.hl7.org/fhir/datatypes.html#CodeableConcept)
     */
    private List<CodeableConcept> buildCommunicationCategory(JSONObject pRoomMessageString) {
        LOG.debug(".buildCommunicationCategory(): for Message --> " + pRoomMessageString);
        // Create an empty list of CodeableConcept elements
        List<CodeableConcept> localCommCatList = new ArrayList<CodeableConcept>();
        // Create the first CodeableConcept (to capture HL7 based category)
        CodeableConcept localCanonicalCC = new CodeableConcept();
        // Create a FHIR::Coding for the CodeableConcept
        Coding localCanonicalCode = new Coding();
        // Set the FHIR::Coding.code to "notification" (from the standards)
        localCanonicalCode.setCode("notification");
        // Set the FHIR::Coding.system that we obtained the code from
        localCanonicalCode.setSystem("http://terminology.hl7.org/CodeSystem/communication-category");
        // Set the FHIR::Coding.version associated with the code we've used
        localCanonicalCode.setVersion("4.0.1");
        // Set a display name (FHIR::Coding.display) - make it nice!
        localCanonicalCode.setDisplay("Notification");
        // Create an empty set of FHIR::Coding elements (this is what CodeableConcept
        // expects)
        List<Coding> localCodingList1 = new ArrayList<Coding>();
        // Add the FHIR::Coding above to the List
        localCodingList1.add(localCanonicalCode);
        // Add the list of Codings to the first FHIR::CodeableConcept
        localCanonicalCC.setCoding(localCodingList1);
        // Add some useful text to display about the FHIR::CodeableConcept
        localCanonicalCC.setText("HL7: Communication Category = Notification ");
        // Create the 2nd CodeableConcept (to capture Pegacorn/Matrix based category)
        CodeableConcept localCanonicalCC2 = new CodeableConcept();
        // Create the 1st FHIR::Coding for the 2nd CodeableConcept
        Coding localMatrixCode = new Coding();
        // Set the FHIR::Coding.code to the (Matrix) content type (msgtype) in the
        // message
        JSONObject localMessageContentType = pRoomMessageString.getJSONObject("content");
        localMatrixCode.setCode(localMessageContentType.getString("msgtype"));
        // Set the FHIR::Coding.system to point to the Matrix standard(s)
        localMatrixCode.setSystem("https://matrix.org/docs/spec/client_server/");
        // Set the FHIR::Coding.system to reference the version of the Matrix standard
        // being used
        localMatrixCode.setVersion("0.6.0");
        // Set the FHIR::Coding.display to reflect the content type from the message
        localMatrixCode.setDisplay(localMessageContentType.getString("msgtype"));
        // Create an empty set of FHIR::Coding elements (again, this is what
        // CodeableConcept expects)
        List<Coding> localCodingList2 = new ArrayList<Coding>();
        // Add the FHIR::Coding to this 2nd Coding list
        localCodingList2.add(localMatrixCode);
        // Add the lost of Codings to he 2nd FHIR::CodeableConcept element
        localCanonicalCC2.setCoding(localCodingList2);
        // Add some useful text to display about the 2nd FHIR::CodeableConcept
        localCanonicalCC2.setText("Matrix.org: Event = " + localMessageContentType.getString("msgtype"));
        // Add the 1st Codeable Concept to the final List<CodeableConcept>
        localCommCatList.add(localCanonicalCC);
        // Add the 2nd Codeable Concept to the final List<CodeableConcept>
        localCommCatList.add(localCanonicalCC2);
        LOG.debug(".buildSubjectReference(): LocalCommCatList (entry 0) --> " + localCommCatList.get(0).toString());
        LOG.debug(".buildSubjectReference(): LocalCommCatList (entry 1) --> " + localCommCatList.get(1).toString());
        // Return the List<CodeableConcept>
        return (localCommCatList);
    }

    private List<Reference> buildMediaReference(JSONObject pRoomMessage) {
        LOG.debug(".buildDefaultMediaEntity(): for Message --> " + pRoomMessage.toString());
        RoomMessage2MediaReferenceSet localMediaReferenceSetGen = new RoomMessage2MediaReferenceSet(pRoomMessage);
        List<Reference> localMediaEntityReference = localMediaReferenceSetGen.getReferenceSet();
        LOG.debug(".buildDefaultMediaEntity(): new Media Entity --> " + localMediaEntityReference.toString());
        return (localMediaEntityReference);
    }
}
