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
