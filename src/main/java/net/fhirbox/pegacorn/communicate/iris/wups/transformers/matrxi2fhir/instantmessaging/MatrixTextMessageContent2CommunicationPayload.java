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
package net.fhirbox.pegacorn.communicate.iris.wups.transformers.matrxi2fhir.instantmessaging;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import net.fhirbox.pegacorn.communicate.iris.wups.common.MatrixMessageException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.TransformErrorException;
import net.fhirbox.pegacorn.communicate.iris.wups.common.WrongContentTypeException;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.StringType;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class MatrixTextMessageContent2CommunicationPayload
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixTextMessageContent2CommunicationPayload.class);

    public List<Communication.CommunicationPayloadComponent> buildMTextPayload(JSONObject roomIMContent)
            throws MatrixMessageException, TransformErrorException, WrongContentTypeException, JSONException
    {
        LOG.debug("buildMTextPayload(): Entry, pRoomMessage.content --> {}", roomIMContent);
        ArrayList<Communication.CommunicationPayloadComponent> payLoadList = new ArrayList<Communication.CommunicationPayloadComponent>();
        JSONObject normalPayload = new JSONObject();
        JSONObject formattedPayload = new JSONObject();
        if (roomIMContent.has("format")) {
            LOG.trace("buildMTextPayload(): format == {}", roomIMContent.getString("format"));
            CharSequence formatType = "org.matrix.custom.html";
            if (roomIMContent.getString("format").contains(formatType)) {
                LOG.trace("buildMTextPayload(): Creating a new Formated Payload Component");
                formattedPayload.put("format", roomIMContent.get("format"));
                formattedPayload.put("formatted_body", roomIMContent.getString("formatted_body"));
                Communication.CommunicationPayloadComponent formattedPayloadComponent = new Communication.CommunicationPayloadComponent();
                formattedPayloadComponent.setContent(new StringType(formattedPayload.toString()));
                payLoadList.add(formattedPayloadComponent);
            }
        }
        normalPayload.put("format", "text");
        normalPayload.put("formatted_body", roomIMContent.getString("body"));
        Communication.CommunicationPayloadComponent normalPayloadComponent = new Communication.CommunicationPayloadComponent();
        normalPayloadComponent.setContent(new StringType(normalPayload.toString()));
        payLoadList.add(normalPayloadComponent);
        LOG.debug("buildMTextPayload(): Exit, Number of PayLoadComponents = " + payLoadList.size());
        return (payLoadList);
    }

}
