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
package net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.rooms;

import java.time.Instant;
import java.util.Date;
import javax.inject.Inject;
import net.fhirbox.pegacorn.communicate.iris.bridge.transformers.matrxi2fhir.common.MatrixAttribute2FHIRIdentifierBuilders;
import net.fhirbox.pegacorn.referencevalues.PegacornSystemReference;
import net.fhirbox.pegacorn.fhir.r4.model.helpers.GroupMemberActionExtensionEnum;
import net.fhirbox.pegacorn.fhir.r4.model.helpers.GroupMemberActionExtensionHelper;
import net.fhirbox.pegacorn.fhir.r4.model.helpers.IdentifierExtensionMeanings;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ACT Health
 */
public class MatrixRoomEvent2FHIRGroupAttributeBuilders
{

    private static final Logger LOG = LoggerFactory.getLogger(MatrixRoomEvent2FHIRGroupAttributeBuilders.class);

    @Inject
    MatrixAttribute2FHIRIdentifierBuilders identifierBuilders;

    @Inject
    PegacornSystemReference pegacornSystemReference;

    public Reference buildGroupManagerReference(JSONObject roomEventContent)
    {
        Identifier localCreatorIdentifier = identifierBuilders.buildFHIRPractitionerIdentifierFromMatrixUserID(roomEventContent.getString("creator"));
        Reference groupManagerRerefence = new Reference();
        groupManagerRerefence.setIdentifier(localCreatorIdentifier);
        groupManagerRerefence.setType("Practitioner");
        groupManagerRerefence.setDisplay("Practitioner = " + roomEventContent.getString("creator"));
        return (groupManagerRerefence);
    }

    public Identifier buildGroupIdentifier(String roomID)
    {
        Identifier groupIdentifier = identifierBuilders.buildFHIRGroupIdentifierFromMatrixRoomID(roomID, Long.MAX_VALUE);
        return (groupIdentifier);
    }

    public Reference buildGroupReference(String roomID)
    {
        Identifier groupIdentifier = buildGroupIdentifier(roomID);
        Reference groupReference = new Reference();
        // Add the FHIR::Identifier to the FHIR::Reference.Identifier
        groupReference.setIdentifier(groupIdentifier);
        // Set the FHIR::Reference.type to "Group"
        groupReference.setType("Group");
//        LOG.debug(".buildFHIRGroupReferenceFromMatrixRoomID(): Created new (Temporary) FHIR::Reference for the FHIR::Group --> " + localSubjectReference.toString());
        return (groupReference);
    }

    public Group.GroupMemberComponent buildGroupMember(String userID, Date membershipDateCommencement, GroupMemberActionExtensionEnum memberStatus)
    {
        Group.GroupMemberComponent newMemberComponent = new Group.GroupMemberComponent();
        Identifier memberIdentifier = identifierBuilders.buildFHIRPractitionerIdentifierFromMatrixUserID(userID);
        Reference memberReference = new Reference();
        memberReference.setIdentifier(memberIdentifier);
        memberReference.setType("Practitioner");
        memberReference.setDisplay("Practitioner = " + userID);
        Period membershipPeriod = new Period();
        membershipPeriod.setStart(membershipDateCommencement);
        membershipPeriod.setEnd(Date.from(Instant.EPOCH));
        GroupMemberActionExtensionHelper membershipHelper = new GroupMemberActionExtensionHelper();
        membershipHelper.injectGroupMemberStatusExtension(newMemberComponent, memberStatus);
        newMemberComponent.setEntity(memberReference);
        newMemberComponent.setPeriod(membershipPeriod);
        newMemberComponent.setInactive(false);
        return (newMemberComponent);
    }

    public Group.GroupMemberComponent buildMembershipComponent(JSONObject roomMemberEvent)
    {
        LOG.debug("buildMembershipComponent(): Entry, creating a GroupMembershipComponent for --> {}", roomMemberEvent.getString("state_key"));
        boolean isValidMemberEvent = true;
        LOG.trace("buildMembershipComponent(): Check to see if -state_key- and -content- are present");
        if (!roomMemberEvent.has("state_key")) {
            LOG.trace("buildMembershipComponent(): no -state_key-");
            isValidMemberEvent = false;
        }
        if (!roomMemberEvent.has("content")) {
            LOG.trace("buildMembershipComponent(): no -content-");
            isValidMemberEvent = false;
        }
        if (!isValidMemberEvent) {
            LOG.debug("buildMembershipComponent(): Exit, no -content- or -state_key-");
            return (null);
        }
        LOG.trace("buildMembershipComponent(): Extract the -content- from the Event");
        JSONObject roomMemberEventContent = roomMemberEvent.getJSONObject("content");
        if (!roomMemberEventContent.has("membership")) {
            LOG.trace("buildMembershipComponent(): no -membership- field, this is Required");
            isValidMemberEvent = false;
        }
        if (!isValidMemberEvent) {
            LOG.debug("buildMembershipComponent(): Exit, no -membership- field in the -content-");
            return (null);
        }
        LOG.trace("buildMembershipComponent(): Create the Identifier for the Member associated with the Event");
        Identifier memberIdentifier = this.identifierBuilders.buildFHIRPractitionerIdentifierFromMatrixUserID(roomMemberEvent.getString("state_key"));
        LOG.trace("buildMembershipComponent(): Check to see if there is an associated Display Name for the Member, if so add it to the Extensions");
        if (roomMemberEventContent.has("displayname")) {
            LOG.trace("buildMembershipComponent(): Adding a friendly name to the Identifier");
            Extension memberFriendlyNameExtension = new Extension();
            String memberDisplayName = roomMemberEventContent.getString("displayname");
            memberFriendlyNameExtension.setUrl(new IdentifierExtensionMeanings().getIdentifierAssociatedFriendlyName());
            memberFriendlyNameExtension.setValue(new StringType(memberDisplayName));
            memberIdentifier.addExtension(memberFriendlyNameExtension);
            LOG.trace("buildMembershipComponent(): Added friendly name extension to the Identifier, value --> {}", memberDisplayName);
        }
        LOG.trace("buildMembershipComponent(): Check to see if there is an associated Avatar for the Member, if so add it to the Extensions");
        if (roomMemberEventContent.has("displayname")) {
            LOG.trace("buildMembershipComponent(): Adding an Avatar to the Identifier");
            Extension memberAvatarExtension = new Extension();
            String memberAvatarURL = roomMemberEventContent.getString("avatar_url");
            memberAvatarExtension.setUrl(new IdentifierExtensionMeanings().getIdentifierAssociatedAvatar());
            memberAvatarExtension.setValue(new StringType(memberAvatarURL));
            memberIdentifier.addExtension(memberAvatarExtension);
            LOG.trace("buildMembershipComponent(): Added Avatar for the member, value --> {}", memberAvatarURL);
        }
        LOG.trace("buildMembershipComponent(): Create a Reference from the Member Identifier");
        Reference memberReference = new Reference();
        memberReference.setIdentifier(memberIdentifier);
        memberReference.setType("Practitioner");
        LOG.trace("buildMembershipComponent(): Create the actual GroupMemberComponent");
        Group.GroupMemberComponent newMembershipComponent = new Group.GroupMemberComponent();
        newMembershipComponent.setEntity(memberReference);
        GroupMemberActionExtensionHelper membershipHelper = new GroupMemberActionExtensionHelper();
        GroupMemberActionExtensionEnum membershipAction;
        LOG.trace("buildMembershipComponent(): Assign the -membership- action");
        switch (roomMemberEventContent.getString("membership")) {
            case "invite":
                LOG.trace("buildMembershipComponent(): Assigning -Invite- as the Membership action");
                membershipAction = GroupMemberActionExtensionEnum.MEMBERSHIP_STATUS_INVITED;
                membershipHelper.injectGroupMemberStatusExtension(newMembershipComponent, membershipAction);
                break;
            case "join":
                LOG.trace("buildMembershipComponent(): Assigning -Join- as the Membership action");
                membershipAction = GroupMemberActionExtensionEnum.MEMBERSHIP_STATUS_JOINED;
                membershipHelper.injectGroupMemberStatusExtension(newMembershipComponent, membershipAction);
                break;
            case "knock":
                LOG.trace("buildMembershipComponent(): Assigning -Knock- as the Membership action");
                membershipAction = GroupMemberActionExtensionEnum.MEMBERSHIP_STATUS_KNOCK;
                membershipHelper.injectGroupMemberStatusExtension(newMembershipComponent, membershipAction);
                break;
            case "leave":
                LOG.trace("buildMembershipComponent(): Assigning -leave- as the Membership action");
                membershipAction = GroupMemberActionExtensionEnum.MEMBERSHIP_STATUS_LEFT;
                membershipHelper.injectGroupMemberStatusExtension(newMembershipComponent, membershipAction);
                break;
            case "ban":
                LOG.trace("buildMembershipComponent(): Assigning -ban- as the Membership action");
                membershipAction = GroupMemberActionExtensionEnum.MEMBERSHIP_STATUS_BANNED;
                membershipHelper.injectGroupMemberStatusExtension(newMembershipComponent, membershipAction);
                break;
            default:
                LOG.trace("buildMembershipComponent(): Assigning default action (nochange) to the Membership action");
                membershipAction = GroupMemberActionExtensionEnum.MEMBERSHIP_STATUS_NOCHANGE;
                membershipHelper.injectGroupMemberStatusExtension(newMembershipComponent, membershipAction);
        }
        LOG.debug("buildMembershipComponent(): Exit, created a Group.GroupMemberComponent --> {}",newMembershipComponent );
        return (newMembershipComponent);
    }
}
