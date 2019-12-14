package nl.luminis.articles.pubsub;

import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import lombok.Getter;
import lombok.Setter;
import nl.luminis.articles.pubsub.auth.AuthenticationMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class PubSubConfig {

    @Value("${gcloud.project.name}")
    private String gcpProjectName;
    @Value("${gcloud.authentication.method}")
    private AuthenticationMethod authenticationMethod;
    @Value("${gcloud.serviceaccount.credentials.file:#{null}}")
    private String serviceAccountCredentialsFile;

    @Value("${gcloud.pubsub.url}")
    private String pubSubUrl;
    @Value("${gcloud.pubsub.topic.name}")
    private String topicName;
    @Value("${gcloud.pubsub.subscription.name}")
    private String subscriptionName;

    public ProjectTopicName getProjectTopicName() {
        return ProjectTopicName.of(gcpProjectName, topicName);
    }

    public ProjectSubscriptionName getProjectSubscriptionName() {
        return ProjectSubscriptionName.of(gcpProjectName, subscriptionName);
    }
}
