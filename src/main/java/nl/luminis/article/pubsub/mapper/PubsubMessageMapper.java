package nl.luminis.article.pubsub.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class PubsubMessageMapper implements Function<Object, PubsubMessage> {

    private final ObjectMapper mapper;

    public PubsubMessageMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PubsubMessage apply(Object pojo) {
        try {
            byte[] message = mapper.writeValueAsBytes(pojo);

            return PubsubMessage
                .newBuilder()
                .setData(ByteString.copyFrom(message))
                .build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize message", e);
        }
    }
}
