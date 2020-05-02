/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.helpers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.RoomMessage2Communication;

import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.ResourceType;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
public class CommunicationSubjectTypeCheck {

    private static final Logger LOG = LoggerFactory.getLogger(CommunicationSubjectTypeCheck.class);
    private FhirContext fhirContextHandle;
    private IParser fhirResourceParser;

    public CommunicationSubjectTypeCheck() {

        this.fhirContextHandle = FhirContext.forR4();
        this.fhirResourceParser = this.fhirContextHandle.newJsonParser();
        this.fhirResourceParser.setPrettyPrint(true);
    }

    public Communication getCommunicationElementFromBundle(Bundle theBundle) {
        LOG.debug("getCommunicationElementFromBundle(): Entry");
        LOG.trace("getCommunicationElementFromBundle(): Bundle Element --> " + fhirResourceParser.encodeResourceToString(theBundle));
        if (!theBundle.hasType()) {
            LOG.debug("getCommunicationElementFromBundle(): Exit, Bundle has no Type");
            return (null);
        }
        if (!(theBundle.getType() == BundleType.MESSAGE)) {
            LOG.debug("getCommunicationElementFromBundle(): Exit, Bundle is not a Message type");
            return (null);
        }
        List<BundleEntryComponent> bundleElements = theBundle.getEntry();
        if (bundleElements.size() < 2) {
            LOG.debug("getCommunicationElementFromBundle(): Exit, Bundle does not contain >= 2 elements - size = " + bundleElements.size());
            return (null);
        }

        for (int counter = 0; counter < bundleElements.size(); counter += 1) {
            BundleEntryComponent testBundle = bundleElements.get(counter);
            if (testBundle.hasResource()) {
                if (testBundle.getResource().getResourceType() == ResourceType.Communication) {
                    Communication communicationElement = (Communication) (testBundle.getResource());
                    LOG.trace("getCommunicationElementFromBundle(): Communication Element --> " + fhirResourceParser.encodeResourceToString(communicationElement));
                    return (communicationElement);
                }
            }
        }
        LOG.debug("getCommunicationElementFromBundle(): Exit, could not find Communication element");
        return (null);
    }

    public boolean isSubjectAPractitioner(Bundle pBundle) {
        LOG.debug("isSubjectAPractitioner(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
        Reference localCommunicationSubject = communicationElement.getSubject();
        if (localCommunicationSubject == null) {
            LOG.debug("isSubjectAPractitioner(): The Subject of the Communication Element is null");
            return (false);
        }
        LOG.trace("isSubjectAPractitioner(): Subject Type --> " + localCommunicationSubject.getType());
        if (localCommunicationSubject.getType().toString().equals("Practitioner")) {
            LOG.debug("isSubjectAPractitioner(): The Subject of the Communication Element is a Practitioner");
            return (true);
        } else {
            LOG.debug("isSubjectAPractitioner(): The Subject of the Communication Element is not a Practitioner");
            return (false);
        }
    }

    public boolean isSubjectAPractitionerRole(Bundle pBundle) {
        LOG.debug("isSubjectAPractitionerRole(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
        Reference localCommunicationSubject = communicationElement.getSubject();
        if (localCommunicationSubject == null) {
            LOG.debug("isSubjectAPractitionerRole(): The Subject of the Communication Element is null");
            return (false);
        }
        LOG.trace("isSubjectAPractitionerRole(): Subject Type --> " + localCommunicationSubject.getType());
        if (localCommunicationSubject.getType().toString().equals("PractitionerRole")) {
            LOG.debug("isSubjectAPractitionerRole(): The Subject of the Communication Element is a PractitionerRole");
            return (true);
        } else {
            LOG.debug("isSubjectAPractitionerRole(): The Subject of the Communication Element is not a PractitionerRole");
            return (false);
        }
    }

    public boolean isSubjectACareTeam(Bundle pBundle) {
        LOG.debug("isSubjectACareTeam(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
        Reference localCommunicationSubject = communicationElement.getSubject();
        if (localCommunicationSubject == null) {
            LOG.debug("isSubjectACareTeam(): The Subject of the Communication Element is null");
            return (false);
        }
        LOG.trace("isSubjectACareTeam(): Subject Type --> " + localCommunicationSubject.getType());
        if (localCommunicationSubject.getType().toString().equals("CareTeam")) {
            LOG.debug("isSubjectACareTeam(): The Subject of the Communication Element is a CareTeam");
            return (true);
        } else {
            LOG.debug("isSubjectACareTeam(): The Subject of the Communication Element is not a CareTeam");
            return (false);
        }
    }

    public boolean isSubjectAOrganization(Bundle pBundle) {
        LOG.debug("isSubjectAOrganization(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
        Reference localCommunicationSubject = communicationElement.getSubject();
        if (localCommunicationSubject == null) {
            LOG.debug("isSubjectAOrganization(): The Subject of the Communication Element is null");
            return (false);
        }
        LOG.trace("isSubjectAOrganization(): Subject Type --> " + localCommunicationSubject.getType());
        if (localCommunicationSubject.getType().toString().equals("Organization")) {
            LOG.debug("isSubjectAOrganization(): The Subject of the Communication Element is a Organization");
            return (true);
        } else {
            LOG.debug("isSubjectAOrganization(): The Subject of the Communication Element is not a Organization");
            return (false);
        }
    }

    public boolean isSubjectAGroup(Bundle pBundle) {
        LOG.debug("isSubjectAGroup(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
        Reference localCommunicationSubject = communicationElement.getSubject();
        if (localCommunicationSubject == null) {
            LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is null");
            return (false);
        }
        LOG.trace("isSubjectAGroup(): Subject Type --> " + localCommunicationSubject.getType());
        if (localCommunicationSubject.getType().toString().equals("Group")) {
            LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is a Group");
            return (true);
        } else {
            LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is not a Group");
            return (false);
        }
    }

    public boolean isSubjectAnOther(Bundle pBundle) {
        LOG.debug("isSubjectAnOther(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
        Reference localCommunicationSubject = communicationElement.getSubject();
        if (localCommunicationSubject == null) {
            LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is null");
            return (true);
        }
        LOG.trace("isSubjectAnOther(): Subject Type --> " + localCommunicationSubject.getType());
        boolean localTestIsPractitioner = (localCommunicationSubject.getType() == "Practitioner");
        boolean localTestIsPractitionerRole = (localCommunicationSubject.getType() == "PractitionerRole");
        boolean localTestIsCareTeam = (localCommunicationSubject.getType() == "CareTeam");
        boolean localTestIsOrganization = (localCommunicationSubject.getType() == "Organization");
        boolean localTestIsGroup = (localCommunicationSubject.getType() == "Group");
        if (localTestIsPractitioner || localTestIsPractitionerRole || localTestIsCareTeam || localTestIsOrganization || localTestIsGroup) {
            LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is already defined");
            return (false);
        }
        LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is not defined");
        return (true);
    }
}
