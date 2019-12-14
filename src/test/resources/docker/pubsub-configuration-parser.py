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
    print(
        "Failed to parse JSON Pub/Sub configuration, please verify the input:")
    print(sys.argv[2])
    raise
