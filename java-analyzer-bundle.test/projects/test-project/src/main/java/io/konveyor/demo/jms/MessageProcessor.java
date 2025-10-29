package io.konveyor.demo.jms;

import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Test class for annotated element matching.
 * Tests @MessageDriven and @ActivationConfigProperty with specific attribute values.
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "queue/test"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class MessageProcessor implements MessageListener {

    @Override
    public void onMessage(Message message) {
        System.out.println("Processing message: " + message);
    }
}
