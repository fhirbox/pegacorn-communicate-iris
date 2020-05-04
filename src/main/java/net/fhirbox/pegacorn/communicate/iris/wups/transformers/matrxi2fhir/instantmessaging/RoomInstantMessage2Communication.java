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
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.instantmessaging;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.time.Instant;
import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.wups.common.MajorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.MatrixMessageException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.PayloadTransformationOutcomeEnum;
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

import net.fhirbox.pegacorn.communicate.iris.wups.common.MinorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.WrongContentTypeException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps.MatrixRoomID2ResourceReferenceMap;
import net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps.MatrxUserID2PractitionerIDMap;
import net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.common.IdentifierBuilders;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.petasos.model.TransientUoW;
import net.fhirbox.pegacorn.petasos.model.UoW;
import net.fhirbox.pegacorn.petasos.model.UoWProcessingOutcomeEnum;
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
 * The Reference for the FHIR::Communication.Subject and
 * FHIR::Communication.Recipient are extracted from a RoomID-ReferenceMap
 * maintained in the AppServers shared memory cache (see
 * MatrixRoomID2ResourceReferenceMap.java).
 * <p>
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
@ApplicationScoped
public class RoomInstantMessage2Communication
{

    private static final Logger LOG = LoggerFactory.getLogger(RoomInstantMessage2Communication.class);

    @Inject
    PegacornSystemReference pegacornSystemReference;

    @Inject
    CommunicateProperties communicateProperties;

    @Inject
    IdentifierBuilders identifierBuilders;

    /**
     *
     */
    @Inject
    protected MatrixRoomID2ResourceReferenceMap theRoom2ReferenceIDMap;

    /**
     *
     */
    @Inject
    protected MatrxUserID2PractitionerIDMap theUserID2PractitionerIDMap;

    @Inject
    private MatrixMessageContent2MediaReferenceSet mediaReferenceGenerator;

    @Inject
    private MatrixRoomID2GroupReference roomID2GroupReference;

    @Inject
    MatrixUserID2CommunicationSenderReference matrixUserID2SenderReferenceMapper;

    @Inject
    MatrixUserID2CommunicationRecipientReference matrixUserID2RecipientReferenceMapper;

    @Inject
    MatrixTextMessageContent2CommunicationPayload matrixTextContent2CommunicationPayloadMapper;

    /**
     *
     * @param theUoW
     * @return
     * @throws MatrixMessageException
     * @throws JSONException
     * @throws MajorTransformationException
     */
    public List<Bundle> convertMatrixInstantMessage2FHIRElements(String roomInstantMessage)
            throws MatrixMessageException, JSONException, MajorTransformationException
    {
        LOG.debug("convertMatrixInstantMessage2FHIRElements(): Entry, Matrix Room Instant Message --> {}", roomInstantMessage);
        if (roomInstantMessage == null) {
            throw (new MatrixMessageException("Matrix Room Instant Message --> is null"));
        }
        if (roomInstantMessage.isEmpty()) {
            throw (new MatrixMessageException("Matrix Room Instant Message --> is empty"));
        }
        // We need some helper functions to encode/decode the FHIR based message structures
        LOG.trace("wrapCommunicationBundle(): Initialising FHIR Resource Parser & Setting Pretty Print");
        FhirContext fhirContextHandle = FhirContext.forR4();
        IParser fhirResourceParser = fhirContextHandle.newJsonParser();
        // Now set up Iterator on the Ingres Content
        Communication newCommunication = matrix2Communication(roomInstantMessage);
        MessageHeader newMessageHeader = matrix2MessageHeader(newCommunication, roomInstantMessage);
        Bundle newCommunicationBundle = wrapCommunicationBundle(newMessageHeader, newCommunication);
//        String newCommunicationBundleAsString = fhirResourceParser.encodeResourceToString(newCommunicationBundle);
//        ArrayList<String> newOutputSet = new ArrayList<String>();
//        newOutputSet.add(newCommunicationBundleAsString);
        ArrayList<Bundle> newOutputSet = new ArrayList<Bundle>();
        newOutputSet.add(newCommunicationBundle);
        return (newOutputSet);
    }

    private Bundle wrapCommunicationBundle(MessageHeader newMH, Communication newComm)
            throws MatrixMessageException, JSONException, MajorTransformationException
    {
        LOG.debug("wrapCommunicationBundle(): Entry, FHIR::MessageHeader --> {}, FHIR::Communication --> {}", newMH, newComm);

        // Before we do anything, let's confirm the (superficial) integrity of the incoming objects
        LOG.trace("wrapCommunicationBundle(): Checking integrity of the incoming Matrix Instant Message");
        if (newMH == null) {
            LOG.error("wrapCommunicationBundle: MessageHeader resource is null!!!");
            throw (new MajorTransformationException("wrapCommunicationBundle: FHIR::MessageHeader resource is null"));
        }
        if (newComm == null) {
            LOG.error("wrapCommunicationBundle: Communication resource is null!!!");
            throw (new MajorTransformationException("wrapCommunicationBundle: FHIR::Communication resource is null"));
        }
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::Bundle and setting the FHIR::Bundle.BundleType");
        Bundle newBundleElement = new Bundle();
        newBundleElement.setType(Bundle.BundleType.MESSAGE);
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::BundleEntryComponent for FHIR::MessageHeader resource");
        BundleEntryComponent bundleEntryForMessageHeaderElement = new BundleEntryComponent();
        bundleEntryForMessageHeaderElement.setResource(newMH);
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::BundleEntryComponent for FHIR::Communication resource");
        BundleEntryComponent bundleEntryForCommunicationElement = new BundleEntryComponent();
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::BundleEntryRequestComponent for the FHIR::Communication resource");
        BundleEntryRequestComponent bundleRequest = new BundleEntryRequestComponent();
        bundleRequest.setMethod(Bundle.HTTPVerb.POST);
        bundleRequest.setUrl("Communication");
        bundleEntryForCommunicationElement.setRequest(bundleRequest);
        bundleEntryForCommunicationElement.setResource(newComm);
        LOG.trace("wrapCommunicationBundle(): Creating Adding the MessageHeader BundleEntryComponent & Communication BundleEntryComponent to the Bundle resource");
        newBundleElement.addEntry(bundleEntryForMessageHeaderElement);
        newBundleElement.addEntry(bundleEntryForCommunicationElement);
        newBundleElement.setTimestamp(new Date());
        LOG.debug("wrapCommunicationBundle(): Exit, Created FHIR::Bundle --> {}", newBundleElement);
        return (newBundleElement);
    }

    private MessageHeader matrix2MessageHeader(Communication theResultantCommunicationElement, String theMessage)
    {
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
     * @throws MinorTransformationException
     */
    private Communication matrix2Communication(String theMatrixRoomInstantMessage)
            throws MatrixMessageException, MajorTransformationException, JSONException
    {
        LOG.debug("matrix2Communication(): The incoming Matrix Instant Message is --> {}", theMatrixRoomInstantMessage);
        // The code wouldn't have got here if the Incoming Message was empty or null, so don't check again.
        LOG.trace("matrix2Communication(): Creating our two primary working objects, fhirCommunication (Communication) & matrixMessageObject (JSONObject)");
        Communication fhirCommunication;
        JSONObject matrixMessageObject = new JSONObject(theMatrixRoomInstantMessage);
        // So we now have a valid JSONObject for the incoming Matrix Instant Message
        LOG.trace("matrix2Communication(): Conversion of incoming Matrix Instant Message into JSONObject successful, now extracting Instant Message -content- field");
        if (!matrixMessageObject.has("content")) {
            LOG.error("matrix2Communication(): Exit, Matrix Room Instant Message (m.room.message) --> missing -content- field");
            throw (new MatrixMessageException("matrix2Communication(): Exit, Matrix Room Instant Message (m.room.message) --> missing -content- field"));
        }
        JSONObject messageContent = matrixMessageObject.getJSONObject("content");
        LOG.trace("matrix2Communication(): Extracted -content- field from Message Object, -content- --> {}", messageContent);
        // OK, now build messageDate --> using present instant if none provided TODO : perhaps we shouldn't use instant
        Date messageDate;
        if (matrixMessageObject.has("origin_server_ts")) {
            messageDate = new Date(matrixMessageObject.getLong("origin_server_ts"));
        } else {
            messageDate = Date.from(Instant.now());
        }
        // OK, so now we want to build the basic structure of the Communication object, which is common irrespective of Instant Message type
        LOG.trace("matrix2Communication(): Building the basic structure of the Communication object");
        fhirCommunication = buildDefaultCommunicationEntity(matrixMessageObject);
        LOG.trace("matrix2Communication(): Built default basic Communication object, now performing Swtich analysis for -content- type");
        switch (messageContent.getString("msgtype")) {
            case "m.audio": {
                LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.audio");
                List<CommunicationPayloadComponent> localPayloadList = new ArrayList<CommunicationPayloadComponent>();
                CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                try {
                    Reference newMediaReference = this.mediaReferenceGenerator.buildVideoReference(messageContent, messageDate);
                    localPayloadList.add(localPayload.setContent(newMediaReference));
                } catch (WrongContentTypeException | MinorTransformationException minorException) {
                    if (minorException instanceof WrongContentTypeException) {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.audio --> inject raw as data!");
                    } else {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.audio, error decoding --> inject raw as data!");
                    }
                    localPayloadList.add(createDumbTextPayload(messageContent.toString()));
                }
                fhirCommunication.setPayload(localPayloadList);
                break;
            }
            case "m.emote": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.emote");
                break;
            }
            case "m.file": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.file");
                break;
            }
            case "m.image": {
                LOG.trace(".matrix2Communication(): MMatrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.image");
                List<CommunicationPayloadComponent> localPayloadList = new ArrayList<CommunicationPayloadComponent>();
                CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                try {
                    Reference newMediaReference = this.mediaReferenceGenerator.buildVideoReference(messageContent, messageDate);
                    localPayloadList.add(localPayload.setContent(newMediaReference));
                } catch (WrongContentTypeException | MinorTransformationException minorException) {
                    if (minorException instanceof WrongContentTypeException) {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.image --> inject raw as data!");
                    } else {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.image, error decoding --> inject raw as data!");
                    }
                    localPayloadList.add(createDumbTextPayload(messageContent.toString()));
                }
                fhirCommunication.setPayload(localPayloadList);
                break;
            }
            case "m.location": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.location");
                break;
            }
            case "m.notice": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.notice");
                break;
            }
            case "m.server_notice": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.server_notice");
                break;
            }
            case "m.text": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.text");
                List<CommunicationPayloadComponent> localPayloadList = new ArrayList<CommunicationPayloadComponent>();
                try {
                    localPayloadList = this.matrixTextContent2CommunicationPayloadMapper.buildMTextPayload(messageContent);
                } catch (WrongContentTypeException | MinorTransformationException contentException) {
                    if (contentException instanceof WrongContentTypeException) {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.text --> inject raw as data!");
                    } else {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.video, error decoding --> inject raw as data!");
                    }
                    localPayloadList.add(createDumbTextPayload(messageContent.toString()));
                    fhirCommunication.setPayload(localPayloadList);
                }
                fhirCommunication.setPayload(localPayloadList);
                Reference referredToCommunicationEvent = this.buildInResponseTo(messageContent);
                if (referredToCommunicationEvent != null) {
                    fhirCommunication.addInResponseTo(referredToCommunicationEvent);
                }
                LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.text: Finished");
                break;
            }
            case "m.video": {
                LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.video");
                List<CommunicationPayloadComponent> localPayloadList = new ArrayList<CommunicationPayloadComponent>();
                CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                try {
                    Reference newMediaReference = this.mediaReferenceGenerator.buildVideoReference(messageContent, messageDate);
                    localPayloadList.add(localPayload.setContent(newMediaReference));
                } catch (WrongContentTypeException | MinorTransformationException minorException) {
                    if (minorException instanceof WrongContentTypeException) {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.video --> inject raw as data!!");
                    } else {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.video, error decoding --> inject raw as data!");
                    }
                    localPayloadList.add(createDumbTextPayload(messageContent.toString()));
                }
                fhirCommunication.setPayload(localPayloadList);
                break;
            }
            default: {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> unknown");
                throw (new MatrixMessageException("matrix2Communication(): Matrix Room Instant Message (m.room.message) --> Unknown Message Type"));
            }
        }
        LOG.debug(".matrix2Communication(): Created Communication Message --> {}", fhirCommunication);
        return (fhirCommunication);
    }

    private CommunicationPayloadComponent createDumbTextPayload(String textToAdd)
    {
        LOG.debug("createDumbTextPayload(): Entry, textToAdd --> {}", textToAdd);
        JSONObject normalPayload = new JSONObject();
        normalPayload.put("format", "org.fhirbox.pegacorn.custom.malformed_content");
        normalPayload.put("formatted_body", textToAdd);
        Communication.CommunicationPayloadComponent normalPayloadComponent = new Communication.CommunicationPayloadComponent();
        normalPayloadComponent.setContent(new StringType(normalPayload.toString()));
        return (normalPayloadComponent);
    }

    private Reference buildInResponseTo(JSONObject pRoomMessageContent)
    {
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
    private Communication buildDefaultCommunicationEntity(JSONObject roomMessage)
            throws MatrixMessageException, MajorTransformationException, JSONException
    {
        LOG.debug("buildDefaultCommunicationMessage(): Entry, Room Instant Message (m.room.message) --> {}", roomMessage);
        if (roomMessage == null) {
            LOG.error("buildDefaultCommunicationMessage(): Exit, Room Instant Message (m.room.message) --> pointer is null");
            throw (new MatrixMessageException("Room Instant Message (m.room.message) --> pointer is null"));
        }
        if (roomMessage.isEmpty()) {
            LOG.error("buildDefaultCommunicationMessage(): Exit, Room Instant Message (m.room.message) is empty");
            throw (new MatrixMessageException("Room Instant Message (m.room.message) --> message is empty"));
        }
        LOG.trace(".buildDefaultCommunicationMessage(): Add the FHIR::Communication.Identifier (type = FHIR::Identifier) Set");
        List<Identifier> communicationIdentifierSet = this.buildCommunicationIdentifier(roomMessage);
        if (communicationIdentifierSet.isEmpty()) {
            LOG.debug("buildDefaultCommunicationMessage(): Exit, Could not create an Identifier!!!");
            return (null);
        }
        LOG.trace(".buildDefaultCommunicationMessage(): Add Id value (from the m.room.message::event_id");
        if (!roomMessage.has("event_id")) {
            LOG.error("buildDefaultCommunicationMessage(): Exit, Room Instant Message (m.room.message) --> -event_id- is empty");
            throw (new MatrixMessageException("Room Instant Message (m.room.message) --> -event-id- is empty"));
        }
        Communication newCommunication = new Communication();
        newCommunication.setId(roomMessage.getString("event_id"));
//        LOG.trace(".buildDefaultCommunicationMessage(): Add narrative of Communication Entity");
//        Narrative elementNarrative = new Narrative();
//        elementNarrative.setStatus(Narrative.NarrativeStatus.GENERATED);
//        XhtmlNode elementDiv = new XhtmlNode();
//        elementDiv.addDocType("xmlns=\\\"http://www.w3.org/1999/xhtml\"");
//        elementDiv.addText("<p> A message generate on the Pegacorn::Communicate::RoomServer platform </p>");
//        LOG.trace("buildDefaultCommunicationMessage(): Adding Narrative, content added --> {}", elementDiv.getContent());
//        elementNarrative.setDiv(elementDiv);
//        newCommunication.setText(elementNarrative);
        LOG.trace("buildDefaultCommunicationMessage(): Set the FHIR::Communication.CommunicationStatus to COMPLETED (we don't chain, yet)");
        // TODO : Add chaining in Communication entities.
        newCommunication.setStatus(Communication.CommunicationStatus.COMPLETED);
        LOG.trace("buildDefaultCommunicationMessage(): Set the FHIR::Communication.CommunicationPriority to ROUTINE (we make no distinction - all are real-time)");
        newCommunication.setPriority(Communication.CommunicationPriority.ROUTINE);
        LOG.trace("buildDefaultCommunicationMessage(): Set the FHIR::COmmunication.Set to when the person sent the message");
        Date sentDate;
        if (roomMessage.has("origin_server_ts")) {
            sentDate = new Date(roomMessage.getLong("origin_server_ts"));
        } else {
            sentDate = Date.from(Instant.now());
        }
        newCommunication.setSent(sentDate);
        LOG.trace("buildDefaultCommunicationMessage(): Set the FHIR::Communication.Sender to the person who sent the message");
        if (roomMessage.has("sender")) {
            String sender = roomMessage.getString("sender");
            Reference senderRef = this.matrixUserID2SenderReferenceMapper.buildPractitionerIDAsSenderReference(sender);
            newCommunication.setSender(senderRef);
        }
        LOG.info(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Subject to the appropriate FHIR element");

        newCommunication.setSubject(this.buildSubjectReference(roomMessage));
        LOG.info(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Recipient to the appropriate FHIR element (normally only one)");
        newCommunication.setRecipient(this.matrixUserID2RecipientReferenceMapper.buildRecipientReferenceSet(roomMessage));
        LOG.info(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Recepient to the appropriate Category (Set)");
        newCommunication.setCategory(this.buildCommunicationCategory(roomMessage));
        LOG.debug(".buildDefaultCommunicationMessage(): Created Identifier --> {}", newCommunication);
        return (newCommunication);
    }

    // TODO: fix javadoc for buildCommunicationIdentifier()
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
    private List<Identifier> buildCommunicationIdentifier(JSONObject roomMessage)
            throws MatrixMessageException, JSONException
    {
        LOG.debug("buildCommunicationIdentifier(): Entry, Room Instant Message (m.room.message) --> {}", roomMessage);
        if (roomMessage == null) {
            LOG.error("buildCommunicationIdentifier(): Exit, Room Instant Message (m.room.message) --> pointer is null");
            throw (new MatrixMessageException("Room Instant Message (m.room.message) --> pointer is null"));
        }
        if (roomMessage.isEmpty()) {
            LOG.debug("buildCommunicationIdentifier(): Exit, Room Instant Message (m.room.message) --> message is empty");
            throw (new MatrixMessageException("Room Instant Message (m.room.message) --> message is empty"));
        }
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "OFFICIAL" (we are the source of truth for this)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "event_id" from the RoomServer system
        if (!roomMessage.has("event_id")) {
            LOG.error("buildCommunicationIdentifier(): Exit, Room Instant Message (m.room.message) --> message does not contain an entity_id");
            throw (new MatrixMessageException("Room Instant Message (m.room.message) --> message does not contain an entity_id"));
        }
        localResourceIdentifier.setValue(roomMessage.getString("event_id"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        Date messageDate;
        if (roomMessage.has("origin_server_ts")) {
            messageDate = new Date(roomMessage.getLong("origin_server_ts"));
        } else {
            messageDate = Date.from(Instant.now());
        }
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have
        // not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        LOG.trace("buildCommunicationIdentifier(): Created Identifier --> {}", localResourceIdentifier);
        ArrayList<Identifier> identifierList = new ArrayList<Identifier>();
        identifierList.add(localResourceIdentifier);
        LOG.debug("buildCommunicationIdentifier(): Exit, Created Identifier --> {} ", localResourceIdentifier);
        return (identifierList);
    }

    /**
     * This method constructs a FHIR::Reference entity for the Subject of the
     * message based on the RoomServer.RoomID (i.e. "room_id").
     * <p>
     * There is only a single Subject (which may be a FHIR::Group).
     * <p>
     * The method extracts the RoomMessage.RoomID (i.e. "room_id") and attempts
     * to find the corresponding FHIR::Reference in the
     * MatrixRoomID2ResourceReferenceMap cache map.
     * <p>
     * The resulting single FHIR::Reference is then returned. If no Reference is
     * found, then an new (non-conanical) one is created that points to a
     * FHIR::Group.
     *
     * @param pRoomServerMessage A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return The FHIR::Reference for the subject (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    private Reference buildSubjectReference(JSONObject roomIM)
            throws MatrixMessageException, JSONException
    {
        LOG.info("buildSubjectReference(): Entry, for Matrix Room Instant Message --> {}", roomIM);
        // For now, we are assuming it is a "FHIR::Group"
        if (!roomIM.has("room_id")) {
            throw (new MatrixMessageException("Matrix Room Instant Message --> has not -room_id-"));
        }
        Reference subjectReference = this.roomID2GroupReference.buildGroupReferenceFromRoomID(roomIM);
        LOG.info(".buildSubjectReference(): Created Reference --> {}", subjectReference);
        return (subjectReference);
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
    private List<CodeableConcept> buildCommunicationCategory(JSONObject pRoomMessageString)
    {
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
}
