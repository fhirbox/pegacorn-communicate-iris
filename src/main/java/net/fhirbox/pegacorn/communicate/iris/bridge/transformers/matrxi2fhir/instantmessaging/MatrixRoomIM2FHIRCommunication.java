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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging;

import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders.MatrixUserID2FHIRPractitionerReference;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders.MatrixRoomIMTextMessageContent2FHIRCommunicationPayload;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders.MatrixRoomID2FHIRGroupReference;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders.MatrixRoomIMMediaContent2FHIRMediaReferenceSet;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.time.Instant;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MajorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MatrixMessageException;
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
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.MessageHeader.MessageSourceComponent;

import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MinorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.WrongContentTypeException;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixRoomID2ResourceReferenceMap;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixUserID2PractitionerIDMap;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common.MatrixAttribute2FHIRIdentifierBuilders;
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
public class MatrixRoomIM2FHIRCommunication
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomIM2FHIRCommunication.class);

    @Inject
    PegacornSystemReference pegacornSystemReference;

    @Inject
    CommunicateProperties communicateProperties;

    @Inject
    MatrixAttribute2FHIRIdentifierBuilders identifierBuilders;

    @Inject
    private MatrixRoomIMMediaContent2FHIRMediaReferenceSet mediaReferenceGenerator;

    @Inject
    private MatrixRoomID2FHIRGroupReference roomID2GroupReference;

    @Inject
    MatrixUserID2FHIRPractitionerReference matrixUserID2PractitionerReferenceMapper;

    @Inject
    MatrixRoomIMTextMessageContent2FHIRCommunicationPayload matrixTextContent2CommunicationPayloadMapper;

    @Inject
    MatrixRoomIM2FHIRCommunicationSkeleton communicationSkeletonFactory;

    /**
     *
     * This function takes an incoming Matrix Room Instant Message
     * (Matrix::m.room.message) and converts it to a FHIR::Bundle containing a
     * FHIR::MessageHeader and a FHIR::Communication element set.
     * <p>
     * Where the message is originating from a Matrix::User, the function
     * looks-up the PractitionerID2MatrixName Cachemap to ascertain if there is
     * any pre-existing mapping between a Matrix User and a FHIR::Practitioner.
     * If not, it creates a temporary FHIR::Identifier (Use = TEMP). This
     * FHIR::Identifier is then used to construct the FHIR::Reference for the
     * FHIR::Communication.Sender field.
     * <p>
     * Similarly, depending on the Matrix::Room (Group) that the message is
     * associated with, the FHIR::Communication.Subject field is populated with
     * a FHIR::Reference associated with a FHIR::CareTeam, FHIR::Group,
     * FHIR::Organisation, FHIR::PractitionerRole or FHIR::Patient.
     * <p>
     * If the message (Matrix::m.room.message) is part of a peer-to-peer
     * dialogue, then the FHIR::Communication.Target is populated with the
     * appropriate FHIR::Practitioner or FHIR::PractitionerRole. Again, the
     * FHIR::Identifier for these may be queried from the
     * PractitionerID2MatrixName map.
     *
     * @param roomInstantMessage The incoming Matrix Room Instant Message
     * @return A List of FHIR::Bundle element, each comprising -->
     * FHIR::MessageHeader, FHIR::Communication
     * @throws MatrixMessageException
     * @throws JSONException
     * @throws MajorTransformationException
     * 
     * @see 
     * <a href="https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields">Matrix Client-Server API Specificaton, Release 0.6.0 - "room_instant_message" Message</a> <p>
     * <a href="https://www.hl7.org/fhir/bundle.html">FHIR Specification, Release 4.0.1, "Bundle" Resource</a>
     *
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
        MessageHeader newMessageHeader = buildDefaultMessageHeader();
        Bundle newCommunicationBundle = buildCommunicationBundle(newMessageHeader, newCommunication);
        ArrayList<Bundle> newOutputSet = new ArrayList<>();
        newOutputSet.add(newCommunicationBundle);
        return (newOutputSet);
    }

    /**
     *
     * This function wraps FHIR::MessageHeader and FHIR::Communication element
     * set into a FHIR::Bundle.
     * <p>
     * Note that The bundles are always of type POST for FHIR::Communication
     * based FHIR::Bundles.
     *
     * @param newMessageHeader The FHIR::MessageHeader associated with the
     * FHIR::Communication element
     * @param newCommunication The FHIR::Communication message generated as part
     * of the transformation
     * @return Bundle: A singular FHIR::Bundle made up of the
     * FHIR::MessageHeader and FHIR::Communication elements.
     */
    private Bundle buildCommunicationBundle(MessageHeader newMessageHeader, Communication newCommunication)
            throws MatrixMessageException, JSONException, MajorTransformationException
    {
        LOG.debug("wrapCommunicationBundle(): Entry, FHIR::MessageHeader --> {}, FHIR::Communication --> {}", newMessageHeader, newCommunication);

        // Before we do anything, let's confirm the (superficial) integrity of the incoming objects
        LOG.trace("wrapCommunicationBundle(): Checking integrity of the incoming Matrix Instant Message");
        if (newMessageHeader == null) {
            LOG.error("wrapCommunicationBundle: MessageHeader resource is null!!!");
            throw (new MajorTransformationException("wrapCommunicationBundle: FHIR::MessageHeader resource is null"));
        }
        if (newCommunication == null) {
            LOG.error("wrapCommunicationBundle: Communication resource is null!!!");
            throw (new MajorTransformationException("wrapCommunicationBundle: FHIR::Communication resource is null"));
        }
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::Bundle and setting the FHIR::Bundle.BundleType");
        Bundle newBundleElement = new Bundle();
        newBundleElement.setType(Bundle.BundleType.MESSAGE);
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::BundleEntryComponent for FHIR::MessageHeader resource");
        BundleEntryComponent bundleEntryForMessageHeaderElement = new BundleEntryComponent();
        bundleEntryForMessageHeaderElement.setResource(newMessageHeader);
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::BundleEntryComponent for FHIR::Communication resource");
        BundleEntryComponent bundleEntryForCommunicationElement = new BundleEntryComponent();
        LOG.trace("wrapCommunicationBundle(): Creating FHIR::BundleEntryRequestComponent for the FHIR::Communication resource");
        BundleEntryRequestComponent bundleRequest = new BundleEntryRequestComponent();
        bundleRequest.setMethod(Bundle.HTTPVerb.POST);
        bundleRequest.setUrl("Communication");
        bundleEntryForCommunicationElement.setRequest(bundleRequest);
        bundleEntryForCommunicationElement.setResource(newCommunication);
        LOG.trace("wrapCommunicationBundle(): Creating Adding the MessageHeader BundleEntryComponent & Communication BundleEntryComponent to the Bundle resource");
        newBundleElement.addEntry(bundleEntryForMessageHeaderElement);
        newBundleElement.addEntry(bundleEntryForCommunicationElement);
        newBundleElement.setTimestamp(new Date());
        LOG.debug("wrapCommunicationBundle(): Exit, Created FHIR::Bundle --> {}", newBundleElement);
        return (newBundleElement);
    }

    /**
     *
     * This function generates a (default) FHIR::MessageHeader (default for use
     * with our FHIR::Communication elements).
     *
     * @return MessageHeader - A FHIR::MessageHeader element suitable for
     * inclusion in a FHIR::Bundle for encapsulating and transporting a
     * transformed Matrix::m.room.message (in a FHIR::Communication element).
     *
     */
    private MessageHeader buildDefaultMessageHeader()
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
     * The method is the primary method for performing the entity
     * transformation. It incorporates a switch statement to derive the nature
     * of the "payload" transformation (re-encapsulation) to be performed.
     *
     * @param theMatrixRoomInstantMessage A Matrix::m.room.message (see
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
        fhirCommunication = communicationSkeletonFactory.buildDefaultCommunicationEntity(matrixMessageObject);
        ArrayList<CommunicationPayloadComponent> localPayloadList = new ArrayList<>();
        LOG.trace("matrix2Communication(): Built default basic Communication object, now performing Swtich analysis for -content- type");
        switch (messageContent.getString("msgtype")) {
            case "m.audio": {
                LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.audio");
                CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                try {
                    Reference newMediaReference = this.mediaReferenceGenerator.buildVideoReference(messageContent, messageDate);
                    localPayloadList.add(localPayload.setContent(newMediaReference));
                } catch (WrongContentTypeException | MinorTransformationException minorException) {
                    if (minorException instanceof WrongContentTypeException) {
                        LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.audio --> not creating a FHIR::Media reference!");
                    } else {
                        LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.audio, error decoding --> not creating a FHIR::Media reference!");
                    }
                }
                break;
            }
            case "m.emote": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.emote");
                break;
            }
            case "m.file": {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.file");
                CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                try {
                    Reference newMediaReference = this.mediaReferenceGenerator.buildFileReference(messageContent, messageDate);
                    localPayloadList.add(localPayload.setContent(newMediaReference));
                } catch (WrongContentTypeException | MinorTransformationException minorException) {
                    if (minorException instanceof WrongContentTypeException) {
                        LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.file --> not creating a FHIR::Media reference!");
                    } else {
                        LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.file, error decoding --> not creating a FHIR::Media reference!");
                    }
                }
                break;
            }
            case "m.image": {
                LOG.trace(".matrix2Communication(): MMatrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.image");
                CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                try {
                    Reference newMediaReference = this.mediaReferenceGenerator.buildVideoReference(messageContent, messageDate);
                    localPayloadList.add(localPayload.setContent(newMediaReference));
                } catch (WrongContentTypeException | MinorTransformationException minorException) {
                    if (minorException instanceof WrongContentTypeException) {
                        LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.image --> not creating a FHIR::Media reference!");
                    } else {
                        LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.image, error decoding --> not creating a FHIR::Media reference!");
                    }
                }
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
                LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.text: Finished");
                break;
            }
            case "m.video": {
                LOG.trace("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.video");
                CommunicationPayloadComponent localPayload = new CommunicationPayloadComponent();
                try {
                    Reference newMediaReference = this.mediaReferenceGenerator.buildVideoReference(messageContent, messageDate);
                    localPayloadList.add(localPayload.setContent(newMediaReference));
                } catch (WrongContentTypeException | MinorTransformationException minorException) {
                    if (minorException instanceof WrongContentTypeException) {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> thought it was m.video --> not creating a FHIR::Media reference!!");
                    } else {
                        LOG.debug("matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> m.video, error decoding --> not creating a FHIR::Media reference!");
                    }
                }
                break;
            }
            default: {
                LOG.trace(".matrix2Communication(): Matrix Room Instant Message (m.room.message), -content-, -msgtype- --> unknown");
                throw (new MatrixMessageException("matrix2Communication(): Matrix Room Instant Message (m.room.message) --> Unknown Message Type"));
            }
        }

        CommunicationPayloadComponent messageContentAsTextPayload = this.matrixTextContent2CommunicationPayloadMapper.buildTextPayload(messageContent);
        localPayloadList.add(messageContentAsTextPayload);
        fhirCommunication.setPayload(localPayloadList);
        Reference referredToCommunicationEvent = this.buildInResponseTo(messageContent);
        if (referredToCommunicationEvent != null) {
            fhirCommunication.addInResponseTo(referredToCommunicationEvent);
        }
        LOG.debug(".matrix2Communication(): Created Communication Message --> {}", fhirCommunication);
        return (fhirCommunication);
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
        LOG.debug("buildSubjectReference(): Entry, for Matrix Room Instant Message --> {}", roomIM);
        // For now, we are assuming it is a "FHIR::Group"
        if (!roomIM.has("room_id")) {
            throw (new MatrixMessageException("Matrix Room Instant Message --> has not -room_id-"));
        }
        try {
            Reference subjectReference = this.roomID2GroupReference.buildFHIRGroupReferenceFromMatrixRoomID(roomIM.getString("room_id"), true);
            LOG.debug(".buildSubjectReference(): Exit, Created FHIR::Group Reference --> {}", subjectReference);
            return (subjectReference);
        } catch (MinorTransformationException transformException) {
            LOG.debug(".buildSubjectReference(): Exit, Could not create FHIR::Group Reference, room_id is null, returning null");
            return (null);
        }
    }

}
