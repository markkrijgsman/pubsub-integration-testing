package nl.luminis.articles.pubsub.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import nl.luminis.articles.pubsub.dto.DummyMessage;
import nl.luminis.articles.pubsub.publisher.DummyMessagePublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublishController {

    private final DummyMessagePublisher publisher;

    public PublishController(DummyMessagePublisher publisher) {
        this.publisher = publisher;
    }

    @ApiOperation(value = "Publish a new message for the subscriber to process")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Successfully published message")
    })
    @PostMapping("publish")
    public ResponseEntity<Void> publish(@RequestBody DummyMessage dummyMessage) {
        publisher.publish(dummyMessage);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
