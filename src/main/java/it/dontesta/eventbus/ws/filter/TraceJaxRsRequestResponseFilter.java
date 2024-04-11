package it.dontesta.eventbus.ws.filter;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Questo filtro JAX-RS è un esempio di come implementare un filtro per le richieste e le risposte
 * HTTP in un'applicazione che utilizza Jakarta EE e MicroProfile.
 *
 * <p>Gli extension point sono rappresentati dalle interfacce {@link ContainerRequestFilter} e
 * {@link ContainerResponseFilter} e questa classe implementa entrambe le interfacce.
 *
 * <p>Il filtro è annotato con {@link Provider} per essere riconosciuto come filtro JAX-RS.
 *
 * <p>La priorità del filtro è impostata a {@link Priorities#USER} per garantire che venga eseguito
 * dopo i filtri predefiniti (ad esempio, {@link Priorities#AUTHENTICATION}).
 *
 * <p>I messaggi di request e response sono elaborati solo se il parametro di configurazione
 * {@code app.filter.enabled} e impostato a {@code true}, per default è {@code false}.
 *
 * <p>L'elenco delle URI da filtrare è definito nel parametro di configurazione
 * {@code app.filter.uris}.
 *
 * <p>Per ulteriori informazioni sui filtri JAX-RS, vedere la specifica Jakarta EE:
 * https://jakarta.ee/specifications/restful-ws/3.1/jakarta-restful-ws-spec-3.1.html#filters
 */
@Provider
@Priority(Priorities.USER)
@ApplicationScoped
public class TraceJaxRsRequestResponseFilter implements ContainerRequestFilter,
    ContainerResponseFilter {

  @Inject
  EventBus eventBus;

  @Inject
  Logger log;

  @Context
  UriInfo uriInfo;

  @Context
  RoutingContext routingContext;

  @ConfigProperty(name = "app.filter.enabled", defaultValue = "false")
  boolean filterEnabled;

  @ConfigProperty(name = "app.filter.uris")
  List<String> uris;

  @ConfigProperty(name = "app.eventbus.consumer.http.request.address")
  String httpRequestVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.http.response.address")
  String httpResponseVirtualAddress;

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  private static final String LOCAL_DATE_TIME_IN = "local-date-time-in";

  private static final String LOCAL_DATE_TIME_OUT = "local-date-time-out";

  private static final String COOKIE_USER_TRACKING_NAME = "user_tracking_id";

  @Override
  public void filter(ContainerRequestContext requestContext) {
    // Se il filtro non è abilitato, esci
    if (!filterEnabled) {
      return;
    }

    // Ottieni l'URI della richiesta
    String requestUri = uriInfo.getRequestUri().getPath();

    // Genera l'ID di correlazione dalla richiesta
    String correlationId = getCorrelationId(requestContext.getHeaderString(CORRELATION_ID_HEADER));

    // Aggiungi l'ID di correlazione alla richiesta
    requestContext.setProperty(CORRELATION_ID_HEADER, correlationId);

    // Applica la logica del filtro in base all'URI
    if (requestUriIsFiltered(requestUri)) {
      // Aggiungi la data ora di quando la richiesta arriva al filtro
      requestContext.setProperty(LOCAL_DATE_TIME_IN, LocalDateTime.now());

      /*
       * Se l'URI richiesto è presente nell'elenco delle URI da filtrare
       * prepara e invia il messaggio della richiesta verso la destinazione
       * dell\'Event Bus.
       */
      eventBus.publish(httpRequestVirtualAddress, prepareMessage(requestContext));

      log.debug("Pubblicazione del messaggio della richiesta HTTP su Event Bus");
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext,
                     ContainerResponseContext responseContext) {
    // Se il filtro non è abilitato, esci
    if (!filterEnabled) {
      return;
    }

    // Ottieni l'URI della richiesta
    String requestUri = uriInfo.getRequestUri().getPath();

    // Recupera l'ID di correlazione dalla richiesta
    String correlationId =
        getCorrelationId((String) requestContext.getProperty(CORRELATION_ID_HEADER));

    // Aggiungi l'ID di correlazione alla risposta
    responseContext.getHeaders().add(CORRELATION_ID_HEADER, correlationId);

    // Aggiungi il cookie alla risposta HTTP
    setCookieUserTracing(requestContext, responseContext);

    // Applica la logica del filtro in base all'URI
    if (requestUriIsFiltered(requestUri)) {
      /*
       * Se l'URI richiesto è presente nell'elenco delle URI da filtrare
       * prepara e invia il messaggio della richiesta verso la destinazione
       * dell\'Event Bus.
       */
      eventBus.publish(httpResponseVirtualAddress, prepareMessage(responseContext));

      log.debug("Pubblicazione del messaggio della risposta HTTP su Event Bus");
    }
  }

  /**
   * Ottiene l'ID di correlazione.
   * Se l'ID di correlazione è nullo, genera un nuovo ID di correlazione.
   * Questo metodo è utilizzato per garantire che l'ID di correlazione sia sempre presente,
   * sia nella richiesta che nella risposta. Il formato dell'ID di correlazione è un UUID.
   *
   * @param correlationId L'ID di correlazione
   * @return L'ID di correlazione
   */
  private String getCorrelationId(String correlationId) {
    // Genera un nuovo ID di correlazione se quello attuale è nullo
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }
    return correlationId;
  }

  /**
   * Conversione degli header della risposta in una mappa di stringhe
   * rispetto all'oggetto {@link jakarta.ws.rs.core.MultivaluedHashMap}  restituito da getHeaders().
   *
   * @param responseContext Il contesto della risposta
   * @return La mappa degli header della risposta
   */
  private Map<String, String> getResponseHeaders(ContainerResponseContext responseContext) {
    Map<String, String> stringMap = new HashMap<>();
    for (Map.Entry<String, List<Object>> entry : responseContext.getHeaders().entrySet()) {
      String key = entry.getKey();
      List<Object> values = entry.getValue();
      StringBuilder valueBuilder = new StringBuilder();
      for (Object value : values) {
        if (value != null) {
          if (valueBuilder.length() > 0) {
            valueBuilder.append(", ");
          }
          valueBuilder.append(value);
        }
      }
      stringMap.put(key, valueBuilder.toString());
    }
    return stringMap;
  }

  /**
   * Ottieni il corpo della richiesta.
   * Questo metodo legge il corpo della richiesta e lo memorizza in un contenitore appropriato.
   * Il corpo della richiesta viene memorizzato in un oggetto appropriato per l'accesso successivo
   * e l'entità della richiesta viene ricollegata all'input stream originale.
   * Questo è necessario perché una volta che il corpo della richiesta è letto, l'input stream
   * non può essere letto nuovamente.
   *
   * @param requestContext Il contesto della richiesta
   * @return Il corpo della richiesta
   * @throws IOException Se si verifica un errore durante la lettura del corpo della richiesta
   */
  private String getRequestBody(ContainerRequestContext requestContext) throws IOException {
    // Leggi il corpo della richiesta e memorizzalo in un contenitore appropriato
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream in = requestContext.getEntityStream()) {
      int bytesRead;
      byte[] data = new byte[1024];
      while ((bytesRead = in.read(data)) != -1) {
        buffer.write(data, 0, bytesRead);
      }
    }

    // Memorizza il corpo della richiesta in un oggetto appropriato per l'accesso successivo
    requestContext.setProperty("requestBody", buffer.toByteArray());

    // Ricollega l'entità della richiesta all'input stream originale
    ByteArrayInputStream input = new ByteArrayInputStream(buffer.toByteArray());
    requestContext.setEntityStream(input);

    return buffer.toString("UTF-8");
  }

  /**
   * Prepara il messaggio della richiesta in formato JSON per l'invio all'Event Bus.
   *
   * <p>Il messaggio restituito contiene le informazioni relative alla richiesta HTTP
   * come URI, headers, corpo, metodo, media-type, lingua accettata, ecc.
   *
   * <p>È in formato {@link JsonObject} per essere inviato direttamente all'Event Bus.
   *
   * @param requestContext Il contesto della richiesta
   * @return JsonObject Il messaggio della richiesta in formato JSON
   */
  private JsonObject prepareMessage(ContainerRequestContext requestContext) {
    JsonObject jsonObject;
    try {
      jsonObject = new JsonObject()
          .put(CORRELATION_ID_HEADER, requestContext.getProperty(CORRELATION_ID_HEADER))
          .put("remote-ip-address", routingContext.request().remoteAddress().host())
          .put("headers", requestContext.getHeaders())
          .put("body", getRequestBody(requestContext))
          .put("uri-info", requestContext.getUriInfo().getRequestUri().toString())
          .put(LOCAL_DATE_TIME_IN, requestContext.getProperty(LOCAL_DATE_TIME_IN).toString())
          .put("method", requestContext.getMethod())
          .put("media-type", "%s/%s".formatted(requestContext.getMediaType().getType(),
              requestContext.getMediaType().getSubtype()))
          .put("acceptable-language", requestContext.getAcceptableLanguages().toString())
          .put("acceptable-media-types", requestContext.getAcceptableMediaTypes().toString());
    } catch (IOException ioException) {
      log.error("Errore nella generazione del JSON dal requestContext object");
      throw new RuntimeException(ioException);
    }

    return jsonObject;
  }

  /**
   * Prepara il messaggio della risposta in formato JSON per l'invio all'Event Bus.
   *
   * <p>Il messaggio restituito contiene le informazioni relative alla risposta HTTP
   * come status, headers, corpo, ecc.
   *
   * <p>È in formato {@link JsonObject} per essere inviato direttamente all'Event Bus.
   *
   * @param responseContext Il contesto della risposta
   * @return JsonObject Il messaggio della risposta in formato JSON
   */
  private JsonObject prepareMessage(ContainerResponseContext responseContext) {
    JsonObject jsonObject = new JsonObject()
        .put(CORRELATION_ID_HEADER,
            responseContext.getHeaders().get(CORRELATION_ID_HEADER).getFirst())
        .put(LOCAL_DATE_TIME_OUT, LocalDateTime.now().toString())
        .put("status", responseContext.getStatus())
        .put("status-info-family-name", responseContext.getStatusInfo().getFamily().name())
        .put("status-info-reason", responseContext.getStatusInfo().getReasonPhrase())
        .put("headers", getResponseHeaders(responseContext))
        .put("body", responseContext.getEntity().toString());

    return jsonObject;
  }

  /**
   * Verifica se la Request URI è tra quelle che devono essere filtrate.
   * Il parametro di configurazione app.filter.uris contiene l'elenco delle URI.
   *
   * @param requestUri La Request URI da verificare
   * @return true se la Request URI è tra quelle che devono essere filtrate, false altrimenti
   */
  private boolean requestUriIsFiltered(String requestUri) {
    log.debugf("La Request URI %s è tra quelle che devono essere filtrate", requestUri);

    return uris.stream().anyMatch(item -> requestUri.startsWith(item));
  }

  private void setCookieUserTracing(ContainerRequestContext requestContext,
                                    ContainerResponseContext responseContext) {
    String trackingCode = UUID.randomUUID().toString();

    // Verifica se esiste già un cookie di tracciamento nella richiesta
    Cookie trackingCookie = requestContext.getCookies().get(COOKIE_USER_TRACKING_NAME);
    if (trackingCookie == null) {
      // Imposta il cookie di tracciamento utente sulla risposta
      NewCookie newTrackingCookie = new NewCookie(COOKIE_USER_TRACKING_NAME, trackingCode,
          "/",
          null,
          NewCookie.DEFAULT_VERSION,
          "Cookie di tracciamento dell'utente",
          (int) TimeUnit.DAYS.toSeconds(30),
          false);

      responseContext.getHeaders().add("Set-Cookie", newTrackingCookie.toString());
    }
  }
}
