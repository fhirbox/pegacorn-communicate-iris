/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.common;

import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Reference;

import java.util.List;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
public class CommunicationRecipientTypeCheck {
    
    public boolean isATargetRecipientAPractitioner( Communication pCommunication )
    {
    	Reference localCommunicationSubject = pCommunication.getSubject();
        if(localCommunicationSubject == null){
            return(false);
        }
        List<Reference> localReferenceList = pCommunication.getRecipient();
        for( int localCounter = 0; localCounter < localReferenceList.size(); localCounter += 1){
            if(localReferenceList.get(localCounter).getType().equals("Practitioner") )
            {
                return(true);
            }
        }
        return(false);
    }
    
    public boolean isATargetRecipientAPractitionerRole( Communication pCommunication )
    {
        if(pCommunication.getRecipient().isEmpty()){
            return(false);
        }
        List<Reference> localReferenceList = pCommunication.getRecipient();
        for( int localCounter = 0; localCounter < localReferenceList.size(); localCounter += 1){
            if(localReferenceList.get(localCounter).getType().equals("PractitionerRole") )
            {
                return(true);
            }
        }
        return(false);
    }    
    
    public boolean isATargetRecipientACareTeam( Communication pCommunication )
    {
        if(pCommunication.getRecipient().isEmpty()){
            return(false);
        }
        List<Reference> localReferenceList = pCommunication.getRecipient();
        for( int localCounter = 0; localCounter < localReferenceList.size(); localCounter += 1){
            if(localReferenceList.get(localCounter).getType().equals("CareTeam") )
            {
                return(true);
            }
        }
        return(false);
    }    
    
    public boolean isATargetRecipientAOrganization( Communication pCommunication )
    {
        if(pCommunication.getRecipient().isEmpty()){
            return(false);
        }
        List<Reference> localReferenceList = pCommunication.getRecipient();
        for( int localCounter = 0; localCounter < localReferenceList.size(); localCounter += 1){
            if(localReferenceList.get(localCounter).getType().equals("Organization") )
            {
                return(true);
            }
        }
        return(false);
    }    

    public boolean isATargetRecipientAGroup( Communication pCommunication )
    {
        if(pCommunication.getRecipient().isEmpty()){
            return(false);
        }
        List<Reference> localReferenceList = pCommunication.getRecipient();
        for( int localCounter = 0; localCounter < localReferenceList.size(); localCounter += 1){
            if(localReferenceList.get(localCounter).getType().equals("Group") )
            {
                return(true);
            }
        }
        return(false);
    }      
    
    public boolean isATargetRecipientAnOther( Communication pCommunication )
    {
        if(pCommunication.getRecipient().isEmpty()){
            return(true);
        }
        boolean localTestIsPractitioner = isATargetRecipientAPractitioner(pCommunication);
        boolean localTestIsPractitionerRole = isATargetRecipientAPractitionerRole(pCommunication);
        boolean localTestIsCareTeam = isATargetRecipientACareTeam(pCommunication);
        boolean localTestIsOrganization = isATargetRecipientAOrganization(pCommunication);
        boolean localTestIsGroup = isATargetRecipientAGroup(pCommunication);
        if( localTestIsPractitioner || localTestIsPractitionerRole || localTestIsCareTeam || localTestIsOrganization || localTestIsGroup )
        {
            return(false);
        }
        return(true);
    }               
}
