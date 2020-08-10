import json
import sys

from concurrent.futures import TimeoutError
from google.cloud import pubsub_v1


def create_topics_and_subscriptions(project_id, json_config):
    try:
        pubsub_config = json.loads(json_config)

        for topic in pubsub_config:
            create_topic(project_id, topic["name"])
            if "subscriptions" in topic:
                for subscription in topic["subscriptions"]:
                    create_subscription(project_id, topic["name"], subscription)

    except:
        print("Failed to parse JSON Pub/Sub configuration, please verify the input:")
        print(sys.argv[2])
        raise


def create_topic(project_id, topic_id):
    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project_id, topic_id)

    topic = publisher.create_topic(topic_path)

    print("Topic created: {}".format(topic))


def create_subscription(project_id, topic_id, subscription_id):
    subscriber = pubsub_v1.SubscriberClient()
    topic_path = subscriber.topic_path(project_id, topic_id)
    subscription_path = subscriber.subscription_path(project_id, subscription_id)
    subscription = subscriber.create_subscription(subscription_path, topic_path)

    print("Subscription created: {}".format(subscription))


def publish(project_id, topic_id, data):
    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project_id, topic_id)

    future = publisher.publish(topic_path, data=data.encode("utf-8"))
    print(future.result())


def receive(project_id, subscription_id, timeout=None):
    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(project_id, subscription_id)

    def callback(message):
        print("Received message: {}".format(message))
        message.ack()

    streaming_pull_future = subscriber.subscribe(subscription_path, callback=callback)
    print("Listening for messages on {}..\n".format(subscription_path))

    try:
        streaming_pull_future.result(timeout=timeout)
    except TimeoutError:
        streaming_pull_future.cancel()


if __name__ == "__main__":
    if sys.argv[1] == "create":
        create_topics_and_subscriptions(sys.argv[2], sys.argv[3])
    elif sys.argv[1] == "publish":
        publish(sys.argv[2], sys.argv[3], sys.argv[4])
    elif sys.argv[1] == "receive":
        receive(sys.argv[2], sys.argv[3])
    else:
        print("Unknown command {}".format(sys.argv[1]))
