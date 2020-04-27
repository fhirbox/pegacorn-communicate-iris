/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.transformers.common.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.hl7.fhir.r4.model.Reference;

/**
 *
 * @author mhunter
 */
public class ReferenceConverter {

    public String fromReference2String(Reference theReference) {
        ObjectMapper mapper = new ObjectMapper();
        if (theReference == null) {
            return (null);
        }
        try {
            String identifierString = mapper.writeValueAsString(theReference);
            return (identifierString);
        } catch (JsonProcessingException jsonEx) {
            return (null);
        }
    }

    public Reference fromString2Reference(String theReferenceString) {
        ObjectMapper mapper = new ObjectMapper();
        if (theReferenceString == null) {
            return (null);
        }
        try {
            Reference theReference = mapper.readValue(theReferenceString, Reference.class);
            return (theReference);
        } catch (IOException ioEx) {
            return (null);
        }
    }
}
