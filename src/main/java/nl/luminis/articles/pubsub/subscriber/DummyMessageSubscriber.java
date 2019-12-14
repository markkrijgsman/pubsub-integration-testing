package nl.luminis.articles.pubsub.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import nl.luminis.articles.pubsub.PubSubConfig;
import nl.luminis.articles.pubsub.dto.DummyMessage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DummyMessageSubscriber implements MessageReceiver {

    private final ObjectMapper objectMapper;
    private final Subscriber subscriber;
    private final List<DummyMessage> messages;

    public DummyMessageSubscriber(PubSubConfig pubSubConfig, SubscriberFactory subscriberFactory, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.subscriber = subscriberFactory.build(pubSubConfig.getProjectSubscriptionName(), this);
        this.messages = new ArrayList<>();
    }

    @PostConstruct
    public void initialize() {
        subscriber.startAsync().awaitRunning();
    }

    @PreDestroy
    public void preDestroy() {
        subscriber.stopAsync();
    }

    @Override
    public void receiveMessage(PubsubMessage pubsubMessage, AckReplyConsumer consumer) {
        try {
            String data = new String(pubsubMessage.getData().toByteArray());
            log.trace(data);
            try {
                DummyMessage message = objectMapper.readValue(data, DummyMessage.class);
                messages.add(message);
                log.debug("Received message with ID {}", message.getId());
                consumer.ack();
            } catch (JsonProcessingException e) {
                log.error("Invalid JSON offered, cannot recover", e);
                log.info(data);
                // We don't want Pub/Sub to resend the message as its content is not parsable.
                consumer.ack();
            }
        } catch (Exception e) {
            log.error("Could not process message", e);
            consumer.nack();
        }
    }

    public List<DummyMessage> getMessages() {
        return messages;
    }
}
