package it.dontesta.eventbus.consumers.events.handlers.queue.incoming;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@QuarkusTest
class AmqpConsumerTest {

  @Mock
  Logger log;

  @Mock
  Message<JsonObject> message;

  @InjectMocks
  AmqpConsumer amqpConsumer;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testConsumeHttpRequest() {
    JsonObject jsonObject = new JsonObject();
    when(message.getPayload()).thenReturn(jsonObject);
    when(message.ack()).thenReturn(CompletableFuture.completedFuture(null));

    amqpConsumer.consumeHttpRequest(message);

    verify(log, times(1)).debug("Received HTTP request message: %s".formatted(jsonObject));
    verify(message, times(1)).ack();
  }

  @Test
  void testConsumeHttpResponse() {
    JsonObject jsonObject = new JsonObject();
    when(message.getPayload()).thenReturn(jsonObject);
    when(message.ack()).thenReturn(CompletableFuture.completedFuture(null));

    amqpConsumer.consumeHttpResponse(message);

    verify(log, times(1)).debug("Received HTTP response message: %s".formatted(jsonObject));
    verify(message, times(1)).ack();
  }
}