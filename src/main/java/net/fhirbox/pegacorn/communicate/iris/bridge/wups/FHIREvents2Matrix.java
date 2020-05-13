/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.fhirbox.pegacorn.communicate.iris.bridge.wups;

import javax.enterprise.context.ApplicationScoped;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;

import org.apache.camel.builder.RouteBuilder;
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

    @Override
    public void configure() throws Exception {
/*        if(getContext().hasComponent("jms") == null) {
            JmsComponent component = new JmsComponent();
            component.setConnectionFactory(connectionFactory);
            getContext().addComponent("jms", component);
        }

        LOG.info("Iris Room Event (Iris --> RoomServer) Endpoint = " + deploymentProperties.getRoomServerEndPointForIrisEvent());
*/
    } 
}
