package nl.luminis.article.pubsub.subscriber;

import com.google.api.core.ApiService;
import com.google.api.core.ApiService.Listener;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.ProjectSubscriptionName;
import lombok.extern.slf4j.Slf4j;
import nl.luminis.article.pubsub.auth.CredentialsProviderFactory;
import nl.luminis.article.pubsub.auth.TransportChannelProviderFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SubscriberFactory {

    private final CredentialsProviderFactory credentialsProviderFactory;
    private final TransportChannelProviderFactory channelProviderFactory;

    public SubscriberFactory(CredentialsProviderFactory credentialsProviderFactory, TransportChannelProviderFactory channelProviderFactory) {
        this.credentialsProviderFactory = credentialsProviderFactory;
        this.channelProviderFactory = channelProviderFactory;
    }

    public Subscriber build(ProjectSubscriptionName subscriptionName, MessageReceiver messageReceiver) {
        log.info("Creating new subscriber for subscription {}", subscriptionName.toString());

        Subscriber subscriber = Subscriber
            .newBuilder(subscriptionName, messageReceiver)
            .setCredentialsProvider(credentialsProviderFactory.createCredentialsProvider())
            .setChannelProvider(channelProviderFactory.create())
            .build();

        subscriber.addListener(createListener(subscriptionName), MoreExecutors.directExecutor());

        return subscriber;
    }

    private Listener createListener(ProjectSubscriptionName subscriptionName) {
        return new Listener() {
            @Override
            public void failed(ApiService.State from, Throwable failure) {
                log.error("An error occurred while subscribing to {} (previous state: {})", subscriptionName.toString(), from.name());
            }
        };
    }
}
