package it.dontesta.eventbus.consumers.events.handlers.nosql;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import it.dontesta.eventbus.consumers.http.HttpRequestConsumer;
import it.dontesta.eventbus.consumers.http.HttpResponseConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Questo consumer si registra all'indirizzo virtuale dell'evento per il Event Handler specifico
 * per MongoDB e gestisce gli eventi ricevuti dall'event bus tramite il metodo {@code handleEvent}.
 *
 * <p>Questo componente è responsabile di processare gli eventi/messaggi veicolati dal Dispatcher
 * tramite l'event bus e inserire il documento MongoDB nel database specificato.
 * L'esito dell'operazione viene inviato come risposta al Dispatcher.
 *
 * <p>La registrazione avviene all'avvio dell'applicazione tramite l'annotazione {@code @Observes}
 * e il metodo {@code onStart}.
 *
 * <p>L'indicazione dell'indirizzo virtuale dell'evento per l'Event Handler è definita nel parametro
 * di configurazione {@code app.eventbus.consumer.event.handler.addresses[1]} che viene iniettato
 * tramite l'annotazione {@code @ConfigProperty}
 */
@ApplicationScoped
public class MongoDbEventHandler {

  @Inject
  EventBus eventBus;

  @Inject
  Logger log;

  @Inject
  ReactiveMongoClient mongoClient;

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.addresses[1]")
  String mongoDbEventHandlerVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.nosql.mongodb.database.name",
      defaultValue = "audit")
  String databaseName;

  @ConfigProperty(name = "app.eventbus.consumer.event.handler.nosql.mongodb.database.collection",
      defaultValue = "jax-rs-requests")
  String databaseCollectionName;

  public static final String SOURCE_COMPONENT = "source-component";

  void onStart(@Observes StartupEvent ev) {
    log.debugf(
        "Registering the MongoDB event handler at addresses: {%s}",
        mongoDbEventHandlerVirtualAddress);

    eventBus.consumer(mongoDbEventHandlerVirtualAddress, this::handleEvent);
  }

  // Method to handle the event
  public void handleEvent(Message<JsonObject> message) {
    // Recupera il componente sorgente dagli header del messaggio
    // e il corpo del messaggio stesso in formato JsonObject
    String sourceComponent = message.headers().get(SOURCE_COMPONENT);
    JsonObject body = message.body();

    // Crea un documento MongoDB a partire dal messaggio dell'evento
    Document mongoDbDocument = getMongoDbDocument(sourceComponent, body);

    if (mongoDbDocument == null || mongoDbDocument.isEmpty()) {
      message.fail(1, "Could not create a MongoDB document from the event message.");
      return;
    }

    // Inserisci il documento MongoDB nel database
    getCollection().insertOne(mongoDbDocument).subscribe().with(
        result -> message.reply(
            "Documents inserted successfully with Id %s".formatted(result.getInsertedId())),
        failure -> message.fail(-1, "Errors occurred while inserting the document.")
    );
  }

  /**
   * Metodo per ottenere la collezione MongoDB specificata nel parametro di configurazione
   * {@code app.eventbus.consumer.event.handler.nosql.mongodb.database.collection}.
   *
   * @return la collezione MongoDB specificata
   */
  private ReactiveMongoCollection<Document> getCollection() {
    return mongoClient.getDatabase(databaseName).getCollection(databaseCollectionName);
  }

  /**
   * Metodo per creare un documento MongoDB a partire dal messaggio dell'evento.
   *
   * <p>In questo caso si considerano solo due componenti sorgente: HttpRequestConsumer e
   * HttpResponseConsumer tramite il parametro sourceComponent
   * restituendo un documento MongoDB creato con la stessa struttura.
   *
   * @param sourceComponent il componente sorgente dell'evento
   * @param jsonObject      il messaggio dell'evento
   * @return il documento MongoDB creato
   */
  private Document getMongoDbDocument(String sourceComponent, JsonObject jsonObject) {
    if (sourceComponent.equals(HttpRequestConsumer.class.getName())) {
      return Document.parse(jsonObject.encode());
    }

    if (sourceComponent.equals(HttpResponseConsumer.class.getName())) {
      return Document.parse(jsonObject.encode());
    }

    return new Document();
  }

}
