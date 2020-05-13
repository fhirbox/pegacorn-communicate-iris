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

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MinorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixUserID2PractitionerIDMap;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common.MatrixAttribute2FHIRIdentifierBuilders;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark Hunter (ACT Health)
 * @since 2020-04-01
 *
 */
@ApplicationScoped
public class MatrixUserID2FHIRPractitionerReference
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixUserID2FHIRPractitionerReference.class);

    @Inject
    protected PegacornSystemReference pegacornSystemReference;

    @Inject
    protected CommunicateProperties communicateProperties;

    @Inject
    protected MatrixUserID2PractitionerIDMap theUserID2PractitionerIDMap;

    /**
     * This method constructs a set of FHIR::Reference entities for the
     * Recipients of the message based on the RoomServer.RoomID (i.e.
     * "room_id").
     * <p>
     * In this release, only a single Recipient is expected and managed.
     * <p>
     * The method extracts the RoomServer.RoomID from the RoomServer.RoomMessage
     * and attempts to find the corresponding FHIR::Reference in the
     * MatrixRoomID2ResourceReferenceMap cache map.
     * <p>
     * The resulting single FHIR::Reference is then added to a
     * List<FHIR::Reference>
     * and returned. If no Reference is found, then an empty set is returned.
     *
     * @param pRoomServerMessage A Matrix(R) "m.room.message" message (see
     * https://matrix.org/docs/spec/client_server/r0.6.0#room-event-fields)
     * @return Reference The List<FHIR::Reference> for the Recipients (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    public Reference buildFHIRPractitionerReferenceFromMatrixUserID(String matrixUserID, boolean createIfNotExist)
            throws MinorTransformationException
    {
        LOG.debug("buildFHIRPractitionerReferenceFromMatrixUserID(): Entry, for Matrix User ID --> {}", matrixUserID);
        // Get the associated Reference from the RoomServer.RoomID ("room_id")
        if (matrixUserID.isEmpty()) {
            LOG.error("buildFHIRPractitionerReferenceFromMatrixUserID(): Matrix User ID missing");
            throw (new MinorTransformationException("buildFHIRPractitionerReferenceFromMatrixUserID(): Matrix User ID missing"));
        }
        LOG.trace("buildFHIRPractitionerReferenceFromMatrixUserID: attempting to retrieve Practitioner Identifier from ID Map" );
        Identifier practitionerId = theUserID2PractitionerIDMap.getPractitionerIDFromUserName(matrixUserID);
        LOG.trace("buildFHIRPractitionerReferenceFromMatrixUserID: retrieved Practitioner Identifier --> {}", practitionerId);
        // If there are no References associated to the RoomServer.RoomID, return an
        // empty set.
        if ((practitionerId == null) & !createIfNotExist) {
            LOG.debug("buildFHIRPractitionerReferenceFromMatrixUserID(): No mapped Matrix User ID <-> FHIR Practitioner Identifier, return null");
            return (null);
        }
        if (practitionerId == null) {
            LOG.trace("buildFHIRPractitionerReferenceFromMatrixUserID(): No Mapped Identifier, creating temporary one");
            practitionerId = new Identifier();
            // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
            practitionerId.setUse(Identifier.IdentifierUse.TEMP);
            // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
            practitionerId.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
            // Set the FHIR::Identifier.Value to the "sender" from the RoomServer system
            practitionerId.setValue(matrixUserID);
            LOG.trace("buildSenderReference(): Created Identifier --> {}" + practitionerId);
        }
        LOG.trace("buildFHIRPractitionerReferenceFromMatrixUserID(): Creatig a Reference");
        // Create the empty FHIR::Reference element
        Reference newPractitionerReference = new Reference();
        // Add the FHIR::Identifier to the FHIR::Reference.Identifier
        newPractitionerReference.setIdentifier(practitionerId);
        // Set the FHIR::Reference.type to "Group"
        newPractitionerReference.setType("Practitioner");
        LOG.debug(".buildFHIRPractitionerReferenceFromMatrixUserID(): Exit, Created FHIR Practitioner Reference --> {}", newPractitionerReference);
        return (newPractitionerReference);
    }
}
