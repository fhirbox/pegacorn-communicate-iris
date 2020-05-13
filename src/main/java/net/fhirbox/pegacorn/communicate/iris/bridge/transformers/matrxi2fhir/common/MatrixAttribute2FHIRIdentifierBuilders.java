package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common;

import java.util.Date;
import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;

import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixUserID2PractitionerIDMap;
import net.fhirbox.pegacorn.referencevalues.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton                                                   
public class MatrixAttribute2FHIRIdentifierBuilders {

    private static final Logger LOG = LoggerFactory.getLogger(MatrixAttribute2FHIRIdentifierBuilders.class);

    PegacornSystemReference pegacornSystemReference = new PegacornSystemReference();
    
    @Inject
    protected MatrixUserID2PractitionerIDMap theUserID2PractitionerIDMap;

    public Identifier buildFHIRPractitionerIdentifierFromMatrixUserID(String userID) {
        LOG.debug("buildFHIRPractitionerIdentifierFromMatrixUserID(): Entry, userID --> " + userID);
        if ((userID == null) || userID.isEmpty()) {
            LOG.debug("buildFHIRPractitionerIdentifierFromMatrixUserID(): Exit, userID is empty or null");
            return (null);
        }
        LOG.trace("buildFHIRPractitionerIdentifierFromMatrixUserID(): userID contains something");
        if( this.theUserID2PractitionerIDMap == null){
            LOG.debug("buildFHIRPractitionerIdentifierFromMatrixUserID(): Something is wrong with the UserID2PractitionerID map");
        }
        Identifier localSenderIdentifier = this.theUserID2PractitionerIDMap.getPractitionerIDFromUserName(userID);
        LOG.trace("buildFHIRPractitionerIdentifierFromMatrixUserID(): looked up the UserID2PractitionerIDMap");
        if (localSenderIdentifier != null) {
            LOG.debug("buildFHIRPractitionerIdentifierFromMatrixUserID(): found valid Identifier in IDMap --> " + localSenderIdentifier.toString() );
            return (localSenderIdentifier);
        } else {
            LOG.trace("buildFHIRPractitionerIdentifierFromMatrixUserID(): not valid Identifier found, creating one");
            // Create an empty FHIR::Identifier element
            localSenderIdentifier = new Identifier();
            // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
            localSenderIdentifier.setUse(Identifier.IdentifierUse.TEMP);
            // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
            localSenderIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
            // Set the FHIR::Identifier.Value to the "sender" from the RoomServer system
            localSenderIdentifier.setValue(userID);
            LOG.debug("buildFHIRPractitionerIdentifierFromMatrixUserID(): Exit, create Identifier --> " + localSenderIdentifier.toString());
            return (localSenderIdentifier);
        }
    }

    public Identifier buildFHIRGroupIdentifierFromMatrixRoomID(String roomID, Long creationTime) {
        LOG.debug("buildFHIRGroupIdentifierFromMatrixRoomID(): Entry, roomID --> {}, creationTime --> {}", roomID, creationTime);
        if ((roomID == null) || roomID.isEmpty()) {
            LOG.debug("buildFHIRPractitionerIdentifierFromMatrixUserID(): Exit, roomID is empty or null");
            return (null);
        }
        // Create an empty FHIR::Identifier element
        Identifier localGroupIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
        localGroupIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localGroupIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "room id" from the RoomServer system
        localGroupIdentifier.setValue(roomID);
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        if (creationTime > 0) {
            lEventIDPeriod.setStart(new Date(creationTime));
        } else {
            lEventIDPeriod.setStart(new Date());
        }
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have
        // not expire point)
        localGroupIdentifier.setPeriod(lEventIDPeriod);
        return (localGroupIdentifier);
    }

}
