package it.dontesta.eventbus.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PublishMessageOnEventBusTest {

  @Inject
  EventBus eventBus;

  @ConfigProperty(name = "app.eventbus.consumer.dispatcher.address")
  String dispatcherVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.http.request.address")
  String httpRequestVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.http.response.address")
  String httpResponseVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.addresses")
  List<String> eventHandlerVirtualAddresses;

  JsonObject requestMessage = new JsonObject().put("message",
      "Message Request to publish on the event bus");

  JsonObject responseMessage = new JsonObject().put("message",
      "Message Response to publish on the event");

  MessageConsumer<JsonObject> httpRequestConsumer;

  MessageConsumer<JsonObject> httpResponseConsumer;

  final String ASSERTION_MESSAGE = "The send message is not as expected";

  final String HTTP_REQUEST_VIRTUAL_ADDRESS_TEST = "http-request-test";

  final String HTTP_RESPONSE_VIRTUAL_ADDRESS_TEST = "http-response-test";

  final String SOURCE_VIRTUAL_ADDRESS = "source-virtual-address";

  /**
   * Source component header.
   *
   * È il nome dell'header che contiene il nome del componente sorgente
   * che ha inviato l'evento. Il nome del componente è in formato
   * <b>FQCN (Fully Qualified Class Name)</b>.
   */
  final String SOURCE_COMPONENT = "source-component";

  final String TARGET_VIRTUAL_ADDRESSES = "target-virtual-addresses";


  @Test
  void testPublishFakeHttpRequestMessageOnEventBus() {
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
        .onFailure().invoke(throwable -> fail(ASSERTION_MESSAGE)).await().indefinitely();
  }

  @Test
  void testPublishFakeHttpResponseMessageOnEventBus() {
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
        .onFailure().invoke(throwable -> fail(ASSERTION_MESSAGE)).await().indefinitely();
  }

  @Test
  void testPublishHttpRequestMessageOnEventBus() {
    // Registrazione del consumatore per l'indirizzo virtuale dell'evento
    httpRequestConsumer = eventBus.consumer(httpRequestVirtualAddress);

    // Registrazione del gestore per i messaggi ricevuti
    httpRequestConsumer.handler(message -> assertEquals(requestMessage, message.body()));

    // Publish the messages on the event bus for the HTTP request and response
    eventBus.publish(httpRequestVirtualAddress, requestMessage);
    assertTrue(true);
  }

  @Test
  void testPublishHttpResponseMessageOnEventBus() {
    // Registrazione del consumatore per l'indirizzo virtuale dell'evento
    httpResponseConsumer = eventBus.consumer(httpResponseVirtualAddress);

    // Registrazione del gestore per i messaggi ricevuti
    httpResponseConsumer.handler(message -> assertEquals(responseMessage, message.body()));

    // Publish the messages on the event bus for the HTTP request and response
    eventBus.publish(httpResponseVirtualAddress, responseMessage);
    assertTrue(true);
  }

  @Test
  void testPublishHttpRequestMessageWithFakeHeaderOnEventBus() {
    // Creare le opzioni di consegna desiderate
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(SOURCE_VIRTUAL_ADDRESS, "fakeSourceVirtualAddress")
        .addHeader(TARGET_VIRTUAL_ADDRESSES, String.join(",", eventHandlerVirtualAddresses))
        .addHeader(SOURCE_COMPONENT, "fakeSourceComponent");

    // Registrazione del consumatore per l'indirizzo virtuale dell'evento
    httpRequestConsumer = eventBus.consumer(dispatcherVirtualAddress);

    // Registrazione del gestore per i messaggi ricevuti
    httpRequestConsumer.handler(message -> assertEquals(requestMessage, message.body()));

    // Publish the messages on the event bus for the HTTP request and response
    // with the fake headers
    eventBus.publish(dispatcherVirtualAddress, requestMessage, options);
    assertTrue(true);
  }

  @AfterEach
  public void tearDown() {
    // De-registra i consumatori dell'evento bus
    if (httpRequestConsumer != null) {
      httpRequestConsumer.unregisterAndAwait();
    }

    if (httpResponseConsumer != null) {
      httpResponseConsumer.unregisterAndAwait();
    }
  }
}