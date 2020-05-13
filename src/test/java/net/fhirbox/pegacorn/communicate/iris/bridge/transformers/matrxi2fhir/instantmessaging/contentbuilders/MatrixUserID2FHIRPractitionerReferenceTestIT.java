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

import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020-05-05
 */
@RunWith(Arquillian.class)
public class MatrixUserID2FHIRPractitionerReferenceTestIT
{
    
    public MatrixUserID2FHIRPractitionerReferenceTestIT()
    {
    }
    
    @Deployment
    public static WebArchive createDeployment(){
        WebArchive irisTestArchive = ShrinkWrap.create(WebArchive.class);
        
        return(irisTestArchive);
    }

    /**
     * Test of buildFHIRPractitionerReferenceFromMatrixUserID method, of class MatrixUserID2FHIRPractitionerReference.
     */
    @Test
    public void testBuildFHIRPractitionerReferenceFromMatrixUserID() throws Exception
    {
        System.out.println("buildFHIRPractitionerReferenceFromMatrixUserID");
        String matrixUserID = "";
        boolean createIfNotExist = false;
        MatrixUserID2FHIRPractitionerReference instance = new MatrixUserID2FHIRPractitionerReference();
        Reference expResult = null;
        Reference result = instance.buildFHIRPractitionerReferenceFromMatrixUserID(matrixUserID, createIfNotExist);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
