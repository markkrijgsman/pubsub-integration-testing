package nl.luminis.article.pubsub;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.protobuf.Timestamp;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.SeekRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import nl.luminis.article.pubsub.auth.TransportChannelProviderFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PubSubTopicService {

    private final SubscriptionAdminClient subscriptionClient;

    public PubSubTopicService(TransportChannelProviderFactory channelProviderFactory) {
        this.subscriptionClient = createSubscriptionClient(channelProviderFactory);
    }

    public void reset(ProjectSubscriptionName subscription) {
        Instant time = Instant.now();

        SeekRequest request = SeekRequest.newBuilder()
            .setSubscription(subscription.toString())
            .setTime(Timestamp.newBuilder().setSeconds(time.getEpochSecond()).setNanos(time.getNano()).build())
            .build();

        subscriptionClient.seek(request);
    }

    private SubscriptionAdminClient createSubscriptionClient(TransportChannelProviderFactory channelProviderFactory) {
        try {
            return SubscriptionAdminClient.create(
                SubscriptionAdminSettings
                    .newBuilder()
                    .setTransportChannelProvider(channelProviderFactory.create())
                    .setCredentialsProvider(NoCredentialsProvider.create()).build()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
