package nl.luminis.articles.pubsub.publisher;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.ProjectTopicName;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.extern.slf4j.Slf4j;
import nl.luminis.articles.pubsub.auth.CredentialsProviderFactory;
import nl.luminis.articles.pubsub.auth.TransportChannelProviderFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PublisherFactory {

    private final CredentialsProviderFactory credentialsProviderFactory;
    private final TransportChannelProviderFactory channelProviderFactory;

    public PublisherFactory(CredentialsProviderFactory credentialsProviderFactory, TransportChannelProviderFactory channelProviderFactory) {
        this.credentialsProviderFactory = credentialsProviderFactory;
        this.channelProviderFactory = channelProviderFactory;
    }

    public Publisher build(ProjectTopicName projectTopicName) {
        log.info("Creating new publisher for topic {}", projectTopicName.toString());
        try {
            return Publisher
                .newBuilder(projectTopicName)
                .setCredentialsProvider(credentialsProviderFactory.create())
                .setChannelProvider(channelProviderFactory.create())
                .build();
        } catch (IOException e) {
            throw new UncheckedIOException(String.format("Failed to build publisher for topic %s", projectTopicName.toString()), e);
        }
    }
}
