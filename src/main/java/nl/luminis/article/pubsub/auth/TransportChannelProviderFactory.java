package nl.luminis.article.pubsub.auth;

import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import nl.luminis.article.pubsub.PubSubConfig;
import org.springframework.stereotype.Service;

@Service
public class TransportChannelProviderFactory {

    private PubSubConfig pubSubConfig;

    public TransportChannelProviderFactory(PubSubConfig pubSubConfig) {
        this.pubSubConfig = pubSubConfig;
    }

    public TransportChannelProvider create() {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(pubSubConfig.getPubSubUrl());
        if (AuthenticationMethod.NONE.equals(pubSubConfig.getAuthenticationMethod())) {
            channelBuilder.usePlaintext();
        }
        ManagedChannel channel = channelBuilder.build();
        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    }
}
