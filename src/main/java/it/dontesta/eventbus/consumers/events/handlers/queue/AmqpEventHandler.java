package it.dontesta.eventbus.consumers.events.handlers.queue;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import it.dontesta.eventbus.application.configuration.EventHandlerAddress;
import it.dontesta.eventbus.consumers.http.HttpRequestConsumer;
import it.dontesta.eventbus.consumers.http.HttpResponseConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

/**
 * Questo consumer si registra all'indirizzo virtuale dell'evento per il Event Handler specifico
 * per AMQP e gestisce gli eventi ricevuti dall'event bus tramite il metodo {@code handleEvent}.
 *
 * <p>Questo componente è responsabile di processare gli eventi/messaggi veicolati dal Dispatcher
 * tramite l'event bus e inviare il messaggio verso il brocker AMQP..
 * L'esito dell'operazione viene inviato come risposta al Dispatcher.
 *
 * <p>La registrazione avviene all'avvio dell'applicazione tramite l'annotazione {@code @Observes}
 * e il metodo {@code onStart}, mentre la de-registrazione avviene all'arresto dell'applicazione
 * attraverso il metodo {@code onStop}. La registrazione avviene solo se l'indirizzo dell'event handler
 * è abilitato.
 *
 * <p>L'indicazione dell'indirizzo virtuale dell'evento per l'Event Handler è definita nel parametro
 * di configurazione {@code app.eventbus.consumer.event.handler.addresses} che viene iniettato
 * tramite l'annotazione {@code @ConfigProperty}
 */
@ApplicationScoped
public class AmqpEventHandler {

  @Channel("http-request-out")
  Emitter<JsonObject> requestEmitter;

  @Channel("http-response-out")
  Emitter<JsonObject> responseEmitter;

  @Inject
  EventBus eventBus;

  @Inject
  Logger log;

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.addresses")
  List<EventHandlerAddress> eventHandlerAddresses;

  MessageConsumer<JsonObject> consumer;

  public static final String SOURCE_COMPONENT = "source-component";

  public static final String SOURCE_VIRTUAL_ADDRESS = "queue-trace";

  void onStart(@Observes StartupEvent ev) {

    boolean existsAndEnabled = eventHandlerAddresses.stream()
        .anyMatch(eventHandlerAddress ->
            eventHandlerAddress.getAddress().equals(SOURCE_VIRTUAL_ADDRESS) &&
            eventHandlerAddress.isEnabled());

    if (existsAndEnabled) {
      log.debugf("Registering the AMQP event handler at addresses: {%s}",
          SOURCE_VIRTUAL_ADDRESS);

      consumer = eventBus.consumer(SOURCE_VIRTUAL_ADDRESS);
      consumer.handler(this::handleEvent);
    }
  }

  void onStop(@Observes ShutdownEvent ev) {
    if (consumer != null) {
      consumer.unregisterAndAwait();
      log.debugf("Unregistering the AMQP event handler at addresses: {%s}",
          SOURCE_VIRTUAL_ADDRESS);
    }
  }

  /**
   * Metodo per gestire l'evento ricevuto dall'event bus.
   *
   * <p>Il metodo riceve il messaggio dell'evento e invia il messaggio alla coda AMQP
   *
   * @param message il messaggio dell'evento
   */
  public void handleEvent(Message<JsonObject> message) {
    // Recupera il componente sorgente dagli header del messaggio
    String sourceComponent = message.headers().get(SOURCE_COMPONENT);

    // Invia il messaggio alla coda AMQP per i messaggi di richiesta HTTP
    if (sourceComponent.equals(HttpRequestConsumer.class.getName())) {
      sendToQueue(message, requestEmitter);
    }

    // Invia il messaggio alla coda AMQP per i messaggi di risposta HTTP
    if (sourceComponent.equals(HttpResponseConsumer.class.getName())) {
      sendToQueue(message, responseEmitter);
    }
  }

  /**
   * Invio del messaggio alla coda AMQP e rimanere in attesa di una risposta
   * per confermare l'invio del messaggio e notificarlo al Dispatcher.
   *
   * @param message Il messaggio da inviare alla coda AMQP
   * @param emitter L'oggetto Emitter per inviare il messaggio alla coda AMQP
   */
  private void sendToQueue(Message<JsonObject> message,
                           Emitter<JsonObject> emitter) {
    emitter.send(message.body()).whenComplete((result, error) -> {
      if (error != null) {
        message.fail(1, error.getMessage());
      } else {
        message.reply("Message sent to AMQP queue successfully!");
      }
    });
  }
}
