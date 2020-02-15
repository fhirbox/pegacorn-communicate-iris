package net.fhirbox.pegacorn.communicate.iris.transformers.helpers;

import java.util.Date;
import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.fhirbox.pegacorn.communicate.iris.transformers.cachedmaps.UserID2PractitionerReferenceMap;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;

import net.fhirbox.pegacorn.referencevalues.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton                                                   
public class IdentifierBuilders {

    private static final Logger LOG = LoggerFactory.getLogger(IdentifierBuilders.class);

    PegacornSystemReference pegacornSystemReference = new PegacornSystemReference();
    
    @Inject
    protected UserID2PractitionerReferenceMap theUserID2PractitionerIDMap;

    public Identifier buildPractitionerIdentifierFromSender(String pSenderID) {
        LOG.debug("buildPractitionerIdentifierFromSender(): Entry, pSenderID --> " + pSenderID);
        if ((pSenderID == null) || pSenderID.isEmpty()) {
            return (null);
        }
        LOG.trace("buildPractitionerIdentifierFromSender(): pSenderID contains something");
        if( this.theUserID2PractitionerIDMap == null){
            LOG.debug("buildPractitionerIdentifierFromSender(): Something is wrong with the UserID2PractitionerID map");
        }
        Identifier localSenderIdentifier = this.theUserID2PractitionerIDMap.getFHIRResourceIdentifier(pSenderID);
        LOG.trace("buildPractitionerIdentifierFromSender(): looked up the UserID2PractitionerIDMap");
        if (localSenderIdentifier != null) {
            LOG.debug("buildPractitionerIdentifierFromSender(): found valid Identifier in IDMap --> " + localSenderIdentifier.toString() );
            return (localSenderIdentifier);
        } else {
            LOG.trace("buildPractitionerIdentifierFromSender(): not valid Identifier found, creating one");
            // Create an empty FHIR::Identifier element
            localSenderIdentifier = new Identifier();
            // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
            localSenderIdentifier.setUse(Identifier.IdentifierUse.TEMP);
            // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
            localSenderIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
            // Set the FHIR::Identifier.Value to the "sender" from the RoomServer system
            localSenderIdentifier.setValue(pSenderID);
            LOG.debug("buildPractitionerIdentifierFromSender(): Exit, create Identifier --> " + localSenderIdentifier.toString());
            return (localSenderIdentifier);
        }
    }

    public Identifier buildGroupIdentifierFromRoomID(String pRoomID, Long pCreationTime) {
        if ((pRoomID == null) || pRoomID.isEmpty()) {
            return (null);
        }
        // Create an empty FHIR::Identifier element
        Identifier localGroupIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
        localGroupIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localGroupIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "room id" from the RoomServer system
        localGroupIdentifier.setValue(pRoomID);
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        if (pCreationTime > 0) {
            lEventIDPeriod.setStart(new Date(pCreationTime));
        } else {
            lEventIDPeriod.setStart(new Date());
        }
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have
        // not expire point)
        localGroupIdentifier.setPeriod(lEventIDPeriod);
        return (localGroupIdentifier);
    }

}
