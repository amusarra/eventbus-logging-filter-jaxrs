package it.dontesta.eventbus.consumers.events.handlers;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import it.dontesta.eventbus.application.configuration.EventHandlerAddress;
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
 * consumer target che sono stati abilitati da configurazione
 * {@code app.eventbus.consumer.event.handler.addresses}. Questi consumer target sono i cosiddetti
 * event handler.
 *
 * <p>La registrazione avviene all'avvio dell'applicazione tramite l'annotazione {@code @Observes}
 * e il metodo {@code onStart}.
 *
 * <p>L'indicazione dell'indirizzo virtuale dell'evento per il dispatcher è definita nel parametro
 * di configurazione {@code app.eventbus.consumer.dispatcher.address} che viene iniettato tramite
 * l'annotazione {@code @ConfigProperty}
 *
 * <p>In questo caso il dispatcher invia l'evento ricevuto a uno o più consumer target configurati
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

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.addresses")
  List<EventHandlerAddress> eventHandlerVirtualAddresses;

  MessageConsumer<JsonObject> consumer;

  public static final String SOURCE_VIRTUAL_ADDRESS = "source-virtual-address";

  public static final String SOURCE_COMPONENT = "source-component";

  public static final String TARGET_VIRTUAL_ADDRESSES = "target-virtual-addresses";

  void onStart(@Observes StartupEvent ev) {
    log.debugf(
        "Registering the Dispatcher to the event bus for the event handler at addresses: {%s}",
        dispatcherVirtualAddress);

    consumer = eventBus.consumer(dispatcherVirtualAddress);
    consumer.handler(this::handleEvent);
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (consumer != null) {
      consumer.unregisterAndAwait();
      log.debugf(
          "Unregistering the Dispatcher from the event bus for the event handler at addresses:{%s}",
          dispatcherVirtualAddress);
    }
  }

  /**
   * Method to handle the event.
   *
   * @param message The message received from the event bus
   */
  public void handleEvent(Message<JsonObject> message) {
    // Leggere gli header dalle DeliveryOptions
    String sourceVirtualAddress = message.headers().get(SOURCE_VIRTUAL_ADDRESS);
    String sourceComponent = message.headers().get(SOURCE_COMPONENT);

    // Filtra gli indirizzi degli event handler abilitati
    List<EventHandlerAddress> enabledEventHandlerAddresses = eventHandlerVirtualAddresses.stream()
        .filter(EventHandlerAddress::isEnabled)
        .toList();

    // Estrai la lista degli indirizzi virtuali di destinazione
    List<String> targetVirtualAddressesList = enabledEventHandlerAddresses.stream()
        .map(EventHandlerAddress::getAddress)
        .toList();

    log.debugf(
        new StringBuilder().append(
                "Received event message from source virtual address: %s and source component: %s ")
            .append("for the target virtual addresses: %s").toString(),
        sourceVirtualAddress, sourceComponent, targetVirtualAddressesList);

    // Invia l'evento a tutti i target virtual addresses
    // che sono stati abilitati da configurazione app.eventbus.consumer.event.handler.addresses
    targetVirtualAddressesList.forEach(targetVirtualAddress -> {

      // Creare le opzioni di consegna desiderate
      DeliveryOptions options = new DeliveryOptions()
          .addHeader(SOURCE_VIRTUAL_ADDRESS, sourceVirtualAddress)
          .addHeader(SOURCE_COMPONENT, sourceComponent);

      log.debugf("Sending event message to target virtual address: %s", targetVirtualAddress);

      Uni<String> response = eventBus.<String>request(targetVirtualAddress, message.body(), options)
          .onItem().transform(Message::body);

      response.subscribe().with(
          result -> log.debugf("Received response from target virtual address: %s with result: %s",
              targetVirtualAddress, result),
          failure -> log.errorf(
              "Failed to receive response from target virtual address: %s with failure: %s",
              targetVirtualAddress, failure)
      );
    });
  }
}
