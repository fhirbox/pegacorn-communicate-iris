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
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.instantmessaging;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.wups.common.PayloadTransformationOutcomeEnum;
import net.fhirbox.pegacorn.communicate.iris.wups.common.TransformErrorException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps.MatrixRoomID2ResourceReferenceMap;
import net.fhirbox.pegacorn.communicate.iris.wups.common.cachedmaps.MatrxUserID2PractitionerIDMap;
import net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.common.IdentifierBuilders;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mhunter
 */
@ApplicationScoped
public class MatrixUserID2CommunicationSenderReference {

    private static final Logger LOG = LoggerFactory.getLogger(MatrixUserID2CommunicationSenderReference.class);

    @Inject
    protected MatrixRoomID2ResourceReferenceMap theRoom2ReferenceIDMap;

    @Inject
    PegacornSystemReference pegacornSystemReference;

    @Inject
    CommunicateProperties communicateProperties;

    @Inject
    protected MatrxUserID2PractitionerIDMap theUserID2PractitionerIDMap;

    @Inject
    IdentifierBuilders identifierBuilders;

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
    public Reference buildPractitionerIDAsSenderReference(String matrixUserID) throws TransformErrorException{
        LOG.debug("buildSenderReference(): Entry, Sender Matrix User ID --> {} ", matrixUserID);
        if (matrixUserID == null) {
            LOG.error("buildSenderReference(): Exit, No Sender is null");
            throw( new TransformErrorException(PayloadTransformationOutcomeEnum.PAYLOAD_TRANSFORM_FAILURE.getPayloadTransformationOutcome()));
        }
        if (matrixUserID.isEmpty()) {
            LOG.error("buildSenderReference(): Exit, the Sender is empty");
            throw( new TransformErrorException(PayloadTransformationOutcomeEnum.PAYLOAD_TRANSFORM_FAILURE.getPayloadTransformationOutcome()));
        }
        // Get the associated Practitioner::Identifier from the RoomServer.UserID (sender)
        Identifier localSenderIdentifier = this.theUserID2PractitionerIDMap.getPractitionerIDFromUserName(matrixUserID);
        Reference senderRef = new Reference();
        // If there is no associated Practitioner::Identifier, create one.
        if (localSenderIdentifier == null) {
            LOG.trace("buildSenderReference(): No Mapped Identifier, creating temporary one");
            // Create an empty FHIR::Identifier element
            localSenderIdentifier = this.identifierBuilders.buildPractitionerIdentifierFromSender(matrixUserID);
            LOG.trace("buildSenderReference(): Created Identifier --> " + localSenderIdentifier.toString());
            // Add the FHIR::Identifier to the FHIR::Reference.Identifier
            senderRef.setIdentifier(localSenderIdentifier);
            // Set the FHIR::Reference.type to "Practitioner" (we only receive events from Practitioners - for now)
            senderRef.setType("Practitioner");
        } else {
            LOG.trace("buildSenderReference(): Mapped Identifier exists, adding it to Reference");
            LOG.trace("buildSenderReference(): Mapped Identifier --> {}" + localSenderIdentifier);
            // Add the FHIR::Identifier to the FHIR::Reference.Identifier
            senderRef.setIdentifier(localSenderIdentifier);
            // Set the FHIR::Reference.type to "Practitioner" (we only receive events from
            // Practitioners - for now)
            senderRef.setType("Practitioner");
        }
        LOG.debug("buildSenderReference(): Exit, Created Reference --> {}", senderRef);
        return (senderRef);
    }

}
