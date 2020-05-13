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
package net.fhirbox.pegacorn.communicate.iris.bridge.gateway.matrixeventreceiver;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.fhirbox.pegacorn.petasos.model.FDN;
import net.fhirbox.pegacorn.petasos.model.RDN;
import net.fhirbox.pegacorn.petasos.model.UoW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ACT Health
 */
@ApplicationScoped
public class IncomingMatrixEventUoWEncapsulator
{
    private static final Logger LOG = LoggerFactory.getLogger(IncomingMatrixEventUoWEncapsulator.class);

    @Inject
    IncomingMatrixMessageSplitter messageSplitter;

    public UoW encapsulateMatrixMessage(String matrixMessage)
    {
        LOG.debug("encapsulateMatrixMessage(): Entry, Matrix Message --> {}", matrixMessage);
        
        // First, create my FDN
        FDN myFDN = new FDN();
        RDN mySystemRDN = new RDN("System", "AETHER");
        RDN mySubSystemRDN = new RDN("SubSystem", "Communicate");
        RDN myModuleRDN = new RDN("Module", "Iris");
        RDN mySubModuleRDN = new RDN("SubModule", "Matrix2FHIR");
        RDN newFunctionRDN = new RDN("Function", "MatrixEventReceiver");
        myFDN.appendRDN(mySystemRDN);
        myFDN.appendRDN(mySubSystemRDN);
        myFDN.appendRDN(myModuleRDN);
        myFDN.appendRDN(mySubModuleRDN);
        myFDN.appendRDN(newFunctionRDN);
        LOG.trace("encapsulateMatrixMessage(): FDN Created, value --> {}", myFDN);
        
        // Now, create the UoW's ingress content list by splitting apart the incoming
        // Matrix message (as the Matrix message itself is an array of messages
        LinkedHashSet<String> newContent = messageSplitter.splitMessageIntoEvents(matrixMessage);
        LOG.trace("encapsulateMatrixMessage(): Matrix Message split into discrete messages");
        UoW newUoW = new UoW( myFDN, newContent);
        LOG.debug("encapsulateMatrixMessage(): Exit, UoW create, FDN --> {}", newUoW.getUoWFDN().toString());
        return(newUoW);
    }
}
