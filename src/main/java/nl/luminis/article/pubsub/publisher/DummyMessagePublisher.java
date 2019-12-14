package nl.luminis.article.pubsub.publisher;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import lombok.extern.slf4j.Slf4j;
import nl.luminis.article.pubsub.PubSubConfig;
import nl.luminis.article.pubsub.dto.DummyMessage;
import nl.luminis.article.pubsub.mapper.PubsubMessageMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DummyMessagePublisher {

    private final Publisher publisher;
    private final PubsubMessageMapper pubsubMessageMapper;

    public DummyMessagePublisher(PubSubConfig pubSubConfig, PublisherFactory publisherFactory, PubsubMessageMapper pubsubMessageMapper) {
        this.publisher = publisherFactory.build(pubSubConfig.getProjectTopicName());
        this.pubsubMessageMapper = pubsubMessageMapper;
    }

    public void publish(DummyMessage message) {
        PubsubMessage pubsubMessage = pubsubMessageMapper.apply(message);
        publisher.publish(pubsubMessage);
        log.debug("Published message with ID {}", message.getId());
    }
}
