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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020-05-05
 */
public class MatrixAttribute2FHIRResourceTypeHelper
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixAttribute2FHIRResourceTypeHelper.class);

    public String deriveAssociatedFHIRResourceTypeFromMatrixRoomID(String roomID)
    {
        LOG.debug("deriveAssociatedFHIRResourceTypeFromMatrixRoomID(): Entry, roomID --> {}", roomID);
        return ("Group");
    }

    public String deriveAssociatedFHIRResourceTypeFromMatrixSenderID(String senderID)
    {
        LOG.debug("deriveAssociatedFHIRResourceTypeFromMatrixSenderID(): Entry, roomID --> {}", senderID);

        return ("Practitioner");
    }

    public String deriveAssociatedFHIRResourceTypeFromMatrixRoomMembership(String roomID)
    {
        LOG.debug("deriveAssociatedFHIRResourceTypeFromMatrixSenderID(): Entry, roomID --> {}", roomID);

        return (null);
    }

}
