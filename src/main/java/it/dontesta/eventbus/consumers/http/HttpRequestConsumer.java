package it.dontesta.eventbus.consumers.http;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Questo consumer si registra all'indirizzo virtuale dell'evento per la richiesta HTTP e gestisce
 * gli eventi ricevuti dall'event bus tramite il metodo {@code handleEvent}.
 *
 * <p>La registrazione avviene all'avvio dell'applicazione tramite l'annotazione {@code @Observes}
 * e il metodo {@code onStart}, mentre la de-registrazione avviene all'arresto dell'applicazione
 * attraverso il metodo {@code onStop}.
 *
 * <p>L'indicazione dell'indirizzo virtuale dell'evento per la richiesta HTTP è definita nel
 * parametro di configurazione {@code app.eventbus.consumer.http.request.address} che viene
 * iniettato tramite l'annotazione {@code @ConfigProperty}.
 */
@ApplicationScoped
public class HttpRequestConsumer {

  @Inject
  EventBus eventBus;

  @Inject
  Logger log;

  @ConfigProperty(name = "app.eventbus.consumer.http.request.address")
  String httpRequestVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.dispatcher.address")
  String dispatcherVirtualAddress;

  MessageConsumer<JsonObject> consumer;

  /**
   * Source virtual address header.
   * È il nome dell'header che contiene l'indirizzo virtuale del consumer che ha inviato l'evento.
   */
  public static final String SOURCE_VIRTUAL_ADDRESS = "source-virtual-address";

  /**
   * Source component header.
   * È il nome dell'header che contiene il nome del componente sorgente
   * che ha inviato l'evento. Il nome del componente è in formato
   * <b>FQCN (Fully Qualified Class Name)</b>.
   */
  public static final String SOURCE_COMPONENT = "source-component";

  void onStart(@Observes StartupEvent ev) {
    log.debugf(
        "Registering the consumers to the event bus for HTTP request at addresses: {%s}",
        httpRequestVirtualAddress);

    consumer = eventBus.consumer(httpRequestVirtualAddress);
    consumer.handler(this::handleEvent);
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (consumer != null) {
      consumer.unregisterAndAwait();
      log.debugf(
          "Unregistering the consumers to the event bus for HTTP request at addresses: {%s}",
          httpRequestVirtualAddress);
    }
  }

  /**
   * Metodo per gestire l'evento ricevuto dall'event bus.
   *
   * <p>Il metodo riceve il messaggio dell'evento e invia il messaggio al Dispatcher per la
   * consegna ai vari Event Handler.
   *
   * @param message il messaggio dell'evento
   */
  public void handleEvent(Message<JsonObject> message) {
    // Creare le opzioni di consegna desiderate
    DeliveryOptions options = new DeliveryOptions()
        .addHeader(SOURCE_VIRTUAL_ADDRESS, httpRequestVirtualAddress)
        .addHeader(SOURCE_COMPONENT, HttpRequestConsumer.class.getName());

    eventBus.publish(dispatcherVirtualAddress, message.body(), options);
  }
}
