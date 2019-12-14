package nl.luminis.articles.pubsub.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import nl.luminis.articles.pubsub.dto.DummyMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PubsubMessageMapperTest {

    @InjectMocks
    private PubsubMessageMapper pubsubMessageMapper;
    @Mock
    private ObjectMapper mapper;

    @Test
    public void testApply() throws IOException {
        DummyMessage message = DummyMessage.builder().id(1L).message("message").build();
        byte[] bytes = "test".getBytes();

        when(mapper.writeValueAsBytes(message)).thenReturn(bytes);

        PubsubMessage pubsubMessage = pubsubMessageMapper.apply(message);

        assertThat(pubsubMessage.getData()).isEqualTo(ByteString.copyFrom(bytes));
    }
}
