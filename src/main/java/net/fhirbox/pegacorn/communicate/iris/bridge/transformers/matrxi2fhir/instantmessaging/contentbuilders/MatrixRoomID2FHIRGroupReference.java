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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MatrixMessageException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.PayloadTransformationOutcomeEnum;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MinorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.WrongContentTypeException;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixRoomID2ResourceReferenceMap;
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
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class MatrixRoomID2FHIRGroupReference
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomID2FHIRGroupReference.class);

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
    public Reference buildFHIRGroupReferenceFromMatrixRoomID(String roomID, boolean createIfNotExist)
            throws MinorTransformationException
    {
        LOG.debug("buildFHIRGroupReferenceFromMatrixRoomID(): Entry, for Matrix Room Instant Message --> {}", roomID);
        // Get the associated Reference from the RoomServer.RoomID ("room_id")
        if (roomID == null) {
            throw (new MinorTransformationException("Exit, Matrix Room ID is null"));
        }
        if (roomID.isEmpty()) {
            LOG.error("buildFHIRGroupReferenceFromMatrixRoomID(): Exit, Matrix Room ID is empty");
            throw (new MinorTransformationException("Room Instant Message --> it is empty"));
        }
        // One didn't exist, so we'll create one and it will map to a FHIR::Group
        // Create the empty FHIR::Reference element
        Reference localSubjectReference = new Reference();
        // Create an empty FHIR::Identifier element
        Identifier localSubjectIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "SECONDARY" (this id is not guaranteed)
        localSubjectIdentifier.setUse(Identifier.IdentifierUse.SECONDARY);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localSubjectIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "sender" from the RoomServer system
        localSubjectIdentifier.setValue(roomID);
        // Add the FHIR::Identifier to the FHIR::Reference.Identifier
        localSubjectReference.setIdentifier(localSubjectIdentifier);
        // Set the FHIR::Reference.type to "Group"
        localSubjectReference.setType("Group");
        LOG.debug(".buildFHIRGroupReferenceFromMatrixRoomID(): Created new (Temporary) FHIR::Reference for the FHIR::Group --> " + localSubjectReference.toString());
        return (localSubjectReference);
    }
}
