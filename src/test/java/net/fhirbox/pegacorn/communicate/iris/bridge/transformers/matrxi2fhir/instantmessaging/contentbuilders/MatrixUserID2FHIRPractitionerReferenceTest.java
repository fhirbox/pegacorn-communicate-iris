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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.instantmessaging.contentbuilders;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common.MatrixAttribute2FHIRIdentifierBuilders;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.common.keyidentifiermaps.MatrixUserID2PractitionerIDMap;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020-05-05
 */
@RunWith(MockitoJUnitRunner.class)
public class MatrixUserID2FHIRPractitionerReferenceTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MatrixUserID2FHIRPractitionerReferenceTest.class);

    PegacornSystemReference pegacornSystemReference;

    CommunicateProperties communicateProperties;

    @Mock
    MatrixUserID2PractitionerIDMap theUserID2PractitionerIDMap;

    public MatrixUserID2FHIRPractitionerReferenceTest()
    {
        communicateProperties = new CommunicateProperties();
        pegacornSystemReference = new PegacornSystemReference();
    }

    /**
     * Test of buildFHIRPractitionerReferenceFromMatrixUserID method, of class
     * MatrixUserID2FHIRPractitionerReference.
     */
    @Test
    public void testBuildFHIRPractitionerReferenceFromMatrixUserIDCreateNonTEMP() throws Exception
    {
        LOG.info("testBuildFHIRPractitionerReferenceFromMatrixUserIDCreateNonTEMP");
        String matrixUserID = "@Doug:CHS";
        boolean createIfNotExist = true;
        Reference expResult = null;

        Identifier testIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
        testIdentifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        testIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "sender" from the RoomServer system
        testIdentifier.setValue("Doug Burrows");
        // Create the empty FHIR::Reference element
        expResult = new Reference();
        // Add the FHIR::Identifier to the FHIR::Reference.Identifier
        expResult.setIdentifier(testIdentifier);
        // Set the FHIR::Reference.type to "Group"
        expResult.setType("Practitioner");
        
        final MatrixUserID2FHIRPractitionerReference instance = new MatrixUserID2FHIRPractitionerReference();
        instance.communicateProperties = communicateProperties;
        instance.pegacornSystemReference = pegacornSystemReference;
        instance.theUserID2PractitionerIDMap = theUserID2PractitionerIDMap;

        when(theUserID2PractitionerIDMap.getPractitionerIDFromUserName(matrixUserID)).thenReturn(testIdentifier);

        Reference result = instance.buildFHIRPractitionerReferenceFromMatrixUserID(matrixUserID, createIfNotExist);
        assertTrue(result.equalsDeep(expResult));
    }

    @Test
    public void testBuildFHIRPractitionerReferenceFromMatrixUserIDReturnNULL() throws Exception
    {
        LOG.info("testBuildFHIRPractitionerReferenceFromMatrixUserIDReturnNULL");
        String matrixUserID = "@Doug:CHS";
        boolean createIfNotExist = false;
        Reference expResult = null;

        final MatrixUserID2FHIRPractitionerReference instance = new MatrixUserID2FHIRPractitionerReference();
        instance.communicateProperties = communicateProperties;
        instance.pegacornSystemReference = pegacornSystemReference;
        instance.theUserID2PractitionerIDMap = theUserID2PractitionerIDMap;

        when(theUserID2PractitionerIDMap.getPractitionerIDFromUserName(matrixUserID)).thenReturn(null);

        Reference result = instance.buildFHIRPractitionerReferenceFromMatrixUserID(matrixUserID, createIfNotExist);
        assertTrue(result == null);
    }

    @Test
    public void testBuildFHIRPractitionerReferenceFromMatrixUserIDCreateTEMP() throws Exception
    {
       
        LOG.info("testBuildFHIRPractitionerReferenceFromMatrixUserIDCreateTEMP");
        String matrixUserID = "@Doug:CHS";
        boolean createIfNotExist = true;
        Reference expResult = null;

        Identifier testIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (this id is not guaranteed)
        testIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        testIdentifier.setSystem(pegacornSystemReference.getDefaultIdentifierSystemForRoomServerDetails());
        // Set the FHIR::Identifier.Value to the "sender" from the RoomServer system
        testIdentifier.setValue("@Doug:CHS");
        // Create the empty FHIR::Reference element
        expResult = new Reference();
        // Add the FHIR::Identifier to the FHIR::Reference.Identifier
        expResult.setIdentifier(testIdentifier);
        // Set the FHIR::Reference.type to "Group"
        expResult.setType("Practitioner");

        final MatrixUserID2FHIRPractitionerReference instance = new MatrixUserID2FHIRPractitionerReference();
        instance.communicateProperties = communicateProperties;
        instance.pegacornSystemReference = pegacornSystemReference;
        instance.theUserID2PractitionerIDMap = theUserID2PractitionerIDMap;

        when(theUserID2PractitionerIDMap.getPractitionerIDFromUserName(matrixUserID)).thenReturn(null);

        Reference result = instance.buildFHIRPractitionerReferenceFromMatrixUserID(matrixUserID, createIfNotExist);
        if(result.equalsDeep(expResult)){
            assertTrue(true);
        } else {
            System.out.println("result.Reference.type = " + result.getType() + ": expResult.Reference.type = " + expResult.getType()) ;
            System.out.println("result.Reference.Identifier.Use = " + result.getIdentifier().getUse().getDisplay() + ": expResult.Reference.Identifier.Use = " + expResult.getIdentifier().getUse().getDisplay()) ;
            System.out.println("result.Reference.Identifier.System = " + result.getIdentifier().getSystem() + ": expResult.Reference.Identifier.System = " + expResult.getIdentifier().getSystem()) ;
            System.out.println("result.Reference.Identifier.Value = " + result.getIdentifier().getValue() + ": expResult.Reference.Identifier.Value = " + expResult.getIdentifier().getValue()) ;
            fail();
        }
    }
}
