## Integration testing Pub/Sub interactions in your application

As part of the [Google Cloud Platform][1] (GCP), Google provides [Pub/Sub][2] as its queuing mechanism. 
After creating a Pub/Sub topic in GCP, you can create [publishers][3] to send messages to a topic and [subscribers][4] to receive messages from a topic.
In order to send or receive Pub/Sub messages from GCP, you can choose to authenticate with GCP through user account or service account credentials.

This has one disadvantage; when running your build either locally or on a build server such as Gitlab, your application will attempt to communicate with GCP for its Pub/Sub interactions.
Your build will either fail because there are no credentials available (usually the case on build servers), 
or it will use you personal user account credentials and publish messages to topics that may also be in use on other (test) environments. 
Both situations are not desirable. 

In this article, I will first describe how to Dockerize Pub/Sub, followed by the changes required in your application. 
__If you want to dive right into the code, [go here][6].__

### Dockerizing the Pub/Sub server

In order to still allow for a build that includes tests with Pub/Sub interactions, we are going to Dockerize the Pub/Sub server. 
With this in place, we don't need to authenticate with GCP anymore and can actually verify the messages that go through our topics and subscriptions without interfering on other environments.
Google already provides us with an [emulator][5] that allows us to emulate a Pub/Sub server locally.
Our [Dockerfile](src/test/resources/docker/Dockerfile) is as follows:

```
FROM google/cloud-sdk:272.0.0

RUN git clone https://github.com/GoogleCloudPlatform/python-docs-samples.git /python-docs-samples && \
    cd /python-docs-samples/pubsub/cloud-client && \
    pip install -r requirements.txt

RUN mkdir -p /root/bin
COPY start-pubsub.sh pubsub-configuration-parser.py /root/bin/

EXPOSE 8432

CMD ["./root/bin/start-pubsub.sh"]
``` 

As per the emulator's installation instructions, we clone the Google repository and install the Pub/Sub requirements.
Afterwards, we execute [start-pubsub.sh](src/test/resources/docker/start-pubsub.sh):
```
#!/bin/bash

if [[ -z "${PUBSUB_PROJECT_ID}" ]]; then
  echo "No PUBSUB_PROJECT_ID supplied, setting default project name"
  export PUBSUB_PROJECT_ID=gcp-docker-project
fi

export PUBSUB_EMULATOR_HOST=localhost:8432

# Start the emulator in the background so that we can continue the script to create topics and subscriptions.
gcloud beta emulators pubsub start --host-port=0.0.0.0:8432 &
PUBSUB_PID=$!

if [[ -z "${PUBSUB_CONFIG}" ]]; then
  echo "No PUBSUB_CONFIG supplied, no additional topics or subscriptions will be created"
else
  python /root/bin/pubsub-configuration-parser.py ${PUBSUB_PROJECT_ID} "${PUBSUB_CONFIG}"
fi

# After these actions we bring the process back to the foreground again and wait for it to complete.
# This restores Docker's expected behaviour of coupling the lifecycle of the Docker container to the primary process.
echo "Ready"
wait ${PUBSUB_PID}
```    

This script indicates that we can provide the Docker container with two optional environment variables:
* `PUBSUB_PROJECT_ID`: GCP project ID
* `PUBSUB_CONFIG`: a JSON array that describes the topics and associated subscriptions you want to create. 
 
With this script, we start the Pub/Sub server at http://localhost:8432 and execute a simple Python script ([pubsub-configuration-parser.py](src/test/resources/docker/pubsub-configuration-parser.py)) that interprets the JSON object.

Building and running the Dockerfile can be done as follows:
```
cd src/test/resources/docker

docker build . -t pubsub

docker run  --name pubsub \
            -p 8432:8432 \
            -e PUBSUB_CONFIG='[{"name": "my-topic", "subscriptions": ["my-subscription"]}]' \
            -d pubsub

docker logs -f pubsub 
```

### Making the application configurable

What remains is configuring the publisher and subscriber Java classes to connect with the locally running Pub/Sub server.

Given a topic with the name `my-topic` and a subscription with the name `my-subscription`, the minimal setup of a publisher and subscriber is as follows:

```
Publisher
    .newBuilder(projectTopicName)
    .build();
```
```
Subscriber subscriber = Subscriber
    .newBuilder(subscriptionName, messageReceiver)
    .build();
``` 

Both builder objects can take a `CredentialsProvider` instance that determines how we authenticate with GCP.

I've created my own [CredentialsProviderFactory](src/main/java/nl/luminis/articles/pubsub/auth/CredentialsProviderFactory.java) that returns either no credentials, user credentials or service account credentials based on the Spring property `gcloud.authentication.method`.
If you are using service account credentials, you will also have to set the property `gcloud.serviceaccount.credentials.file`, which is a reference to the JSON file that contains the actual credentials.
This JSON file can be retrieved from GCP in the IAM section.

```
    public CredentialsProvider createCredentialsProvider() {
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
```

Lastly, you will have to provide both the publisher and subscriber builder objects with a `TransportChanneProvider`.
This provider will allow us to instruct Pub/Sub to use a plain text connection when we are running our application against the Dockerized Pub/Sub server.
Furthermore, it also allows us to set the URL of the Pub/Sub server. 
This has been made configurable through the `gcloud.pubsub.url` property, which is set to `localhost:8432` by default, but should be set to `pubsub.googleapis.com` when running in GCP.
```
    public TransportChannelProvider create() {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forTarget(pubSubConfig.getPubSubUrl());
        if (AuthenticationMethod.NONE.equals(pubSubConfig.getAuthenticationMethod())) {
            channelBuilder.usePlaintext();
        }
        ManagedChannel channel = channelBuilder.build();
        return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
    }
```

### Different scenarios, different settings

The table below shows the different scenarios that you can have when working with Pub/Sub and the appropriate property settings that go with each scenario:

| Application running at | Pub/Sub running at | gcloud.pubsub.url     | gcloud.authentication.method | gcloud.serviceaccount.credentials.file |
|------------------------|--------------------|-----------------------|------------------------------|----------------------------------------|
| localhost              | localhost          | localhost:8432        | NONE                         | N/A                                    |
| localhost              | GCP                | pubsub.googleapis.com | USER_ACCOUNT                 | N/A                                    |
| localhost              | GCP                | pubsub.googleapis.com | SERVICE_ACCOUNT              | /path/to/credentials.json              |
| GCP                    | GCP                | pubsub.googleapis.com | SERVICE_ACCOUNT              | /path/to/credentials.json              |

You can also run the [Application.java](src/main/java/nl/luminis/articles/pubsub/Application.java) and publish a message with the use of the [Swagger UI][7]
or checkout this [Pub/Sub integration test](src/test/java/nl/luminis/articles/pubsub/PubSubIT).

[1]: https://cloud.google.com
[2]: https://cloud.google.com/pubsub/docs/overview
[3]: https://cloud.google.com/pubsub/docs/publisher
[4]: https://cloud.google.com/pubsub/docs/subscriber
[5]: https://cloud.google.com/pubsub/docs/emulator
[6]: https://github.com/markkrijgsman/pubsub-integration-testing
[7]: localhost:8080/swagger-ui.html