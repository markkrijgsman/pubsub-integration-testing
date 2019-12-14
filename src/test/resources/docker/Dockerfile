FROM google/cloud-sdk:272.0.0

# Yes, this is actually the Google recommended best practice:
# https://cloud.google.com/pubsub/docs/emulator
RUN git clone https://github.com/GoogleCloudPlatform/python-docs-samples.git /python-docs-samples && \
    cd /python-docs-samples/pubsub/cloud-client && \
    pip install -r requirements.txt

RUN mkdir -p /root/bin
COPY start-pubsub.sh pubsub-configuration-parser.py /root/bin/

EXPOSE 8432

CMD ["./root/bin/start-pubsub.sh"]