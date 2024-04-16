package it.dontesta.eventbus.publish;

import static org.junit.jupiter.api.Assertions.fail;

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

  final String ASSERTION_MESSAGE = "The send message is not as expected";

  public static final String HTTP_REQUEST_VIRTUAL_ADDRESS_TEST = "http-request-test";
  public static final String HTTP_RESPONSE_VIRTUAL_ADDRESS_TEST = "http-response-test";

  @Test
  void testPublishFakeHttpRequestMessageOnEventBus() {

    JsonObject requestMessage = new JsonObject().put("message",
        "Message to publish on the event bus {virtualAddress: %s}".formatted(
            httpRequestVirtualAddress));

    // Register the consumers to the event bus for HTTP request and response
    // at the test addresses.
    // Test the event bus consumers by publishing messages on the event bus
    // to verify that the received message body is the same as the published message.
    eventBus.consumer(HTTP_REQUEST_VIRTUAL_ADDRESS_TEST, message -> {
      if (message.body().equals(requestMessage)) {
        message.reply("OK");
      } else {
        message.fail(1, "KO");
      }
    });

    // Invia il messaggio in modo sincrono e attendi una risposta
    eventBus.request(HTTP_REQUEST_VIRTUAL_ADDRESS_TEST, requestMessage)
        .onFailure().invoke(throwable -> {
          fail(ASSERTION_MESSAGE);
        }).await().indefinitely();
  }

  @Test
  void testPublishFakeHttpResponseMessageOnEventBus() {

    JsonObject responseMessage = new JsonObject().put("message",
        "Message to publish on the event {virtualAddress: %s}".formatted(
            httpResponseVirtualAddress));

    // Register the consumers to the event bus for HTTP request and response
    // at the test addresses.
    // Test the event bus consumers by publishing messages on the event bus
    // to verify that the received message body is the same as the published message.
    eventBus.consumer(HTTP_RESPONSE_VIRTUAL_ADDRESS_TEST, message -> {
      if (message.body().equals(responseMessage)) {
        message.reply("OK");
      } else {
        message.fail(1, "KO");
      }
    });

    // Invia il messaggio in modo sincrono e attendi una risposta
    eventBus.request(HTTP_RESPONSE_VIRTUAL_ADDRESS_TEST, responseMessage)
        .onFailure().invoke(throwable -> {
          fail(ASSERTION_MESSAGE);
        }).await().indefinitely();
  }

  @Test
  void testPublishHttpRequestMessageOnEventBus() {

    JsonObject requestMessage = new JsonObject().put("message",
        "Message to publish on the event bus {virtualAddress: %s}".formatted(
            httpRequestVirtualAddress));

    // Publish the messages on the event bus for the HTTP request and response
    eventBus.publish(httpRequestVirtualAddress, requestMessage);
  }

  @Test
    void testPublishHttpResponseMessageOnEventBus() {

        JsonObject responseMessage = new JsonObject().put("message",
            "Message to publish on the event {virtualAddress: %s}".formatted(
                httpResponseVirtualAddress));

        // Publish the messages on the event bus for the HTTP request and response
        eventBus.publish(httpResponseVirtualAddress, responseMessage);
    }
}