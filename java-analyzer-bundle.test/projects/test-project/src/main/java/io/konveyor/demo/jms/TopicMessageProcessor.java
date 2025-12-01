package io.konveyor.demo.jms;

import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * Test class for annotated element matching with different attribute values.
 * Tests @ActivationConfigProperty with destinationType = Topic.
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "topic/notifications"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic")
})
public class TopicMessageProcessor implements MessageListener {

    @Override
    public void onMessage(Message message) {
        System.out.println("Processing topic message: " + message);
    }
}
