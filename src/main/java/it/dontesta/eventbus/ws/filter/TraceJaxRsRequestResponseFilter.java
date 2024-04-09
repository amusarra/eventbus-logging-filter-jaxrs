package it.dontesta.eventbus.ws.filter;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Questo filtro JAX-RS è un esempio di come implementare un filtro per le richieste e le risposte
 * HTTP in un'applicazione che utilizza Jakarta EE e MicroProfile.
 *
 * <p>Gli extension point sono rappresentati dalle interfacce {@link ContainerRequestFilter} e
 * {@link ContainerResponseFilter} e questa classe implementa entrambe le interfacce.
 *
 * <p>Il filtro è annotato con {@link Provider} e {@link ApplicationScoped} per essere riconosciuto
 * come un componente CDI.
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
  Logger log;

  @Inject
  UriInfo uriInfo;

  @ConfigProperty(name = "app.filter.enabled", defaultValue = "false")
  boolean filterEnabled;

  @ConfigProperty(name = "app.filter.uris")
  List<String> uris;

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  public void filter(ContainerRequestContext requestContext) {
    // Se il filtro non è abilitato, esci
    if (!filterEnabled) {
      return;
    }

    // Ottieni l'URI della richiesta
    String requestUri = uriInfo.getRequestUri().getPath();

    String correlationId = getCorrelationId(requestContext.getHeaderString(CORRELATION_ID_HEADER));

    // Aggiungi l'ID di correlazione alla richiesta
    requestContext.setProperty(CORRELATION_ID_HEADER, correlationId);

    // Applica la logica del filtro in base all'URI
    if (requestUriIsFiltered(requestUri)) {
      /*
        @TODO: Se l'URI richiesto è presente nell'elenco delle URI da filtrare
       * prepara e invia il messaggio della richiesta verso la destinazione
       * dell\'Event Bus.
       */
      log.debug("Pubblicazione del messaggio della richiesta HTTP sull\\'Event Bus");
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

    // Applica la logica del filtro in base all'URI
    if (requestUriIsFiltered(requestUri)) {
      /*
        @TODO: Se l'URI richiesto è presente nell'elenco delle URI da filtrare
       * prepara e invia il messaggio della richiesta verso la destinazione
       * dell\'Event Bus.
       */
      log.debug("Pubblicazione del messaggio della risposta HTTP sull\\'Event Bus");
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
   * Verifica se la Request URI è tra quelle che devono essere filtrate.
   * Il parametro di configurazione app.filter.uris contiene l'elenco delle URI.
   *
   * @param requestUri La Request URI da verificare
   * @return true se la Request URI è tra quelle che devono essere filtrate, false altrimenti
   */
  private boolean requestUriIsFiltered(String requestUri) {
    log.debug("La Request URI %s è tra quelle che devono essere filtrate".formatted(requestUri));

    return uris.stream().anyMatch(item -> requestUri.startsWith(item));
  }
}
