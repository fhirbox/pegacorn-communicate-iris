package net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.transformers.TransformErrorException;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirbox.pegacorn.communicate.iris.transformers.helpers.IdentifierBuilders;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import net.fhirbox.pegacorn.referencevalues.communication.PegacornCommunicateValueReferences;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.StringType;
import org.json.JSONException;

public class RoomCreate2CommunicationCreate {

    private static final Logger LOG = LoggerFactory.getLogger(RoomMessage2Communication.class);

    PegacornSystemReference pegacornSystemReference = new PegacornSystemReference();
    
    CommunicateProperties communicateProperties = new CommunicateProperties();

    @Inject
    IdentifierBuilders identifierBuilders;

    PegacornCommunicateValueReferences pegacornCommunicateValueReferences = new PegacornCommunicateValueReferences();

    public Bundle matrix2CommunicationBundle(String theMessage) throws TransformErrorException {
        Bundle newBundleElement = new Bundle();
        LOG.debug(".matrix2CommunicationBundle(): Message In --> " + theMessage);
        Communication communicationElement = new Communication();
        MessageHeader messageHeader = new MessageHeader();
        LOG.trace(".matrix2CommunicationBundle(): Message to be converted --> " + theMessage);
        try {
            communicationElement = roomCreateEvent2Communication(theMessage);
            messageHeader = matrix2MessageHeader(communicationElement, theMessage);
            newBundleElement.setType(Bundle.BundleType.MESSAGE);
            Bundle.BundleEntryComponent bundleEntryForMessageHeaderElement = new Bundle.BundleEntryComponent();
            bundleEntryForMessageHeaderElement.setResource(messageHeader);
            Bundle.BundleEntryComponent bundleEntryForCommunicationElement = new Bundle.BundleEntryComponent();
            Bundle.BundleEntryRequestComponent bundleRequest = new Bundle.BundleEntryRequestComponent();
            bundleRequest.setMethod(Bundle.HTTPVerb.POST);
            bundleRequest.setUrl("Communication");
            bundleEntryForCommunicationElement.setRequest(bundleRequest);
            newBundleElement.addEntry(bundleEntryForMessageHeaderElement);
            newBundleElement.addEntry(bundleEntryForCommunicationElement);
            newBundleElement.setTimestamp(new Date());
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
        MessageHeader.MessageSourceComponent messageSource = new MessageHeader.MessageSourceComponent();
        messageSource.setName("Pegacorn Matrix2FHIR Integration Service");
        messageSource.setSoftware("Pegacorn::Communicate::Iris");
        messageSource.setEndpoint(communicateProperties.getIrisEndPointForIncomingCommunicationBundle());
        return (messageHeaderElement);
    }

    public Communication roomCreateEvent2Communication(String theMessage) throws TransformErrorException {
        LOG.debug(".roomCreateEvent2Communication(): Entry, Message In --> " + theMessage);
        Communication communicationElement = new Communication();
        LOG.trace(".roomCreateEvent2Communication(): Message to be converted --> " + theMessage);
        try {
            JSONObject roomStatusEvent = new JSONObject(theMessage);
            communicationElement = buildAssociatedCommunicationEntity(roomStatusEvent);
        } catch (Exception Ex) {
            Communication emptyCommunication = new Communication();
            return (emptyCommunication);
        }
        return (communicationElement);
    }

    private Communication buildAssociatedCommunicationEntity(JSONObject pRoomServerEvent) {
        LOG.debug(".buildAssociatedCommunicationEntity() for Event --> " + pRoomServerEvent);
        LOG.trace(".buildAssociatedCommunicationEntity(): Create the empty FHIR::Communication entity.");
        Communication communicationEntity = new Communication();
        LOG.trace(".buildAssociatedCommunicationEntity(): Add the FHIR::Communication.Identifier (type = FHIR::Identifier) Set");
        communicationEntity.addIdentifier(this.buildAssociatedCommunicationIdentifier(pRoomServerEvent));
        LOG.trace(".buildAssociatedCommunicationEntity(): Set the FHIR::Communication.CommunicationStatus to COMPLETED (we don't chain)");
        communicationEntity.setStatus(Communication.CommunicationStatus.COMPLETED);
        LOG.trace(".buildAssociatedCommunicationEntity(): Set the FHIR::Communication.CommunicationPriority to ROUTINE (we make no distinction - all are real-time)");
        communicationEntity.setPriority(Communication.CommunicationPriority.ROUTINE);
        LOG.trace(".buildAssociatedCommunicationEntity(): Set the FHIR::COmmunication.Set to when the person sent the message");
        communicationEntity.setSent(new Date(pRoomServerEvent.getLong("origin_server_ts")));
        LOG.trace(".buildAssociatedCommunicationEntity(): Set the FHIR::Communication.Sender to the person who sent the message");
        Reference localCreatorReference = null;
        if (pRoomServerEvent.has("content")) {
            JSONObject localEventContent = pRoomServerEvent.getJSONObject("content");
            if (localEventContent.has("creator")) {
                LOG.trace(".buildAssociatedCommunicationEntity(): Creating a Sender reference");
                Identifier localCreatorIdentifier = identifierBuilders.buildPractitionerIdentifierFromSender(localEventContent.getString("creator"));
                localCreatorReference = new Reference();
                localCreatorReference.setIdentifier(localCreatorIdentifier);
                localCreatorReference.setType("Practitioner");
                localCreatorReference.setDisplay("Practitioner = " + localEventContent.getString("creator"));
                LOG.trace(".buildAssociatedCommunicationEntity(): Sender created --> " + localCreatorReference.toString());
            }
        }
        if (localCreatorReference != null) {
            communicationEntity.setSender(localCreatorReference);
            LOG.trace(".buildAssociatedCommunicationEntity(): Communication.Sender is set --> " + localCreatorReference.toString());
        }
        LOG.trace(".buildAssociatedCommunicationEntity(): Set the FHIR::Communication.Subject to the appropriate FHIR element");
        Reference associatedRoomReference = null;
        if (pRoomServerEvent.has("room_id")) {
            LOG.trace(".buildAssociatedCommunicationEntity(): room_id detected --> " + pRoomServerEvent.getString("room_id"));
            Identifier localCreatorIdentifier = this.buildGroupIdentifier(pRoomServerEvent);
            associatedRoomReference = new Reference();
            associatedRoomReference.setIdentifier(localCreatorIdentifier);
            associatedRoomReference.setType("Group");
            associatedRoomReference.setDisplay("Group = " + pRoomServerEvent.getString("room_id"));
            communicationEntity.setSubject(associatedRoomReference);
            LOG.trace(".buildAssociatedCommunicationEntity(): Communication.Subject is set --> " + associatedRoomReference.toString());
        }
        LOG.trace(".buildAssociatedCommunicationEntity(): Set the FHIR::Communication.Recepient to the appropriate Category (Set)");
        communicationEntity.setCategory(this.buildAssociatedCommunicationCategory(pRoomServerEvent));
        LOG.trace(".buildAssociatedCommunicationEntity(): Add EventAction to Extension");
        Extension eventActionExtension = new Extension();
        communicationEntity.addExtension(eventActionExtension);
        LOG.debug(".buildAssociatedCommunicationEntity(): Created Entity --> " + communicationEntity.toString());
        return (communicationEntity);
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
        LOG.debug("buildGroupIdentifier(): Entry");
        if ((pRoomEventMessage == null) || pRoomEventMessage.isEmpty()) {
            LOG.debug("buildGroupIdentifier(): Room Event Message is Empty");
            return (null);
        }
        LOG.trace("buildGroupIdentifier(): Event to be converted --> " + pRoomEventMessage);
        String localRoomID = pRoomEventMessage.getString("room_id");
        if (localRoomID.isEmpty()) {
            LOG.debug("buildGroupIdentifier(): Room ID from RoomEvent is Empty");
            return (null);
        }
        LOG.trace(".buildGroupIdentifier(): Room Identifier from Room Event --> " + localRoomID);
        Long localGroupAge;
        if (pRoomEventMessage.has("origin_server_ts")) {
            localGroupAge = pRoomEventMessage.getLong("origin_server_ts");
            LOG.trace(".buildGroupIdentifier(): Age (from Room Event) = " + localGroupAge);
        } else {
            localGroupAge = 0L;
            LOG.trace(".buildGroupIdentifier(): Age (default) = " + localGroupAge);
        }
        LOG.trace(".buildGroupIdentifier(): Create the FHIR::Identifier element via helper method ");
        Identifier localResourceIdentifier = this.identifierBuilders.buildGroupIdentifierFromRoomID(localRoomID, localGroupAge);
        LOG.debug(".buildGroupIdentifier(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localResourceIdentifier);
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
    private Identifier buildAssociatedCommunicationIdentifier(JSONObject pRoomServerEvent) {
        LOG.debug(".buildAssociatedCommunicationIdentifier(): for Event --> " + pRoomServerEvent.getString("event_id"));
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "OFFICIAL" (we are the source of truth for
        // this)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "event_id" from the RoomServer system
        localResourceIdentifier.setValue(pRoomServerEvent.getString("event_id"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(new Date(pRoomServerEvent.getLong("origin_server_ts")));
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have
        // not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        LOG.debug(".buildAssociatedCommunicationIdentifier(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localResourceIdentifier);
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
    private List<CodeableConcept> buildAssociatedCommunicationCategory(JSONObject pRoomMessageString) {
        LOG.debug(".buildAssociatedCommunicationCategory(): for Message --> " + pRoomMessageString);
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
        localMatrixCode.setCode(pRoomMessageString.getString("type"));
        // Set the FHIR::Coding.system to point to the Matrix standard(s)
        localMatrixCode.setSystem("https://matrix.org/docs/spec/client_server/");
        // Set the FHIR::Coding.system to reference the version of the Matrix standard
        // being used
        localMatrixCode.setVersion("0.6.0");
        // Set the FHIR::Coding.display to reflect the content type from the message
        localMatrixCode.setDisplay(pRoomMessageString.getString("type"));
        // Create an empty set of FHIR::Coding elements (again, this is what
        // CodeableConcept expects)
        List<Coding> localCodingList2 = new ArrayList<Coding>();
        // Add the FHIR::Coding to this 2nd Coding list
        localCodingList2.add(localMatrixCode);
        // Add the lost of Codings to he 2nd FHIR::CodeableConcept element
        localCanonicalCC2.setCoding(localCodingList2);
        // Add some useful text to display about the 2nd FHIR::CodeableConcept
        localCanonicalCC2.setText("Matrix.org: Event = " + pRoomMessageString.getString("type"));
        // Add the 1st Codeable Concept to the final List<CodeableConcept>
        localCommCatList.add(localCanonicalCC);
        // Add the 2nd Codeable Concept to the final List<CodeableConcept>
        localCommCatList.add(localCanonicalCC2);
        LOG.debug(".buildAssociatedCommunicationCategory(): LocalCommCatList (entry 0) --> " + localCommCatList.get(0).toString());
        LOG.debug(".buildAssociatedCommunicationCategory(): LocalCommCatList (entry 1) --> " + localCommCatList.get(1).toString());
        // Return the List<CodeableConcept>
        return (localCommCatList);
    }
}
