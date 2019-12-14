package nl.luminis.articles.pubsub.auth;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import nl.luminis.articles.pubsub.PubSubConfig;
import org.springframework.stereotype.Service;

@Service
public class CredentialsProviderFactory {

    private final PubSubConfig pubSubConfig;

    public CredentialsProviderFactory(PubSubConfig pubSubConfig) {
        this.pubSubConfig = pubSubConfig;
    }

    public CredentialsProvider create() {
        switch (pubSubConfig.getAuthenticationMethod()) {
            case NONE:
                return getNoCredentialsProvider();
            case USER_ACCOUNT:
                return getUserCredentialsProvider();
            case SERVICE_ACCOUNT:
                return getServiceAccountCredentials();
            default:
                throw new IllegalArgumentException("Unexpected authentication method " + pubSubConfig.getAuthenticationMethod());
        }
    }

    private CredentialsProvider getNoCredentialsProvider() {
        return NoCredentialsProvider.create();
    }

    private CredentialsProvider getUserCredentialsProvider() {
        try {
            return FixedCredentialsProvider.create(GoogleCredentials.getApplicationDefault());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CredentialsProvider getServiceAccountCredentials() {
        try (InputStream stream = Files.newInputStream(Paths.get(pubSubConfig.getServiceAccountCredentialsFile()))) {
            ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(stream);
            return FixedCredentialsProvider.create(credentials);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
