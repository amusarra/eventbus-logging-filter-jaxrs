/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.vertx.ext.web.RoutingContext;

/**
 * Filtro HTTP basato sulle annotazioni di RESTEasy Reactive ({@link ServerRequestFilter} /
 * {@link ServerResponseFilter}) che implementa il pattern
 * <b>capture-only + external dispatcher</b> per il tracciamento delle richieste e risposte HTTP.
 *
 * <p>
 * I messaggi di request e response sono elaborati solo se il parametro di configurazione
 * {@code app.filter.enabled} è impostato a {@code true}, per default è {@code false}.
 *
 * <p>
 * L'elenco delle URI da filtrare è definito nel parametro di configurazione
 * {@code app.filter.uris}.
 *
 * <p>
 * <b>Strategia di performance — capture-only</b>:
 * <p>
 * Il filtro è <em>solo produttore</em>: l'unica operazione non banale che esegue è la
 * lettura del body (inevitabile perché deve avvenire prima che l'endpoint lo consumi).
 * Tutto il resto — costruzione dei {@link io.vertx.core.json.JsonObject}, serializzazione
 * e publish sull'Event Bus — è delegato al {@link TraceEventDispatcher}, un task schedulato
 * che gira <em>completamente fuori dal lifecycle HTTP</em>.
 *
 * <ol>
 * <li><b>Request filter</b>: legge il body, costruisce un {@link TraceEventDispatcher.RequestTrace}
 * (record leggero, solo campi stringa/primitivi) e lo deposita nella coda lock-free del
 * dispatcher con un'operazione O(1).</li>
 * <li><b>Response filter</b>: aggiunge gli header obbligatori alla risposta, costruisce un
 * {@link TraceEventDispatcher.ResponseTrace} e lo deposita nella coda del dispatcher.</li>
 * <li><b>Dispatcher schedulato</b>: drena le code a intervalli configurabili, costruisce i
 * {@link io.vertx.core.json.JsonObject} e pubblica sull'Event Bus — senza alcun impatto
 * sul throughput HTTP.</li>
 * </ol>
 *
 * <p>
 * Per ulteriori informazioni sui filtri Quarkus REST, vedere la guida:
 * <a href="https://quarkus.io/guides/rest#via-annotations">Quarkus REST – Filters via annotations</a>
 *
 * @author Antonio Musarra
 * @see TraceEventDispatcher
 */
@ApplicationScoped
public class TraceJaxRsRequestResponseFilter {

    @Inject
    TraceEventDispatcher dispatcher;

    @Inject
    Logger log;

    @ConfigProperty(name = "app.filter.enabled", defaultValue = "false")
    boolean filterEnabled;

    @ConfigProperty(name = "app.filter.uris")
    List<String> uris;

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String COOKIE_USER_TRACKING_NAME = "user_tracking_id";
    private static final String POD_NAME_HEADER = "X-Pod-Name";

    /**
     * Filtro di richiesta HTTP — <em>solo capture + enqueue</em>.
     *
     * <p>
     * Legge il body (da {@link RoutingContext#body()} se già bufferizzato dal framework,
     * oppure dall'EntityStream come fallback), costruisce un {@link TraceEventDispatcher.RequestTrace}
     * e lo deposita nella coda del {@link TraceEventDispatcher} con un'operazione O(1).
     * Nessuna costruzione di {@link io.vertx.core.json.JsonObject} avviene qui.
     *
     * @param requestContext contesto JAX-RS della richiesta
     * @param routingContext contesto Vert.x della richiesta
     * @param uriInfo informazioni sull'URI della richiesta
     */
    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public void requestFilter(ContainerRequestContext requestContext,
            RoutingContext routingContext,
            UriInfo uriInfo) {
        if (!filterEnabled) {
            return;
        }

        String requestUri = uriInfo.getRequestUri().getPath();
        String correlationId = getCorrelationId(requestContext.getHeaderString(CORRELATION_ID_HEADER));
        requestContext.setProperty(CORRELATION_ID_HEADER, correlationId);

        if (requestUriIsFiltered(requestUri)) {
            String mediaType = requestContext.getMediaType() == null ? null
                    : "%s/%s".formatted(requestContext.getMediaType().getType(),
                            requestContext.getMediaType().getSubtype());

            // Enqueue O(1): l'unica operazione "costosa" è la lettura del body (inevitabile)
            dispatcher.enqueueRequest(new TraceEventDispatcher.RequestTrace(
                    correlationId,
                    routingContext.request().remoteAddress().host(),
                    new HashMap<>(requestContext.getHeaders()), // copia headers prima che scadano
                    readBody(requestContext, routingContext), // lettura body con fast-path
                    requestContext.getUriInfo().getRequestUri().toString(),
                    Instant.now().toString(),
                    requestContext.getMethod(),
                    mediaType,
                    requestContext.getAcceptableLanguages().toString(),
                    requestContext.getAcceptableMediaTypes().toString()));

            log.debug("RequestTrace accodata nel dispatcher");
        }
    }

    /**
     * Filtro di risposta HTTP — aggiunge gli header obbligatori e <em>solo enqueue</em>.
     *
     * <p>
     * Aggiunge gli header {@code X-Correlation-ID}, {@code X-Pod-Name} e il cookie di
     * tracciamento (operazioni che devono essere nel filtro perché fanno parte della risposta
     * HTTP), poi costruisce un {@link TraceEventDispatcher.ResponseTrace} e lo deposita nella
     * coda del {@link TraceEventDispatcher} con un'operazione O(1).
     *
     * @param requestContext contesto JAX-RS della richiesta
     * @param responseContext contesto JAX-RS della risposta
     * @param routingContext contesto Vert.x della richiesta (non usato, richiesto dalla firma)
     * @param uriInfo informazioni sull'URI della richiesta
     */
    @ServerResponseFilter(priority = Priorities.AUTHENTICATION - 1)
    public void responseFilter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext,
            RoutingContext routingContext,
            UriInfo uriInfo) {
        if (!filterEnabled) {
            return;
        }

        // Header obbligatori - devono stare nel filtro perché fanno parte della risposta HTTP
        String correlationId = getCorrelationId((String) requestContext.getProperty(CORRELATION_ID_HEADER));
        responseContext.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        setCookieUserTracing(requestContext, responseContext);
        responseContext.getHeaders().add(POD_NAME_HEADER, System.getenv("HOSTNAME"));

        String requestUri = uriInfo.getRequestUri().getPath();
        if (requestUriIsFiltered(requestUri)) {
            // Enqueue O(1): snapshot dei dati della risposta (solo primitive/stringhe)
            dispatcher.enqueueResponse(new TraceEventDispatcher.ResponseTrace(
                    correlationId,
                    Instant.now().toString(),
                    responseContext.getStatus(),
                    responseContext.getStatusInfo().getFamily().name(),
                    responseContext.getStatusInfo().getReasonPhrase(),
                    getResponseHeaders(responseContext),
                    responseContext.getEntity() == null ? null : responseContext.getEntity().toString()));

            log.debug("ResponseTrace accodata nel dispatcher");
        }
    }

    /**
     * Legge il body della richiesta con strategia a due livelli:
     * <ol>
     * <li><b>Fast path</b>: {@link RoutingContext#body()} — accesso O(1) al buffer già
     * presente in memoria (zero I/O), usato quando il framework ha già bufferizzato
     * il body prima che il filtro venisse invocato.</li>
     * <li><b>Fallback</b>: lettura dall'EntityStream con ripristino automatico dello stream
     * affinché l'endpoint possa rileggerlo.</li>
     * </ol>
     *
     * @param requestContext contesto JAX-RS
     * @param routingContext contesto Vert.x
     * @return Il corpo della richiesta come stringa UTF-8, mai {@code null}
     */
    private String readBody(ContainerRequestContext requestContext, RoutingContext routingContext) {
        // Fast path: body già bufferizzato nel RoutingContext (zero I/O)
        io.vertx.ext.web.RequestBody rb = routingContext.body();
        if (rb != null) {
            io.vertx.core.buffer.Buffer buf = rb.buffer();
            if (buf != null && buf.length() > 0) {
                return buf.toString(StandardCharsets.UTF_8.name());
            }
        }

        // Fallback: legge dall'EntityStream (bloccante — accettabile sul worker thread)
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (InputStream in = requestContext.getEntityStream()) {
                byte[] data = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(data)) != -1) {
                    buffer.write(data, 0, bytesRead);
                }
            }
            byte[] bodyBytes = buffer.toByteArray();
            requestContext.setEntityStream(new ByteArrayInputStream(bodyBytes)); // ripristina per l'endpoint
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warnf("Impossibile leggere il body della richiesta per il tracing: %s", e.getMessage());
            return "";
        }
    }

    /**
     * Ottiene l'ID di correlazione. Se {@code null}, genera un nuovo UUID.
     *
     * @param correlationId l'ID di correlazione da validare
     * @return L'ID di correlazione non nullo
     */
    private String getCorrelationId(String correlationId) {
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    /**
     * Converte gli header della risposta in una {@link Map} di stringhe piatte
     * (più valori per lo stesso header uniti con {@code ", "}).
     *
     * @param responseContext Il contesto della risposta
     * @return La mappa degli header della risposta
     */
    private Map<String, String> getResponseHeaders(ContainerResponseContext responseContext) {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, List<Object>> entry : responseContext.getHeaders().entrySet()) {
            String key = entry.getKey();
            List<Object> values = entry.getValue();
            StringBuilder sb = new StringBuilder();
            for (Object value : values) {
                if (value != null) {
                    if (!sb.isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append(value);
                }
            }
            stringMap.put(key, sb.toString());
        }
        return stringMap;
    }

    /**
     * Verifica se la Request URI è tra quelle che devono essere filtrate.
     *
     * @param requestUri La Request URI da verificare
     * @return {@code true} se la Request URI deve essere filtrata
     */
    private boolean requestUriIsFiltered(String requestUri) {
        log.debugf("Verifica URI filtrata: %s", requestUri);
        return uris.stream().anyMatch(requestUri::startsWith);
    }

    /**
     * Imposta il cookie di tracciamento utente sulla risposta se non è già presente.
     *
     * @param requestContext Il contesto della richiesta
     * @param responseContext Il contesto della risposta
     */
    private void setCookieUserTracing(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) {
        Cookie trackingCookie = requestContext.getCookies().get(COOKIE_USER_TRACKING_NAME);
        if (trackingCookie == null) {
            NewCookie newTrackingCookie = new NewCookie.Builder(COOKIE_USER_TRACKING_NAME)
                    .value(UUID.randomUUID().toString())
                    .domain(null)
                    .path("/")
                    .comment("Cookie di tracciamento dell'utente")
                    .maxAge((int) TimeUnit.DAYS.toSeconds(30))
                    .secure(false)
                    .httpOnly(true)
                    .build();
            responseContext.getHeaders().add("Set-Cookie", newTrackingCookie);
        }
    }
}
