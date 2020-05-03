/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.instantmessaging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.wups.common.MatrixMessageException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.PayloadTransformationOutcomeEnum;
import net.fhirbox.pegacorn.communicate.iris.wups.common.TransformErrorException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.WrongContentTypeException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps.MatrixRoomID2ResourceReferenceMap;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mhunter
 */
@ApplicationScoped
public class MatrixRoomID2GroupReference
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomID2GroupReference.class);

    @Inject
    protected MatrixRoomID2ResourceReferenceMap theRoom2ReferenceIDMap;

    @Inject
    PegacornSystemReference pegacornSystemReference;

    @Inject
    CommunicateProperties communicateProperties;

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
     * @param roomIM A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return The FHIR::Reference for the subject (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    public Reference buildGroupReferenceFromRoomID(JSONObject roomIM)
            throws MatrixMessageException, TransformErrorException, WrongContentTypeException, JSONException
    {
        LOG.debug("buildSubjectReference(): Entry, for Matrix Room Instant Message --> {}", roomIM);
        // Get the associated Reference from the RoomServer.RoomID ("room_id")
        if (roomIM == null) {
            throw (new MatrixMessageException("Room Instant Message --> whole message is null"));
        }
        if (roomIM.isEmpty()) {
            LOG.error("buildSubjectReference(): Exit, Instant Message Payload is empty");
            throw (new MatrixMessageException("Room Instant Message --> it is empty"));
        }
        if (!roomIM.has("room_id")) {
            throw (new MatrixMessageException("Room Instant Message --> Missing -room_id-"));
        }
        String roomId = roomIM.getString("room_id");
        Reference localSubjectReference = theRoom2ReferenceIDMap.getFHIRResourceReferenceFromRoomID(roomId);
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
        localSubjectIdentifier.setValue(roomIM.getString("room_id"));
        // Add the FHIR::Identifier to the FHIR::Reference.Identifier
        localSubjectReference.setIdentifier(localSubjectIdentifier);
        // Set the FHIR::Reference.type to "Group"
        localSubjectReference.setType("Group");
        LOG.debug(".buildSubjectReference(): Created Reference --> " + localSubjectReference.toString());
        return (localSubjectReference);
    }
}
