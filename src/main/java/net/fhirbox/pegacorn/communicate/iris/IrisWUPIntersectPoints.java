/*
 * The MIT License
 *
 * Copyright 2020 MAHun.
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
package net.fhirbox.pegacorn.communicate.iris;

import javax.enterprise.context.ApplicationScoped;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020 - May - 1
 */

@ApplicationScoped
public class IrisWUPIntersectPoints {

    private static final String RAW_ROOMSERVER_MESSAGES = "direct:iris.wupintersect.MatrixRoomServerMessages";

    private static final String EVENT_UNHANDLED = "direct:iris.wupintersect.queueEvent-unhandled";

    private static final String IRIS_TRANSFORMED_FHIR_MESSAGE = "direct:iris.wupintersect.TransformedFHIRMessage";

    
    public String getRAWMatrixRoomServerMessagePoint(){
        return(RAW_ROOMSERVER_MESSAGES);
    }
    
    public String getUnhandledEventMessagePoint(){
        return(EVENT_UNHANDLED);
    }
    
    public String getTransformedFHIRMessagePoint(){
        return(IRIS_TRANSFORMED_FHIR_MESSAGE);
    }
}
