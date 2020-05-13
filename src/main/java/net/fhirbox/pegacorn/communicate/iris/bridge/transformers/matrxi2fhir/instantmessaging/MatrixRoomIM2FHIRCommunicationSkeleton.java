/*
 * The MIT License
 *
 * Copyright 2020 ACT Health.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders.MatrixRoomID2FHIRGroupReference;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders.MatrixUserID2FHIRPractitionerReference;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MajorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MatrixMessageException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MinorTransformationException;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ACT Health
 */
@ApplicationScoped
public class MatrixRoomIM2FHIRCommunicationSkeleton
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomIM2FHIRCommunicationSkeleton.class);

    @Inject
    protected PegacornSystemReference pegacornSystemReference;

    @Inject
    protected CommunicateProperties communicateProperties;

    @Inject
    protected MatrixRoomID2FHIRGroupReference roomID2GroupReference;

    @Inject
    protected MatrixUserID2FHIRPractitionerReference matrixUserID2PractitionerReferenceMapper;

    /**
     * This class encapsulates a set of methods that create the default (skeleton)
     * Communicate resource - based on a preliminary set of information
     * defined within the incoming "m.room.message".
     *
     * @param roomMessage (JSONObject) The Matrix(R) "m.room.message" message <p> 
     * @return A FHIR::Communication resource 
     *
     * @throws MatrixMessageException
     * @throws MajorTransformationException
     * @throws JSONException
     * 
     * @see 
     * <a href="https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields">Matrix Client-Server API Specificaton, Release 0.6.0 - "room_instant_message" Message</a> <p>
     * <a href="https://www.hl7.org/fhir/communication.html">FHIR Specification, Release 4.0.1, "Communicaton" Resource</a>
     */
    public Communication buildDefaultCommunicationEntity(JSONObject roomMessage)
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
        Identifier communicationIdentifier = this.buildCommunicationIdentifier(roomMessage);
        if (communicationIdentifier == null) {
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
        LOG.trace(".buildDefaultCommunicationMessage(): Add narrative of Communication Entity");
        Narrative communicationResourceNarrative = new Narrative();
        communicationResourceNarrative.setStatus(Narrative.NarrativeStatus.GENERATED);
        XhtmlNode elementDiv = new XhtmlNode();
        elementDiv.addDocType("xmlns=\\\"http://www.w3.org/1999/xhtml\"");
        elementDiv.addText("<p> A message generate on the Pegacorn::Communicate::RoomServer platform </p>");
        LOG.trace("buildDefaultCommunicationMessage(): Adding Narrative, content added --> {}", elementDiv.getContent());
        communicationResourceNarrative.setDiv(elementDiv);
        newCommunication.setText(communicationResourceNarrative);
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
            Reference senderRef = null;
            try {
                senderRef = this.matrixUserID2PractitionerReferenceMapper.buildFHIRPractitionerReferenceFromMatrixUserID(sender, true);
            } catch (MinorTransformationException transformException) {
                LOG.trace("buildDefaultCommunicationMessage(): No Communication Sender Reference created");
            }
            if (senderRef != null) {
                newCommunication.setSender(senderRef);
            }
        }
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Subject to the appropriate FHIR element");
        Reference newGroupReference;
        try {
            newGroupReference = this.roomID2GroupReference.buildFHIRGroupReferenceFromMatrixRoomID(roomMessage.getString("room_id"), true);
            newCommunication.setSubject(newGroupReference);
        } catch (MinorTransformationException minorException) {

        }
        LOG.trace(".buildDefaultCommunicationMessage(): Set the FHIR::Communication.Recepient to the appropriate Category (Set)");
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
     * @param roomMessage A Matrix(R) "m.room.message" message 
     * 
     * @return Identifier A FHIR::Identifier resource 
     * 
     * @see 
     * <a href="https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields">Matrix Client-Server API Specificaton, Release 0.6.0 - "room_instant_message" Message</a> <p>
     * <a href="https://www.hl7.org/fhir/datatypes.html#Identifier">FHIR Specification, Release 4.0.1, "Identifier" Resource</a>
     */
    private Identifier buildCommunicationIdentifier(JSONObject roomMessage)
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
        Identifier newCommunicationIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (Ladon needs to analyse it before its "OFFICIAL")
        newCommunicationIdentifier.setUse(Identifier.IdentifierUse.SECONDARY);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        newCommunicationIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "event_id" from the RoomServer system
        if (!roomMessage.has("event_id")) {
            LOG.error("buildCommunicationIdentifier(): Exit, Room Instant Message (m.room.message) --> message does not contain an entity_id");
            throw (new MatrixMessageException("Room Instant Message (m.room.message) --> message does not contain an entity_id"));
        }
        newCommunicationIdentifier.setValue(roomMessage.getString("event_id"));
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
        newCommunicationIdentifier.setPeriod(lEventIDPeriod);
        LOG.debug("buildCommunicationIdentifier(): Created Identifier --> {}", newCommunicationIdentifier);
        return (newCommunicationIdentifier);
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
     * @param roomMessage A Matrix(R) "m.room.message" message 
     * @return A list of FHIR::CodeableConcept elements 
     * 
     * @see 
     * <a href="https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields">Matrix Client-Server API Specificaton, Release 0.6.0 - "room_instant_message" Message</a> <p>
     * <a href="https://www.hl7.org/fhir/datatypes.html#CodeableConcept">FHIR Specification, Release 4.0.1, "CodeableConcept" Resource</a>
     */
    private List<CodeableConcept> buildCommunicationCategory(JSONObject roomMessage)
    {
        LOG.debug(".buildCommunicationCategory(): for Message --> " + roomMessage);
        // Create an empty list of CodeableConcept elements
        List<CodeableConcept> newCommunicationCategoryList = new ArrayList<>();
        // Create the first CodeableConcept (to capture HL7 based category)
        CodeableConcept pegacornCanonicalCodeableConcept = new CodeableConcept();
        // Create a FHIR::Coding for the CodeableConcept
        Coding pegacornCanonicalCode = new Coding();
        // Set the FHIR::Coding.code to "notification" (from the standards)
        pegacornCanonicalCode.setCode("notification");
        // Set the FHIR::Coding.system that we obtained the code from
        pegacornCanonicalCode.setSystem("http://terminology.hl7.org/CodeSystem/communication-category");
        // Set the FHIR::Coding.version associated with the code we've used
        pegacornCanonicalCode.setVersion("4.0.1"); // TODO - this needs to be a DeploymentVariable
        // Set a display name (FHIR::Coding.display) - make it nice!
        pegacornCanonicalCode.setDisplay("Notification");
        // Create an empty set of FHIR::Coding elements (this is what CodeableConcept
        // expects)
        List<Coding> localCodingList1 = new ArrayList<>();
        // Add the FHIR::Coding above to the List
        localCodingList1.add(pegacornCanonicalCode);
        // Add the list of Codings to the first FHIR::CodeableConcept
        pegacornCanonicalCodeableConcept.setCoding(localCodingList1);
        // Add some useful text to display about the FHIR::CodeableConcept
        pegacornCanonicalCodeableConcept.setText("HL7: Communication Category = Notification ");
        // Create the 2nd CodeableConcept (to capture Pegacorn/Matrix based category)
        CodeableConcept matrixBasedCadeableConcept = new CodeableConcept();
        // Create the 1st FHIR::Coding for the 2nd CodeableConcept
        Coding matrixBasedCode = new Coding();
        // Set the FHIR::Coding.code to the (Matrix) content type (msgtype) in the
        // message
        JSONObject localMessageContentType = roomMessage.getJSONObject("content");
        matrixBasedCode.setCode("Matrix::m.room.message::" + localMessageContentType.getString("msgtype"));
        // Set the FHIR::Coding.system to point to the Matrix standard(s)
        matrixBasedCode.setSystem("https://matrix.org/docs/spec/client_server/r0.6.0");
        // Set the FHIR::Coding.system to reference the version of the Matrix standard
        // being used
        matrixBasedCode.setVersion("0.6.0");
        // Set the FHIR::Coding.display to reflect the content type from the message
        matrixBasedCode.setDisplay("Matrix.org: Room Instant Message --> Matrix::m.room.message::" + localMessageContentType.getString("msgtype"));
        // Create an empty set of FHIR::Coding elements (again, this is what
        // CodeableConcept expects)
        List<Coding> localCodingList2 = new ArrayList<>();
        // Add the FHIR::Coding to this 2nd Coding list
        localCodingList2.add(matrixBasedCode);
        // Add the lost of Codings to he 2nd FHIR::CodeableConcept element
        matrixBasedCadeableConcept.setCoding(localCodingList2);
        // Add some useful text to display about the 2nd FHIR::CodeableConcept
        matrixBasedCadeableConcept.setText("Matrix::m.room.message::" + localMessageContentType.getString("msgtype"));
        // Add the 1st Codeable Concept to the final List<CodeableConcept>
        newCommunicationCategoryList.add(pegacornCanonicalCodeableConcept);
        // Add the 2nd Codeable Concept to the final List<CodeableConcept>
        newCommunicationCategoryList.add(matrixBasedCadeableConcept);
        LOG.debug(".buildSubjectReference(): LocalCommCatList (entry 0) --> " + newCommunicationCategoryList.get(0).toString());
        LOG.debug(".buildSubjectReference(): LocalCommCatList (entry 1) --> " + newCommunicationCategoryList.get(1).toString());
        // Return the List<CodeableConcept>
        return (newCommunicationCategoryList);
    }
}
