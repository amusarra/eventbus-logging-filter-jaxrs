package it.dontesta.eventbus.consumers.events.handlers;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
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
 * Questo consumer si registra all'indirizzo virtuale dell'evento per il dispatcher e gestisce
 * gli eventi ricevuti dall'event bus tramite il metodo {@code handleEvent}.
 *
 * <p>Questo componente è responsabile di inviare gli eventi ricevuti da un consumer a uno o più
 * consumer target specificati nell'header {@TARGET_VIRTUAL_ADDRESSES} del messaggio.
 *
 * <p>La registrazione avviene all'avvio dell'applicazione tramite l'annotazione {@code @Observes}
 * e il metodo {@code onStart}.
 *
 * <p>L'indicazione dell'indirizzo virtuale dell'evento per il dispatcher è definita nel parametro
 * di configurazione {@code app.eventbus.consumer.dispatcher.address} che viene iniettato tramite
 * l'annotazione {@code @ConfigProperty}
 *
 * <p>In questo caso il dispatcher invia l'evento ricevuto a uno o più consumer target specificati
 * attendendo una risposta da ciascuno di essi scrivendo il risultato sul log.
 */
@ApplicationScoped
public class Dispatcher {
  @Inject
  EventBus eventBus;

  @Inject
  Logger log;

  @ConfigProperty(name = "app.eventbus.consumer.dispatcher.address")
  String dispatcherVirtualAddress;

  public static final String SOURCE_VIRTUAL_ADDRESS = "source-virtual-address";

  public static final String SOURCE_COMPONENT = "source-component";

  public static final String TARGET_VIRTUAL_ADDRESSES = "target-virtual-addresses";

  void onStart(@Observes StartupEvent ev) {
    log.debugf(
        "Registering the dispatcher to the event bus for the event handler at addresses: {%s}",
        dispatcherVirtualAddress);

    eventBus.consumer(dispatcherVirtualAddress, this::handleEvent);
  }

  // Method to handle the event
  public void handleEvent(Message<JsonObject> message) {
    // Leggere gli header dalle DeliveryOptions
    String sourceVirtualAddress = message.headers().get(SOURCE_VIRTUAL_ADDRESS);
    String sourceComponent = message.headers().get(SOURCE_COMPONENT);
    List<String> targetVirtualAddressesList =
        List.of(message.headers().get(TARGET_VIRTUAL_ADDRESSES).split(","));

    log.debugf(
        new StringBuilder().append(
                "Received event message from source virtual address: %s and source component: %s ")
            .append("for the target virtual addresses: %s").toString(),
        sourceVirtualAddress, sourceComponent, message.headers().get(TARGET_VIRTUAL_ADDRESSES));

    // Invia l'evento a tutti i target virtual addresses
    targetVirtualAddressesList.forEach(targetVirtualAddress -> {
      log.debugf("Sending event message to target virtual address: %s", targetVirtualAddress);

      Uni<String> response = eventBus.<String>request(targetVirtualAddress, message.body())
          .onItem().transform(Message::body);

      response.subscribe().with(
          result -> {
            log.debugf("Received response from target virtual address: %s with result: %s",
                targetVirtualAddress, result);
          },
          failure -> {
            log.errorf(
                "Failed to receive response from target virtual address: %s with failure: %s",
                targetVirtualAddress, failure);
          }
      );
    });
  }
}
