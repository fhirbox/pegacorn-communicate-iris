/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.transformers.common.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.hl7.fhir.r4.model.Identifier;

/**
 *
 * @author mhunter
 */
public class IdentifierConverter {

    public String fromIdentifier2String(Identifier theIdentifier) {
        ObjectMapper mapper = new ObjectMapper();
        if (theIdentifier == null) {
            return (null);
        }
        try {
            String identifierString = mapper.writeValueAsString(theIdentifier);
            return (identifierString);
        } catch (JsonProcessingException jsonEx) {
            return (null);
        }
    }

    public Identifier fromString2Identifier(String theIdentifierString) {
        ObjectMapper mapper = new ObjectMapper();
        if (theIdentifierString == null) {
            return (null);
        }
        try {
            Identifier theIdentifier = mapper.readValue(theIdentifierString, Identifier.class);
            return (theIdentifier);
        } catch (IOException ioEx) {
            return (null);
        }
    }
}
