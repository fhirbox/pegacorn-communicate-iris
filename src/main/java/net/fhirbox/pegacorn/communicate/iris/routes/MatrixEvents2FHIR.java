/*
 * #%L
 * Wildfly Camel
 * %%
 * Copyright (C) 2013 - 2015 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package net.fhirbox.pegacorn.communicate.iris.routes;

import javax.annotation.Resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;

import net.fhirbox.pegacorn.communicate.iris.routes.helpers.CommunicationSubjectTypeCheck;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomMessage2Communication;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.communicate.iris.routes.helpers.RoomServerMessageSplitter;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomCreate2CommunicationCreate;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomCreate2GroupCreate;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomInfoName2Group;
import net.fhirbox.pegacorn.communicate.iris.validators.IncomingEventListValidator;
import org.apache.camel.LoggingLevel;

@ApplicationScoped
public class MatrixEvents2FHIR extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MatrixEvents2FHIR.class);

    private static final String RAW_ROOMSERVER_MESSAGES = "direct:roomServerMessages";

    private static final String EVENT_UNHANDLED = "direct:queueEvent-unhandled";

    private static final String EVENT_M_ROOM_ALIASES = "direct:queueEvent-m.room.aliases";
    private static final String EVENT_M_ROOM_CANONICAL_ALIASES = "direct:queueEvent-m.room.canonical_aliases";
    private static final String EVENT_M_ROOM_CREATE = "direct:queueEvent-m.room.create";
    private static final String EVENT_M_ROOM_JOIN_RULES = "direct:queueEvent-m.room.join_rules";
    private static final String EVENT_M_ROOM_MEMBER = "direct:queueEvent-m.room.member";
    private static final String EVENT_M_ROOM_POWER_LEVELS = "direct:queueEvent-m.room.power_levels";
    private static final String EVENT_M_ROOM_REDACTION = "direct:queueEvent-m.room.redaction";
    private static final String EVENT_M_ROOM_MESSAGE = "direct:queueEvent-m.room.message";
    private static final String EVENT_M_ROOM_NAME = "direct:queueEvent-m.room.name";
    
    private static final String EVENT_ROOM_CREATE_TO_COMMUNICATION = "direct:queueEvent-RoomCreate2Communication";
    private static final String EVENT_ROOM_CREATE_TO_GROUP = "direct:queueEvent-RoomCreate2Group";

    private static final String RECIPIENT_IS_A_PRACTIONER = "direct:recipient_is_a_practitioner";
    private static final String RECIPIENT_IS_A_PRACTROLE = "direct:recipient_is_a_practitionerrole";
    private static final String RECIPIENT_IS_A_CARETEAM = "direct:recipient_is_a_careteam";
    private static final String RECIPIENT_IS_A_ORGANIZATION = "direct:recipient_is_a_organization";
    private static final String RECIPIENT_IS_A_GROUP = "direct:recipient_is_a_group";
    private static final String RECIPIENT_IS_UNKNOWN = "direct:recipient_is_unkown";

    protected CommunicateProperties deploymentProperties = new CommunicateProperties();

    @Inject
    RoomMessage2Communication roomMessage2Communication;

    @Inject
    CommunicationSubjectTypeCheck communicationSubjectCheck;

    @Inject
    IncomingEventListValidator messageValidator;

    @Inject
    RoomServerMessageSplitter roomServerMessageSplitter;
    
    @Inject 
    RoomCreate2CommunicationCreate roomState2Communication;
    
    @Inject 
    RoomCreate2GroupCreate roomState2Group;
    
    @Inject
    RoomInfoName2Group roomName2Group;
    
    @Resource(mappedName = "java:jboss/DefaultJMSConnectionFactory")
    protected ConnectionFactory connectionFactory;

    @Override
    public void configure() throws Exception {
        if (getContext().hasComponent("jms") == null) {
            JmsComponent component = new JmsComponent();
            component.setConnectionFactory(connectionFactory);
            getContext().addComponent("jms", component);
        }

        LOG.info("Iris Room Event (RoomServer --> Iris) Endpoint = " + deploymentProperties.getIrisEndPointForRoomServerEvent());
        
        from(deploymentProperties.getIrisEndPointForRoomServerEvent())
                .routeId("MatrixEvents2FHIR-RoomServer2Iris-Route")
                .transform(simple("${bodyAs(String)}"))
                .log(LoggingLevel.DEBUG, "Message received!!!")
                .bean(messageValidator, "validateEventSetMessage")
                .log(LoggingLevel.DEBUG, "Message Validated, Forwarding!!!")
                .to(ExchangePattern.InOnly, RAW_ROOMSERVER_MESSAGES)
                .transform().simple("{}")
                .end();

        from(RAW_ROOMSERVER_MESSAGES)
                .routeId("MatrixEvents2FHIR-Message2EventIterator-Route")
                .log(LoggingLevel.DEBUG, "RoomServer Message Split and Distribution")
                .split().method(roomServerMessageSplitter, "splitMessageIntoEvents")
                .choice()
                    .when(simple("${body} contains 'm.room.aliases'")).to(EVENT_M_ROOM_ALIASES)
                    .when(simple("${body} contains 'm.room.canonical_aliases'")).to(EVENT_M_ROOM_CANONICAL_ALIASES)
                    .when(simple("${body} contains 'm.room.create'")).to(EVENT_M_ROOM_CREATE)
                    .when(simple("${body} contains 'm.room.join_rules'")).to(EVENT_M_ROOM_JOIN_RULES)
                    .when(simple("${body} contains 'm.room.member'")).to(EVENT_M_ROOM_MEMBER)
                    .when(simple("${body} contains 'm.room.power_levels'")).to(EVENT_M_ROOM_POWER_LEVELS)
                    .when(simple("${body} contains 'm.room.redaction'")).to(EVENT_M_ROOM_REDACTION)
                    .when(simple("${body} contains 'm.room.message'")).to(EVENT_M_ROOM_MESSAGE)
                    .when(simple("${body} contains 'm.room.name'")).to(EVENT_M_ROOM_NAME)
                    .otherwise().to(EVENT_UNHANDLED)
                .endChoice()
                .end();

        from(EVENT_M_ROOM_MESSAGE)
                .routeId("MatrixEvents2FHIR-m_room_message-Route")
                .log(LoggingLevel.INFO,"m.room.message --> ${body}")
                .bean(roomMessage2Communication, "doTransform")
                .multicast().parallelProcessing()
                    .to(RECIPIENT_IS_A_PRACTIONER, RECIPIENT_IS_A_PRACTROLE, RECIPIENT_IS_A_CARETEAM, RECIPIENT_IS_A_ORGANIZATION, RECIPIENT_IS_A_GROUP, RECIPIENT_IS_UNKNOWN)
                    .end()
                .end();

        from(EVENT_M_ROOM_NAME)
                .routeId("MatrixEvents2FHIR-m_room_name-Route")
                .log(LoggingLevel.INFO,"m.room.name --> ${body}")
                .bean(roomName2Group, "transformToGroup")
                .to(deploymentProperties.getRawGroupTopic());
                
        from(EVENT_M_ROOM_ALIASES)
                .routeId("MatrixEvents2FHIR-m_room_aliases-Route")
                .log(LoggingLevel.INFO,"m.room.aliases --> ${body}")
                .to("stub:nowhere")
                .end();

        from(EVENT_M_ROOM_CANONICAL_ALIASES)
                .routeId("MatrixEvents2FHIR-m_room_canonical-aliases-Route")
                .log(LoggingLevel.INFO,"m.room.canonical_aliases --> ${body}")
                .to("stub:nowhere")
                .end();

        from(EVENT_M_ROOM_CREATE)
                .routeId("MatrixEvents2FHIR-m_room_create-Route")
                .log(LoggingLevel.INFO, "m.room.create --> ${body}")
                .multicast().parallelProcessing()
                    .to(EVENT_ROOM_CREATE_TO_COMMUNICATION, EVENT_ROOM_CREATE_TO_GROUP)
                    .end()
                .end();

        from(EVENT_M_ROOM_JOIN_RULES)
                .routeId("MatrixEvents2FHIR-m_room_join_rules-Route")
                .log(LoggingLevel.INFO,"m.room.join_rules --> ${body}")
                .to("stub:nowhere")
                .end();

        from(EVENT_M_ROOM_MEMBER)
                .routeId("MatrixEvents2FHIR-m_room_member-Route")
                .log(LoggingLevel.INFO,"m.room.member --> ${body}")
                .to("stub:nowhere")
                .end();

        from(EVENT_M_ROOM_POWER_LEVELS)
                .routeId("MatrixEvents2FHIR-m_room_power_levels-Route")
                .log(LoggingLevel.INFO, "m.room.power_levels --> ${body}")
                .to("stub:nowhere")
                .end();

        from(EVENT_M_ROOM_REDACTION)
                .routeId("MatrixEvents2FHIR-m_room_redaction-Route")
                .log(LoggingLevel.INFO, "m.room.redaction --> ${body}")
                .to("stub:nowhere")
                .end();
        
        from(EVENT_UNHANDLED)
                .routeId("MatrixEvents2FHIR-unhandled_event-Route")
                .log(LoggingLevel.INFO, "undhandled event --> ${body}")
                .to("stub:nowhere")
                .end();

    // Routing for Messages (actual chat messages) 
    
        from(RECIPIENT_IS_A_PRACTIONER)
                .routeId("MatrixEvents2FHIR-CommunicationToAPractitioner-Route")
                .filter().method(communicationSubjectCheck, "isSubjectAPractitioner")
                .to(deploymentProperties.getRawCommunication2PractitionersTopic());

        from(RECIPIENT_IS_A_PRACTROLE)
                .routeId("MatrixEvents2FHIR-CommunicationToAPractitionerRole-Route")
                .filter().method(communicationSubjectCheck, "isSubjectAPractitionerRole")
                .to(deploymentProperties.getRawCommunication2PractitionerRolesTopic());

        from(RECIPIENT_IS_A_CARETEAM)
                .routeId("MatrixEvents2FHIR-CommunicationToACareTeam-Route")
                .filter().method(communicationSubjectCheck, "isSubjectACareTeam")
                .to(deploymentProperties.getRawCommunication2CareTeamsTopic());

        from(RECIPIENT_IS_A_ORGANIZATION)
                .routeId("MatrixEvents2FHIR-CommunicationToAnOrganization-Route")
                .filter().method(communicationSubjectCheck, "isSubjectAOrganization")
                .to(deploymentProperties.getRawCommunication2OrganizationsTopic());

        from(RECIPIENT_IS_A_GROUP)
                .routeId("MatrixEvents2FHIR-CommunicationToAGroup-Route")
                .filter().method(communicationSubjectCheck, "isSubjectAGroup")
                .to(deploymentProperties.getRawCommunication2GroupsTopic());

        from(RECIPIENT_IS_UNKNOWN)
                .routeId("MatrixEvents2FHIR-CommunicationRecipientUnknown")
                .filter().method(communicationSubjectCheck, "isSubjectAnOther")
                .to(deploymentProperties.getRawCommunicationTopic());
        
    // routes for Processing Room Events --> m.room.create
        from(EVENT_ROOM_CREATE_TO_COMMUNICATION)
                .routeId("MatrixEvents2FHIR-m.room.create2Communication-Route")
                .bean(roomState2Communication, "transformToCommunication")
                .to(deploymentProperties.getRawCommunicationTopic());
        
        from(EVENT_ROOM_CREATE_TO_GROUP)
                .routeId("MatrixEvents2FHIR-m.room.create2Group-Route")
                .bean(roomState2Group, "transformToGroup")
                .to(deploymentProperties.getRawGroupTopic());
    
    }

    protected RouteDefinition asyncDistributeTo(RouteDefinition route, String shardName) {
        route
                .to(ExchangePattern.InOnly, deploymentProperties.getRawCommunicationTopic());
        return route;
    }

}
