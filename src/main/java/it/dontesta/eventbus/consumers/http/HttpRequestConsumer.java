package it.dontesta.eventbus.consumers.http;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
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
 * e il metodo {@code onStart}.
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

  void onStart(@Observes StartupEvent ev) {
    log.debugf(
        "Registering the consumers to the event bus for HTTP request at addresses: {%s}",
        httpRequestVirtualAddress);

    eventBus.consumer(httpRequestVirtualAddress, this::handleEvent);
  }

  // Method to handle the event
  public void handleEvent(Message<JsonObject> message) {
    log.debug("Received HTTP request message: " + message.body());
  }
}
