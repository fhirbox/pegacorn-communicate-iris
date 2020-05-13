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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.hl7.fhir.r4.model.Identifier;

/**
 *
 * @author Mark A. Hunter (ACT Health) 
 * @since 2020-01-20
 */
public class FHIRIdentifier2StringUtility {

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
