/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
 * to be updated once the FHIR::Media entity is generated & populated with 
 * the content.
 * 
 * @author Mark A. Hunter (ACT Health)
 * @since 2020-01-20
 * 
 */
public class RoomMessage2MediaReferenceSet {
    private static final Logger LOG = LoggerFactory.getLogger(RoomMessage2MediaReferenceSet.class);
    private static final String IDENTIFIER_SYSTEM = "http://pegacorn.fhirbox.net/pegacorn/R1/iris/roomserver";

    protected List<Reference> referenceSet;
    protected JSONObject roomMessage;
    
    public RoomMessage2MediaReferenceSet(JSONObject pRoomMessage ){
        LOG.debug("constructor(): message -> " + pRoomMessage);
        this.referenceSet = new ArrayList<Reference>();
        this.roomMessage = pRoomMessage;
    }
    
    /**
     * This method returns the set (list) of References constructed from the
     * Room Message provided in the constructor.
     * <p>
     * It builds the list of References via invoking the appropriate method 
     * associated with the media type.
     * <p>
     * @return a List of References
     */
    public List<Reference> getReferenceSet(){
        LOG.debug("getReferenceSet()");
        if( this.roomMessage == null ){
            return(null);
        }
        // Check all the bits are where we need them and return "null" if not
        // Extract "content" from the room message (pRoomMessage)
        JSONObject localMessageContent = this.roomMessage.getJSONObject("content");
        // First, check that it is, in fact, an message with an image
        switch(localMessageContent.getString("msgtype")){
            case "m.image":
                this.referenceSet.add(this.buildImageReference());
                break;
            case "m.audio":
                this.referenceSet.add(this.buildAudioReference());
                break;
            case "m.video":
                this.referenceSet.add(this.buildVideoReference());
                break;
        }        
        return(this.referenceSet);
    }

    /**
     * This method constructs a basic FHIR::Reference for the reference
     * Media entity that contains the message's image attachment.
     * <p>
     * The Identifier::Value will be constructed to have the following content:
     *     "url={the url text}"
     * The identifier/URI contained within the RoomServer room message will always
     * be used as part of the identifier value - with the appropriate System.
     * <p>
     * @return Identifier A FHIR::Identifier resource (see https://www.hl7.org/fhir/references.html#Reference)
     */
    private Reference buildImageReference() {
        LOG.debug(".buildImageReference(): for Event --> " + this.roomMessage.getString("event_id"));
        // Check all the bits are where we need them and return "null" if not
        // Extract "content" from the room message (pRoomMessage)
        JSONObject localMessageContent = this.roomMessage.getJSONObject("content");
        // First, check that it is, in fact, an message with an image
        if(!("m.image".equals(localMessageContent.getString("msgtype")))){return(null);}
        // Now check that there is a "body" element
        if(localMessageContent.getString("body").length() <= 0){return(null);}
        // Lastly, check that there is a "url"  element
        if(localMessageContent.getString("url").length() <= 0){return(null);}
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (it needs to be updateed when Media created/populated)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(IDENTIFIER_SYSTEM);
        // Set the FHIR::Identifier.value" to the "url" segment
        localResourceIdentifier.setValue(localMessageContent.getString("url"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(new Date(this.roomMessage.getLong("origin_server_ts")));
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        // Create an empty FHIR::Reference element
        Reference localReference = new Reference();
        // Set the type of Resource (FHIR::Reference.type) to which this segment points
        localReference.setType("Media");
        // Add the FHIR::Identifier we just created to FHIR::Reference.identifier
        localReference.setIdentifier(localResourceIdentifier);
        // Add the Display Name to the FHIR::Reference.display using the "body" from "content"
        localReference.setDisplay("Image = " + localMessageContent.getString("body"));
        LOG.debug(".buildImageReference(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localReference);
    }   
    
    /**
     * This method constructs a basic FHIR::Reference for the reference
     * Media entity that contains the message's video attachment.
     * <p>
     * The Identifier::Value will be constructed to have the following content:
     *      "BodyText={the body text}::url={the url text}"
     * The identifier/URI contained within the RoomServer room message will always
     * be used as part of the identifier value - with the appropriate System.
     * <p>
     * @return A FHIR::Reference resource (see https://www.hl7.org/fhir/references.html#Reference)
     */
    private Reference buildVideoReference() {
        LOG.debug(".buildVideoReference(): for Event --> " + this.roomMessage.getString("event_id"));
        // Check all the bits are where we need them and return "null" if not
        // Extract "content" from the room message (pRoomMessage)
        JSONObject localMessageContent = this.roomMessage.getJSONObject("content");
        // First, check that it is, in fact, an message with an audio
        if(!("m.video".equals(localMessageContent.getString("msgtype")))){return(null);}
        // Now check that there is a "body" element
        if(localMessageContent.getString("body").length() <= 0){return(null);}
        // Lastly, check that there is a "url"  element
        if(localMessageContent.getString("url").length() <= 0){return(null);}
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (it needs to be updateed when Media created/populated)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(IDENTIFIER_SYSTEM);
        // Set the FHIR::Identifier.value" to the "url" segment
        localResourceIdentifier.setValue(localMessageContent.getString("url"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(new Date(this.roomMessage.getLong("origin_server_ts")));
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        // Create an empty FHIR::Reference element
        Reference localReference = new Reference();
        // Set the type of Resource (FHIR::Reference.type) to which this segment points
        localReference.setType("Media");
        // Add the FHIR::Identifier we just created to FHIR::Reference.identifier
        localReference.setIdentifier(localResourceIdentifier);
        // Add the Display Name to the FHIR::Reference.display using the "body" from "content"
        localReference.setDisplay("Video = " + localMessageContent.getString("body"));
        LOG.debug(".buildVideoReference(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localReference);
    }  

    /**
     * This method constructs a basic FHIR::Reference for the reference
     * Media entity that contains the message's audio attachment.
     * <p>
     * The Identifier::Value will be constructed to have the following content:
     *      "BodyText={the body text}::url={the url text}"
     * The identifier/URI contained within the RoomServer room message will always
     * be used as part of the identifier value - with the appropriate System.
     * <p>
     *
     * @return Identifier A FHIR::Identifier resource (see https://www.hl7.org/fhir/references.html#Reference)
     */
    private Reference buildAudioReference() {
        LOG.debug(".buildAudioReference(): for Event --> " + this.roomMessage.getString("event_id"));
        // Check all the bits are where we need them and return "null" if not
        // Extract "content" from the room message (pRoomMessage)
        JSONObject localMessageContent = this.roomMessage.getJSONObject("content");
        // First, check that it is, in fact, an message with an audio
        if(!("m.audio".equals(localMessageContent.getString("msgtype")))){return(null);}
        // Now check that there is a "body" element
        if(localMessageContent.getString("body").length() <= 0){return(null);}
        // Lastly, check that there is a "url"  element
        if(localMessageContent.getString("url").length() <= 0){return(null);}
        // Create the empty FHIR::Identifier element
        Identifier localResourceIdentifier = new Identifier();
        // Set the FHIR::Identifier.Use to "TEMP" (it needs to be updateed when Media created/populated)
        localResourceIdentifier.setUse(Identifier.IdentifierUse.TEMP);
        // Set the FHIR::Identifier.System to Pegacorn (it's our ID we're creating)
        localResourceIdentifier.setSystem(IDENTIFIER_SYSTEM);
        // Set the FHIR::Identifier.value" to the "url" segment
        localResourceIdentifier.setValue(localMessageContent.getString("url"));
        // Create a FHIR::Period as a container for the valid message start/end times
        Period lEventIDPeriod = new Period();
        // Set the FHIR::Period.start value to the time the message was created/sent
        lEventIDPeriod.setStart(new Date(this.roomMessage.getLong("origin_server_ts")));
        // Set the FHIR::Identifier.period to created FHIR::Period (our messages have not expire point)
        localResourceIdentifier.setPeriod(lEventIDPeriod);
        // Create an empty FHIR::Reference element
        Reference localReference = new Reference();
        // Set the type of Resource (FHIR::Reference.type) to which this segment points
        localReference.setType("Media");
        // Add the FHIR::Identifier we just created to FHIR::Reference.identifier
        localReference.setIdentifier(localResourceIdentifier);
        // Add the Display Name to the FHIR::Reference.display using the "body" from "content"
        localReference.setDisplay("Audio = " + localMessageContent.getString("body"));
        LOG.debug(".buildAudioReference(): Created Identifier --> " + localResourceIdentifier.toString());
        return (localReference);
    }   
}
