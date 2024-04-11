package it.dontesta.eventbus.consumers.http;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Questo consumer si registra all'indirizzo virtuale dell'evento per la risposta HTTP e gestisce
 * gli eventi ricevuti dall'event bus tramite il metodo {@code handleEvent}.
 *
 * <p>La registrazione avviene all'avvio dell'applicazione tramite l'annotazione {@code @Observes}
 * e il metodo {@code onStart}.
 *
 * <p>L'indicazione dell'indirizzo virtuale dell'evento per la richiesta HTTP è definita nel
 * parametro di configurazione {@code app.eventbus.consumer.http.response.address} che viene
 * iniettato tramite l'annotazione {@code @ConfigProperty}.
 */

@ApplicationScoped
public class HttpResponseConsumer {

  @Inject
  EventBus eventBus;

  @Inject
  Logger log;

  @ConfigProperty(name = "app.eventbus.consumer.http.response.address")
  String httpResponseVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.dispatcher.address")
  String dispatcherVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.addresses")
  List<String> eventHandlerVirtualAddresses;

  public static final String SOURCE_VIRTUAL_ADDRESS = "source-virtual-address";

  public static final String SOURCE_COMPONENT = "source-component";

  public static final String TARGET_VIRTUAL_ADDRESSES = "target-virtual-addresses";

  void onStart(@Observes StartupEvent ev) {
    log.debugf(
        "Registering the consumers to the event bus for HTTP response at addresses: {%s}",
        httpResponseVirtualAddress);

    eventBus.consumer(httpResponseVirtualAddress, this::handleEvent);
  }

  // Method to handle the event
  public void handleEvent(Message<JsonObject> message) {
    // Creare le opzioni di consegna desiderate
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(TARGET_VIRTUAL_ADDRESSES, String.join(",", eventHandlerVirtualAddresses))
        .addHeader(SOURCE_VIRTUAL_ADDRESS, httpResponseVirtualAddress)
        .addHeader(SOURCE_COMPONENT, HttpRequestConsumer.class.getName());

    eventBus.publish(dispatcherVirtualAddress, message.body(), options);
  }
}
