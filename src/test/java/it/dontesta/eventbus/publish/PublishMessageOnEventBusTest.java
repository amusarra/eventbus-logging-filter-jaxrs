package it.dontesta.eventbus.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PublishMessageOnEventBusTest {

  @Inject
  EventBus eventBus;

  @ConfigProperty(name = "app.eventbus.consumer.http.request.address")
  String httpRequestVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.http.response.address")
  String httpResponseVirtualAddress;

  public static final String HTTP_REQUEST_VIRTUAL_ADDRESS_TEST = "http-request-test";
  public static final String HTTP_RESPONSE_VIRTUAL_ADDRESS_TEST = "http-response-test";

  @Test
  void testPublishMessageOnEventBus() {
    JsonObject requestMessage = new JsonObject().put("message",
        "Message to publish on the event bus {virtualAddress: %s}".formatted(
            httpRequestVirtualAddress));
    JsonObject responseMessage = new JsonObject().put("message",
        "Message to publish on the event {virtualAddress: %s}".formatted(
            httpResponseVirtualAddress));

    // Register the consumers to the event bus for HTTP request and response
    // at the test addresses.
    // Test the event bus consumers by publishing messages on the event bus
    // to verify that the received message body is the same as the published message.
    eventBus.consumer(HTTP_REQUEST_VIRTUAL_ADDRESS_TEST, message -> {
      assertEquals(requestMessage, message.body());
    });
    eventBus.consumer(HTTP_RESPONSE_VIRTUAL_ADDRESS_TEST, message -> {
      assertEquals(responseMessage, message.body());
    });

    // Publish the messages on the event bus for the HTTP request and response
    // for the test addresses.
    eventBus.publish(HTTP_REQUEST_VIRTUAL_ADDRESS_TEST, requestMessage);
    eventBus.publish(HTTP_RESPONSE_VIRTUAL_ADDRESS_TEST, responseMessage);

    // Publish the messages on the event bus for the HTTP request and response
    eventBus.publish(httpRequestVirtualAddress, requestMessage);
    eventBus.publish(httpResponseVirtualAddress, responseMessage);
  }
}