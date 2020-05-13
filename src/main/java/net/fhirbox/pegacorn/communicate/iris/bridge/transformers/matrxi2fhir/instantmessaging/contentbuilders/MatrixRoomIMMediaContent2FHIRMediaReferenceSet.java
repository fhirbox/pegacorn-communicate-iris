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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MatrixMessageException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.PayloadTransformationOutcomeEnum;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.MinorTransformationException;
import net.fhirbox.pegacorn.communicate.iris.common.Exceptions.WrongContentTypeException;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.json.JSONObject;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h1> Create FHIR::Media References from Room Based Message content </h1>
 * <p>
 * This class is used to create the FHIR::Media References using the content
 * within the Room Message.
 * <p>
 * <b> Note:  </b> This class ONLY generates a "temporary" Reference which needs
 * to be updated once the FHIR::Media entity is generated & populated with the
 * content.
 *
 * @author Mark A. Hunter (ACT Health)
 * @since 2020-01-20
 *
 */
@ApplicationScoped
public class MatrixRoomIMMediaContent2FHIRMediaReferenceSet
{
    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomIMMediaContent2FHIRMediaReferenceSet.class);
    private static final String IDENTIFIER_SYSTEM = "http://pegacorn.fhirbox.net/pegacorn/R1/communicate/groupserver";

    // TODO : Fix javadoc for generateMediaReferenceSet()
    /**
     * This method returns the set (list) of References constructed from the
     * Room Message provided in the constructor.
     * <p>
     * It builds the list of References via invoking the appropriate method
     * associated with the media type.
     * <p>
     * @return a List of References
     */
    public Reference buildMediaReferenceSet(JSONObject roomIMContent, Date messageDate) 
            throws MatrixMessageException, MinorTransformationException, WrongContentTypeException, JSONException
    {
        LOG.debug("getReferenceSet(), Entry, Message Payload --> {}", roomIMContent);
        // Check for validity of the payload message & return -null- if there is a problem
        if (roomIMContent == null) {
            LOG.error("getReferenceSet(): Exit, Instant Message Payload is null");
            throw (new MatrixMessageException("Room Instant Message --> whole message is null"));
        }
        if (roomIMContent.isEmpty()) {
            LOG.error("getReferenceSet(): Exit, Instant Message Payload is empty");
            throw (new MatrixMessageException("Room Instant Message --> it is empty"));
        }
        if (!roomIMContent.has("msgtype")) {
            LOG.error("getReferenceSet(): Exit, could not find -msgtype-");
            throw (new MatrixMessageException("Room Instant Message --> does not contain a -msgtype-"));
        }
        String instantMessagePayloadType = roomIMContent.getString("msgtype");
        Reference newReference = null;
        // Now we do a -case- based on content (-msgtype-) type
        switch (instantMessagePayloadType) {
            case "m.image":
                newReference = this.buildImageReference(roomIMContent, messageDate);
                break;
            case "m.audio":
                newReference = this.buildAudioReference(roomIMContent, messageDate);
                break;
            case "m.video":
                newReference = this.buildVideoReference(roomIMContent, messageDate);
                break;
            case "m.file":
                newReference = this.buildFileReference(roomIMContent, messageDate);
                break;
            default:
                throw(new WrongContentTypeException("Room Instant Message --> is not of type m.file, m.video, m.audio or m.picture"));
        }
        return (newReference);
    }

    // TODO : fix javadoc for buildImageReference()
    /**
     * This method constructs a basic FHIR::Reference for the reference Media
     * entity that contains the message's image attachment.
     * <p>
     * The Identifier::Value will be constructed to have the following content:
     * "url={the url text}" The identifier/URI contained within the RoomServer
     * room message will always be used as part of the identifier value - with
     * the appropriate System.
     * <p>
     * @return an Enum stating the success of the transformation
     */
    public Reference buildImageReference(JSONObject mediaMessageContent, Date messageDate) 
            throws WrongContentTypeException, MinorTransformationException, JSONException
    {
        LOG.debug("buildImageReference(): Entry, Instant Message Content {} ", mediaMessageContent);
        if (mediaMessageContent == null) {
            LOG.error("buildImageReference(): Instant Message Content is null");
            throw (new MinorTransformationException("Instant Message Content is null"));
        }
        // First, check that it is, in fact, an message with an image
        LOG.trace("buildImageReference(): Checking to ensure the -msgtype- is m.image");
        String msgType = mediaMessageContent.getString("msgtype");
        if (!msgType.equals("m.image")) {
            LOG.error("buildImageReference(): Exit, This is not an m.image content block, returning --> PAYLOAD_TRANSFORM_FAILURE_INGRES_CONTENT_MALFORMED");
            throw (new WrongContentTypeException("Instant Message is not of type m.image"));
        }
        // Now we know there is a "body" element (all -content- blocks should have one) 
        // as we wouldn't have passed the first statement in this function!
        // So, let's now check that there is a "url"  element
        LOG.trace("buildImageReference(): Checking to ensure the -content- has a -url- element");
        String imageURL = mediaMessageContent.getString("url");
        LOG.trace("buildImageReference(): Creating the Reference Identifier");
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (it needs to be updateed when Media created/populated)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(IDENTIFIER_SYSTEM);
        // Set the FHIR::Identifier.value" to the "url" segment
        localResourceIdentifier.setValue(mediaMessageContent.getString("url"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(messageDate);
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        LOG.trace("buildImageReference(): Creating the Reference");
        // Create an empty FHIR::Reference element
        Reference localReference = new Reference();
        // Set the type of Resource (FHIR::Reference.type) to which this segment points
        localReference.setType("Media");
        // Add the FHIR::Identifier we just created to FHIR::Reference.identifier
        localReference.setIdentifier(localResourceIdentifier);
        // Add the Display Name to the FHIR::Reference.display using the "body" from "content"
        localReference.setDisplay("Image = " + mediaMessageContent.getString("body"));
        LOG.debug("buildImageReference(): Created Reference --> {}", localReference.toString());
        return (localReference);
    }

    // TODO : fix javadoc for buildVideoReference()
    /**
     * This method constructs a basic FHIR::Reference for the reference Media
     * entity that contains the message's video attachment.
     * <p>
     * The Identifier::Value will be constructed to have the following content:
     * "BodyText={the body text}::url={the url text}" The identifier/URI
     * contained within the RoomServer room message will always be used as part
     * of the identifier value - with the appropriate System.
     * <p>
     * @return A FHIR::Reference resource (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    public Reference buildVideoReference(JSONObject mediaMessageContent, Date messageDate)
            throws WrongContentTypeException, MinorTransformationException, JSONException
    {
        // TODO : Add the additional information (e.g. Thumbnail, Duration etc.) to an "extension" of the Media Reference?
        LOG.debug("buildVideoReference(): Entry, Instant Message Content {} ", mediaMessageContent);
        if (mediaMessageContent == null) {
            LOG.error("buildVideoReference(): Instant Message Content is null");
            throw (new MinorTransformationException("Instant Message Content is null"));
        }
        // First, check that it is, in fact, an message with an image
        LOG.trace("buildVideoReference(): Checking to ensure the -msgtype- is m.video");
        String msgType = mediaMessageContent.getString("msgtype");
        if (!msgType.equals("m.video")) {
            LOG.error("buildVideoReference(): Exit, This is not an m.video content block");
            throw (new WrongContentTypeException("Instant Message is not of type m.video"));
        }
        LOG.trace("buildVideoReference(): Creating the Reference Identifier");
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (it needs to be updateed when Media created/populated)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(IDENTIFIER_SYSTEM);
        // Set the FHIR::Identifier.value" to the "url" segment
        localResourceIdentifier.setValue(mediaMessageContent.getString("url"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(messageDate);
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        // Create an empty FHIR::Reference element
        LOG.trace("buildVideoReference(): Creating the Reference");
        Reference localReference = new Reference();
        // Set the type of Resource (FHIR::Reference.type) to which this segment points
        localReference.setType("Media");
        // Add the FHIR::Identifier we just created to FHIR::Reference.identifier
        localReference.setIdentifier(localResourceIdentifier);
        // Add the Display Name to the FHIR::Reference.display using the "body" from "content"
        localReference.setDisplay("Video = " + mediaMessageContent.getString("body"));
        LOG.debug("buildVideoReference(): Created Reference --> {}", localReference);
        return (localReference);
    }

    // TODO : fix javadoc for buildAudioReference()
    /**
     * This method constructs a basic FHIR::Reference for the reference Media
     * entity that contains the message's audio attachment.
     * <p>
     * The Identifier::Value will be constructed to have the following content:
     * "BodyText={the body text}::url={the url text}" The identifier/URI
     * contained within the RoomServer room message will always be used as part
     * of the identifier value - with the appropriate System.
     * <p>
     *
     * @return Identifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    public Reference buildAudioReference(JSONObject mediaMessageContent, Date messageDate)
            throws WrongContentTypeException, MinorTransformationException, JSONException
    {
        // TODO : Add the additional information (e.g. Thumbnail, Duration etc.) to an "extension" of the Media Reference?
        LOG.debug("buildAudioReference(): Entry, Instant Message Content {} ", mediaMessageContent);
        if (mediaMessageContent == null) {
            LOG.error("buildAudioReference(): Instant Message Content is null");
            throw (new MinorTransformationException("Instant Message Content is null"));
        }
        // First, check that it is, in fact, an message with an image
        LOG.trace("buildAudioReference(): Checking to ensure the -msgtype- is m.audio");
        String msgType = mediaMessageContent.getString("msgtype");
        if (!msgType.equals("m.video")) {
            LOG.error("buildAudioReference(): Exit, This is not an m.audio content block");
            throw (new WrongContentTypeException("Instant Message is not of type m.audio"));
        }
        LOG.trace("buildAudioReference(): Creating the Reference Identifier");
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (it needs to be updateed when Media created/populated)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(IDENTIFIER_SYSTEM);
        // Set the FHIR::Identifier.value" to the "url" segment
        localResourceIdentifier.setValue(mediaMessageContent.getString("url"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(messageDate);
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        // Create an empty FHIR::Reference element
        LOG.trace("buildAudioReference(): Creating the Reference");
        Reference localReference = new Reference();
        // Set the type of Resource (FHIR::Reference.type) to which this segment points
        localReference.setType("Media");
        // Add the FHIR::Identifier we just created to FHIR::Reference.identifier
        localReference.setIdentifier(localResourceIdentifier);
        // Add the Display Name to the FHIR::Reference.display using the "body" from "content"
        localReference.setDisplay("Audio = " + mediaMessageContent.getString("body"));
        LOG.debug("buildAudioReference(): Exiting, successfully created Media (Audio) Reference --> {}", localReference);
        return (localReference);
    }
    
    // TODO : fix javadoc for buildAudioReference()
    /**
     * This method constructs a basic FHIR::Reference for the reference Media
     * entity that contains the message's file attachment.
     * <p>
     * The Identifier::Value will be constructed to have the following content:
     * "BodyText={the body text}::url={the url text}" The identifier/URI
     * contained within the RoomServer room message will always be used as part
     * of the identifier value - with the appropriate System.
     * <p>
     *
     * @return Identifier A FHIR::Identifier resource (see
     * https://www.hl7.org/fhir/references.html#Reference)
     */
    public Reference buildFileReference(JSONObject mediaMessageContent, Date messageDate)
            throws WrongContentTypeException, MinorTransformationException, JSONException
    {
        // TODO : Add the additional information (e.g. Thumbnail, Duration etc.) to an "extension" of the Media Reference?
        LOG.debug("buildFileReference(): Entry, Instant Message Content {} ", mediaMessageContent);
        if (mediaMessageContent == null) {
            LOG.error("buildFileReference(): Instant Message Content is null");
            throw (new MinorTransformationException("Instant Message Content is null"));
        }
        // First, check that it is, in fact, an message with an image
        LOG.trace("buildFileReference(): Checking to ensure the -msgtype- is m.file");
        String msgType = mediaMessageContent.getString("msgtype");
        if (!msgType.equals("m.file")) {
            LOG.error("buildFileReference(): Exit, This is not an m.file content block");
            throw (new WrongContentTypeException("Instant Message is not of type m.file"));
        }
        LOG.trace("buildFileReference(): Creating the Reference Identifier");
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (it needs to be updateed when Media created/populated)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(IDENTIFIER_SYSTEM);
        // Set the FHIR::Identifier.value" to the "url" segment
        localResourceIdentifier.setValue(mediaMessageContent.getString("url"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(messageDate);
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        // Create an empty FHIR::Reference element
        LOG.trace("buildFileReference(): Creating the Reference");
        Reference localReference = new Reference();
        // Set the type of Resource (FHIR::Reference.type) to which this segment points
        localReference.setType("Media");
        // Add the FHIR::Identifier we just created to FHIR::Reference.identifier
        localReference.setIdentifier(localResourceIdentifier);
        // Add the Display Name to the FHIR::Reference.display using the "body" from "content"
        localReference.setDisplay("File = " + mediaMessageContent.getString("body"));
        LOG.debug("buildFileReference(): Exiting, successfully created Media (File) Reference --> {}", localReference);
        return (localReference);
    }    
}
