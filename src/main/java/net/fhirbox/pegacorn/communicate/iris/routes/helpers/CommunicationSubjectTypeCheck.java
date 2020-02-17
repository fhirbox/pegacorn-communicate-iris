/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.routes.helpers;

import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomMessage2Communication;

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
        
    public Communication getCommunicationElementFromBundle( Bundle theBundle ){
        if( !theBundle.hasType()){
            return(null);
        }
        if( !(theBundle.getType() == BundleType.MESSAGE) ){
            return(null);
        }
        if( theBundle.getTotal() < 2 ){
            return(null);
        }
        List<BundleEntryComponent> bundleElements = theBundle.getEntry();
        for( int counter = 0; counter < bundleElements.size(); counter += 1){
            BundleEntryComponent testBundle = bundleElements.get(counter);
            if( testBundle.hasResource()){
                if( testBundle.getResource().getResourceType() == ResourceType.Communication){
                    Communication communicationElement = (Communication)(testBundle.getResource());
                    return(communicationElement);
                }
            }
        }
        return(null);
    }    

    public boolean isSubjectAPractitioner( Bundle pBundle )
    {
    	LOG.debug("isSubjectAPractitioner(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
    	Reference localCommunicationSubject = communicationElement.getSubject();
        if(localCommunicationSubject == null){
        	LOG.debug("isSubjectAPractitioner(): The Subject of the Communication Element is null");
            return(false);
        }
    	LOG.trace("isSubjectAPractitioner(): Subject Type --> " + localCommunicationSubject.getType());
        if(localCommunicationSubject.getType().toString().equals("Practitioner") ){
            LOG.debug("isSubjectAPractitioner(): The Subject of the Communication Element is a Practitioner");
           	return(true);
        } else {
	    	LOG.debug("isSubjectAPractitioner(): The Subject of the Communication Element is not a Practitioner");
	    	return(false);
        }
    }
    
    public boolean isSubjectAPractitionerRole( Bundle pBundle )
    {
    	LOG.debug("isSubjectAPractitionerRole(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
    	Reference localCommunicationSubject = communicationElement.getSubject();
        if(localCommunicationSubject == null){
        	LOG.debug("isSubjectAPractitionerRole(): The Subject of the Communication Element is null");
            return(false);
        }
    	LOG.trace("isSubjectAPractitionerRole(): Subject Type --> " + localCommunicationSubject.getType());
        if(localCommunicationSubject.getType().toString().equals("PractitionerRole") ){
            LOG.debug("isSubjectAPractitionerRole(): The Subject of the Communication Element is a PractitionerRole");
           	return(true);
        } else {
	    	LOG.debug("isSubjectAPractitionerRole(): The Subject of the Communication Element is not a PractitionerRole");
	    	return(false);
        }
    }    
    
    public boolean isSubjectACareTeam( Bundle pBundle )
    {
    	LOG.debug("isSubjectACareTeam(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
    	Reference localCommunicationSubject = communicationElement.getSubject();
        if(localCommunicationSubject == null){
        	LOG.debug("isSubjectACareTeam(): The Subject of the Communication Element is null");
            return(false);
        }
    	LOG.trace("isSubjectACareTeam(): Subject Type --> " + localCommunicationSubject.getType());
    	if(localCommunicationSubject.getType().toString().equals("CareTeam") ){
            LOG.debug("isSubjectACareTeam(): The Subject of the Communication Element is a CareTeam");
           	return(true);
        } else {
	    	LOG.debug("isSubjectACareTeam(): The Subject of the Communication Element is not a CareTeam");
	    	return(false);
        }
    }    
    
    public boolean isSubjectAOrganization( Bundle pBundle )
    {
    	LOG.debug("isSubjectAOrganization(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
    	Reference localCommunicationSubject = communicationElement.getSubject();
        if(localCommunicationSubject == null){
        	LOG.debug("isSubjectAOrganization(): The Subject of the Communication Element is null");
            return(false);
        }
    	LOG.trace("isSubjectAOrganization(): Subject Type --> " + localCommunicationSubject.getType());
    	if(localCommunicationSubject.getType().toString().equals("Organization") ){
            LOG.debug("isSubjectAOrganization(): The Subject of the Communication Element is a Organization");
           	return(true);
        } else {
	    	LOG.debug("isSubjectAOrganization(): The Subject of the Communication Element is not a Organization");
	    	return(false);
        }
    }    

    public boolean isSubjectAGroup( Bundle pBundle )
    {
    	LOG.debug("isSubjectAGroup(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
    	Reference localCommunicationSubject = communicationElement.getSubject();
        if(localCommunicationSubject == null){
        	LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is null");
            return(false);
        }
    	LOG.trace("isSubjectAGroup(): Subject Type --> " + localCommunicationSubject.getType());
    	if(localCommunicationSubject.getType().toString().equals("Group") ){
            LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is a Group");
           	return(true);
        } else {
	    	LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is not a Group");
	    	return(false);
        }
    }      
    
    public boolean isSubjectAnOther( Bundle pBundle )
    {
    	LOG.debug("isSubjectAnOther(): Bundle Element --> " + pBundle.toString());
        Communication communicationElement = getCommunicationElementFromBundle(pBundle);
    	Reference localCommunicationSubject = communicationElement.getSubject();
        if(localCommunicationSubject == null){
        	LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is null");
            return(true);
        }
    	LOG.trace("isSubjectAnOther(): Subject Type --> " + localCommunicationSubject.getType());
        boolean localTestIsPractitioner = (localCommunicationSubject.getType() == "Practitioner");
        boolean localTestIsPractitionerRole = (localCommunicationSubject.getType() == "PractitionerRole");
        boolean localTestIsCareTeam = (localCommunicationSubject.getType() == "CareTeam");
        boolean localTestIsOrganization = (localCommunicationSubject.getType() == "Organization");
        boolean localTestIsGroup = (localCommunicationSubject.getType() == "Group");
        if( localTestIsPractitioner || localTestIsPractitionerRole || localTestIsCareTeam || localTestIsOrganization || localTestIsGroup )
        {
        	LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is already defined");
        	return(false);
        }
    	LOG.debug("isSubjectAGroup(): The Subject of the Communication Element is not defined");
    	return(true);
    }               
}
