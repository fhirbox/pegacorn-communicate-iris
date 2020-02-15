/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.routes;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import net.fhirbox.pegacorn.communicate.iris.routes.helpers.CommunicationSubjectTypeCheck;
import net.fhirbox.pegacorn.communicate.iris.routes.helpers.RoomServerMessageSplitter;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomCreate2CommunicationCreate;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomCreate2GroupCreate;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomInfoName2Group;
import net.fhirbox.pegacorn.communicate.iris.transformers.matrix2fhir.RoomMessage2Communication;
import net.fhirbox.pegacorn.communicate.iris.validators.IncomingEventListValidator;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Mark A. Hunter (ACT Health)
 */
@ApplicationScoped
public class FHIREvents2Matrix extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(FHIREvents2Matrix.class);

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
        if(getContext().hasComponent("jms") == null) {
            JmsComponent component = new JmsComponent();
            component.setConnectionFactory(connectionFactory);
            getContext().addComponent("jms", component);
        }

        LOG.info("Iris Room Event (Iris --> RoomServer) Endpoint = " + deploymentProperties.getRoomServerEndPointForIrisEvent());

    }
}