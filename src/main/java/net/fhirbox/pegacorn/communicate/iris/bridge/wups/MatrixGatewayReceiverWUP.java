package net.fhirbox.pegacorn.communicate.iris.bridge.wups;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import net.fhirbox.pegacorn.communicate.iris.IrisWUPIntersectPoints;
import net.fhirbox.pegacorn.communicate.iris.bridge.gateway.matrixeventreceiver.IncomingEventListValidator;
import net.fhirbox.pegacorn.communicate.iris.bridge.gateway.matrixeventreceiver.IncomingMatrixEventUoWEncapsulator;
import net.fhirbox.pegacorn.deploymentproperties.CommunicateProperties;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MatrixGatewayReceiverWUP extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(MatrixGatewayReceiverWUP.class);

    @Resource(mappedName = "java:jboss/DefaultJMSConnectionFactory")
    protected ConnectionFactory connectionFactory;
    
    @Inject
    CommunicateProperties deploymentProperties;

    @Inject
    IncomingEventListValidator messageValidator;
    
    @Inject
    IrisWUPIntersectPoints wupHandoverPoints;
    
    @Inject
    IncomingMatrixEventUoWEncapsulator incomingMessageHandler;

    @Override
    public void configure() throws Exception {

        if (getContext().hasComponent("jms") == null) {
            JmsComponent component = new JmsComponent();
            component.setConnectionFactory(connectionFactory);
            getContext().addComponent("jms", component);
        }

        LOG.info(".configure(): Iris Room Event (RoomServer --> Iris) Endpoint = " + deploymentProperties.getIrisEndPointForRoomServerEvent());

        from(deploymentProperties.getIrisEndPointForRoomServerEvent())
                .routeId("MatrixEvents2FHIR-RoomServer2Iris-Route -->")
                .transform(simple("${bodyAs(String)}"))
                .log(LoggingLevel.DEBUG, "Message received!!!")
                .bean(messageValidator, "validateEventSetMessage")
                .bean(incomingMessageHandler, "encapsulateMatrixMessage")
                .log(LoggingLevel.DEBUG, "Message Validated, Forwarding!!!")
                .to(ExchangePattern.InOnly, wupHandoverPoints.getRAWMatrixRoomServerMessagePoint())
                .transform().simple("{}")
                .end();
    }
}
