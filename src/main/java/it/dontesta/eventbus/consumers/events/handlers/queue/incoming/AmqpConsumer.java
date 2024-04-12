package it.dontesta.eventbus.consumers.events.handlers.queue.incoming;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Questo componente consuma i messaggi dai canali AMQP http-request-in e http-response-in
 * utilizzando l'annotazione {@code @Incoming} per ricevere i messaggi.
 */
@ApplicationScoped
public class AmqpConsumer {

  @Inject
  Logger log;

  @Incoming("http-request-in")
  public CompletionStage<Void> consumeHttpRequest(Message<JsonObject> requestMessage) {
    // Implementa la logica per consumare il messaggio della richiesta HTTP
    log.debug("Received HTTP request message: " + requestMessage.getPayload());
    return requestMessage.ack();
  }

  @Incoming("http-response-in")
  public CompletionStage<Void> consumeHttpResponse(Message<JsonObject> requestMessage) {
    // Implementa la logica per consumare il messaggio della richiesta HTTP
    log.debug("Received HTTP response message: " + requestMessage.getPayload());
    return requestMessage.ack();
  }
}
