As part of the [Google Cloud Platform][1] (GCP), Google provides [Pub/Sub][2] as its queuing mechanism. 
After creating a Pub/Sub topic in GCP, you can create [publishers][3] to send messages to a topic and [subscribers][4] to receive messages from a topic.
In order to send or receive Pub/Sub messages, you can choose to authenticate with GCP through service account or user account credentials.

Given a topic with the name `my-topic` and a subscription with the name `my-subscription`, the minimal setup of a publisher and subscriber is as follows:

```
publisher
```
```
subscriber
``` 

All of this works perfectly fine, until we run this code on a build server such as Gitlab.
While you have the availability of user credentials when running your application locally, Gitla has no way of authenticating with GCP. 
This is also not desirable - we don't want our builds to send messages to topics in GCP.

In order to still allow for a build that includes tests with Pub/Sub interactions, we are going to Dockerize the Pub/Sub server. 
With this in place, we don't need to authenticate with GCP anymore and can actually verify the messages that go through our topics and subscriptions.
Google already provides us with an [emulator][5] that allows us to emulate a Pub/Sub server locally.
Our Dockerfile is as follows:

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
Afterwards, we execute `start-pubsub.sh`:
```
#!/bin/bash

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
echo "[pubsub] Ready"
wait ${PUBSUB_PID}
```    

This script indicates that we need to provide our Docker container with two environment variables:
* PUBSUB_PROJECT_ID: optional GCP project ID
* PUBSUB_CONFIG: a JSON object that describes the topics and subscriptions you want to create

With this script, we start our Pub/Sub server at http://localhost:8432 and execute a simple Python script that interprets the JSON object:
```
import json
import sys

# This assumes that the Google Pub/Sub repository has been cloned in the current directory.
sys.path.append('python-docs-samples/pubsub/cloud-client')
import publisher
import subscriber

project = sys.argv[1]
try:
    pubsubConfig = json.loads(sys.argv[2])
    for topic in pubsubConfig:
        publisher.create_topic(project, topic["name"])
        for subscription in topic["subscriptions"]:
            subscriber.create_subscription(project, topic["name"], subscription)

except:
    print("Failed to parse JSON Pub/Sub configuration, please verify the input:")
    print(sys.argv[2])
    raise
``` 

Building and running this Dockerfile gives us the following:
```
docker build . -t pubsub
docker run  --name pubsub \
            -p 8432:8432 \
            -e PUBSUB_CONFIG='[{"name": "my-topic-1", "subscriptions": ["my-subscription-1", "my-subscription-2"]}, {"name": "my-topic-2", "subscriptions": []}]' \
            -d pubsub
docker logs -f pubsub 
```

```
Executing: /usr/lib/google-cloud-sdk/platform/pubsub-emulator/bin/cloud-pubsub-emulator --host=0.0.0.0 --port=8432
[pubsub] This is the Google Pub/Sub fake.

...

Topic created: name: "projects/my-gcp-project/topics/my-topic"
Subscription created: name: "projects/my-gcp-project/subscriptions/my-subscription"
topic: "projects/my-gcp-project/topics/my-topic"
push_config {
}
ack_deadline_seconds: 10
message_retention_duration {
  seconds: 604800
}
[pubsub] Ready
```

[1]: https://cloud.google.com
[2]: https://cloud.google.com/pubsub/docs/overview
[3]: https://cloud.google.com/pubsub/docs/publisher
[4]: https://cloud.google.com/pubsub/docs/subscriber
[5]: https://cloud.google.com/pubsub/docs/emulator