/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.wups.common;

/**
 *
 * @author mhunter
 */
public enum PayloadTransformationOutcomeEnum {
    PAYLOAD_TRANSFORM_SUCCESSFUL("pegacorn.communicate.iris.transforms.outcome.success"),
    PAYLOAD_TRANSFORM_FAILURE("pegacorn.communicate.iris.transforms.outcome.failure"),
    PAYLOAD_TRANSFORM_FAILURE_INGRES_CONTENT_MALFORMED("pegacorn.communicate.iris.transforms.outcome.failure.content_malformed"),
    PAYLOAD_TRANSFORM_FAILURE_INGRES_CONTENT_INCOMPLETE("pegacorn.communicate.iris.transforms.outcome.failure.ingres_content_incomplete"),
    PAYLOAD_TRANSFORM_FAILURE_EGRES_CONTENT_INSUFFICIENT("pegacorn.communicate.iris.transforms.outcome.failure.egres_content_insufficient");
    
    private String payloadTransformationOutcome;
    
    private PayloadTransformationOutcomeEnum(String outcome ){
        this.payloadTransformationOutcome = outcome;
    }
    
    public String getPayloadTransformationOutcome(){
        return(this.payloadTransformationOutcome);
    } 
}
