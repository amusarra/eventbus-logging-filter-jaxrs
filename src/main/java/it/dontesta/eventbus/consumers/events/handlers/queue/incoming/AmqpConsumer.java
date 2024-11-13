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
 *
 * @author Antonio Musarra
 */
@ApplicationScoped
public class AmqpConsumer {

  @Inject
  Logger log;

  /**
   * Questo metodo è un consumer per i messaggi in arrivo dal canale AMQP http-request-in.
   *
   * @param requestMessage Il messaggio in arrivo dal canale AMQP http-request-in
   * @return Una CompletionStage che rappresenta il completamento del consumo del messaggio
   */
  @Incoming("http-request-in")
  public CompletionStage<Void> consumeHttpRequest(Message<JsonObject> requestMessage) {
    // Implementa la logica per consumare il messaggio della richiesta HTTP
    log.debug("Received HTTP request message: %s".formatted(requestMessage.getPayload()));
    return requestMessage.ack();
  }

  /**
   * Questo metodo è un consumer per i messaggi in arrivo dal canale AMQP http-response-in.
   *
   * @param requestMessage Il messaggio in arrivo dal canale AMQP http-response-in
   * @return Una CompletionStage che rappresenta il completamento del consumo del messaggio
   */
  @Incoming("http-response-in")
  public CompletionStage<Void> consumeHttpResponse(Message<JsonObject> requestMessage) {
    // Implementa la logica per consumare il messaggio della richiesta HTTP
    log.debug("Received HTTP response message: %s".formatted(requestMessage.getPayload()));
    return requestMessage.ack();
  }
}
