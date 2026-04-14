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

import io.smallrye.mutiny.Uni;
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
 * <b>Strategia di performance - capture-only + non-blocking</b>:
 * <p>
 * Entrambi i metodi filtro restituiscono {@link Uni}{@code <Void>}: RESTEasy Reactive interpreta
 * questo tipo di ritorno come segnale che il filtro è non bloccante ed è in grado di eseguire
 * operazioni asincrone. Il filtro è <em>solo produttore</em>: l'unica operazione non banale
 * che esegue è la lettura del body (inevitabile perché deve avvenire prima che l'endpoint lo
 * consumi, tramite fast-path non bloccante {@code RoutingContext#body()}).
 * Tutto il resto - costruzione dei {@link io.vertx.core.json.JsonObject}, serializzazione
 * e publish sull'Event Bus - è delegato al {@link TraceEventDispatcher}, un drain thread dedicato
 * che gira <em>completamente fuori dal lifecycle HTTP</em>.
 *
 * <ol>
 * <li><b>Request filter</b>: legge il body via fast-path non bloccante, costruisce un
 * {@link TraceEventDispatcher.RequestTrace} (record leggero, solo campi stringa/primitivi) e lo
 * deposita nella coda bounded del dispatcher con un'operazione O(1); restituisce immediatamente
 * {@code Uni.createFrom().voidItem()} senza mai bloccare l'event loop.</li>
 * <li><b>Response filter</b>: aggiunge gli header obbligatori alla risposta, costruisce un
 * {@link TraceEventDispatcher.ResponseTrace} e lo deposita nella coda del dispatcher con O(1);
 * restituisce immediatamente {@code Uni.createFrom().voidItem()}.</li>
 * <li><b>Dispatcher</b>: il drain thread dedicato drena le code a burst/idle, costruisce i
 * {@link io.vertx.core.json.JsonObject} e pubblica sull'Event Bus - senza alcun impatto
 * sul throughput HTTP.</li>
 * </ol>
 *
 * <p>
 * <b>Perché {@code Uni<Void>}</b>: con la firma {@code void}, RESTEasy Reactive può schedulare
 * il filtro su un worker thread se l'endpoint è {@code @Blocking}.
 * Restituire {@code Uni<Void>} segnala al framework che il filtro è intrinsecamente non
 * bloccante e che può completare la filter chain non appena il {@code Uni} emette.
 * <b>Nota importante sul thread model</b>: per endpoint blocking (JAX-RS classico senza
 * {@code @NonBlocking}), sia con firma {@code void} che con {@code Uni<Void>}, il filtro viene
 * comunque eseguito su un worker thread ({@code executor-thread-*}). Il vantaggio di
 * {@code Uni<Void>} non è cambiare il thread di esecuzione, ma segnalare al framework
 * la natura non bloccante del filtro e abilitare l'integrazione con pipeline Mutiny future.
 * <p>
 * <b>Nota</b>: {@code @NonBlocking} <em>non</em> deve essere usato sui metodi
 * {@code @ServerRequestFilter}/{@code @ServerResponseFilter}: Quarkus vieta esplicitamente
 * le annotazioni di execution model su metodi che non sono "entrypoint" diretti del framework
 * ({@code ExecutionModelAnnotationsProcessor} lancia {@code IllegalStateException} a build time).
 * Il tipo di ritorno {@code Uni<Void>} è l'unico meccanismo corretto per questi filtri.
 *
 * <p>
 * <b>Nota sul body reading</b>: il metodo {@code readBodyAsync()} applica una strategia a due
 * livelli: (1) fast-path {@code RoutingContext#body()} O(1); (2) lettura bloccante via
 * {@code EntityStream} JAX-RS ({@code VertxInputStream}) con ripristino dello stream.
 * Per endpoint blocking (worker thread) la lettura è diretta; per endpoint reattivi
 * (I/O event loop thread) è delegata a un Vert.x worker thread via
 * {@code Vertx#executeBlocking()} — il body è catturato in entrambi i casi.
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
     * Filtro di richiesta HTTP - <em>capture + enqueue con lettura body adattiva</em>.
     *
     * <p>
     * Restituisce {@code Uni<Void>}: RESTEasy Reactive interpreta questo tipo di ritorno come
     * segnale che il filtro supporta operazioni asincrone. Il thread su cui viene eseguito
     * dipende dall'endpoint:
     * <ul>
     * <li>Endpoint <b>blocking</b> (JAX-RS classico, privo di {@code @NonBlocking}): il filtro
     * viene eseguito su un <b>worker thread</b> ({@code executor-thread-*}). La lettura del body
     * avviene in modo bloccante via {@code EntityStream} direttamente sul thread corrente.</li>
     * <li>Endpoint <b>reattivo</b> ({@code Uni<Response>} o simile): il filtro gira
     * sull'<b>I/O event loop thread</b> ({@code vert.x-eventloop-thread-*}). La lettura del body
     * viene delegata a un Vert.x worker thread tramite {@code Vertx#executeBlocking()}, così
     * l'event loop non viene mai bloccato; {@code VertxInputStream.readBlocking()} coordina
     * correttamente con l'event loop per ricevere i data frame.</li>
     * </ul>
     * In entrambi i casi il body viene ripristinato nell'EntityStream tramite
     * {@code ByteArrayInputStream} affinché RESTEasy Reactive lo passi correttamente al parametro
     * dell'endpoint.
     *
     * @param requestContext contesto JAX-RS della richiesta
     * @param routingContext contesto Vert.x della richiesta
     * @param uriInfo informazioni sull'URI della richiesta
     * @return {@code Uni<Void>} che completa dopo l'enqueue del trace (sincrono o asincrono)
     */
    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public Uni<Void> requestFilter(ContainerRequestContext requestContext,
            RoutingContext routingContext,
            UriInfo uriInfo) {
        if (!filterEnabled) {
            return Uni.createFrom().voidItem();
        }

        String requestUri = uriInfo.getRequestUri().getPath();
        String correlationId = getCorrelationId(requestContext.getHeaderString(CORRELATION_ID_HEADER));
        requestContext.setProperty(CORRELATION_ID_HEADER, correlationId);

        if (!requestUriIsFiltered(requestUri)) {
            return Uni.createFrom().voidItem();
        }

        // Snapshot dei campi del requestContext prima dell'eventuale catena asincrona:
        // alcuni campi potrebbero non essere accessibili dopo il completamento del Uni.
        final String mediaType = requestContext.getMediaType() == null ? null
                : "%s/%s".formatted(requestContext.getMediaType().getType(),
                        requestContext.getMediaType().getSubtype());
        final String remoteHost = routingContext.request().remoteAddress().host();
        final Map<String, List<String>> headersCopy = new HashMap<>(requestContext.getHeaders());
        final String requestUriStr = requestContext.getUriInfo().getRequestUri().toString();
        final String method = requestContext.getMethod();
        final String acceptLang = requestContext.getAcceptableLanguages().toString();
        final String acceptMedia = requestContext.getAcceptableMediaTypes().toString();
        final String dateTimeIn = Instant.now().toString();

        return readBodyAsync(requestContext, routingContext)
                .invoke(body -> {
                    // Enqueue O(1): avviene dopo che il body è stato letto (sync o async)
                    dispatcher.enqueueRequest(new TraceEventDispatcher.RequestTrace(
                            correlationId,
                            remoteHost,
                            headersCopy,
                            body,
                            requestUriStr,
                            dateTimeIn,
                            method,
                            mediaType,
                            acceptLang,
                            acceptMedia));
                    log.debug("RequestTrace accodata nel dispatcher");
                })
                .replaceWithVoid();
    }

    /**
     * Filtro di risposta HTTP - <em>header injection + enqueue</em>.
     *
     * <p>
     * Restituisce {@code Uni<Void>}: il thread di esecuzione segue la stessa regola del
     * request filter (worker thread per endpoint blocking, event loop per endpoint non-blocking).
     * Aggiunge gli header obbligatori alla risposta (operazioni che devono stare nel filtro
     * perché fanno parte della risposta HTTP), poi deposita un
     * {@link TraceEventDispatcher.ResponseTrace} nella coda del dispatcher con O(1).
     * Il metodo restituisce {@code Uni.createFrom().voidItem()} immediatamente.
     *
     * <p>
     * <b>Nota su {@code @ServerResponseFilter}</b>: RESTEasy Reactive supporta {@code Uni<Void>}
     * come tipo di ritorno anche per i response filter (non solo per i request filter),
     * consentendo la stessa strategia su entrambi i lati del lifecycle HTTP.
     *
     * @param requestContext contesto JAX-RS della richiesta
     * @param responseContext contesto JAX-RS della risposta
     * @param uriInfo informazioni sull'URI della richiesta
     * @return {@code Uni<Void>} che completa immediatamente segnalando il successo
     */
    @ServerResponseFilter(priority = Priorities.AUTHENTICATION - 1)
    public Uni<Void> responseFilter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext,
            UriInfo uriInfo) {
        if (!filterEnabled) {
            return Uni.createFrom().voidItem();
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

        return Uni.createFrom().voidItem();
    }

    /**
     * Legge il body della richiesta con strategia adattiva a due livelli.
     *
     * <ol>
     * <li><b>Fast-path</b>: {@code RoutingContext#body()} – accesso O(1) al buffer Vert.x già
     * presente in memoria (zero I/O). Disponibile se il framework ha bufferizzato il body
     * prima dell'invocazione del filtro (es. tramite un {@code @RouteFilter} con
     * {@code BodyHandler}).</li>
     * <li><b>Lettura bloccante via EntityStream</b>: legge l'EntityStream JAX-RS
     * ({@code requestContext.getEntityStream()}, backed da {@code VertxInputStream}) e ripristina
     * lo stream tramite {@code ByteArrayInputStream} affinché l'endpoint riceva correttamente il
     * parametro body. Il thread su cui avviene la lettura dipende dall'endpoint:
     * <ul>
     * <li><b>Endpoint blocking</b> ({@code executor-thread-*}): lettura diretta sul thread
     * corrente (bloccante OK su worker thread).</li>
     * <li><b>Endpoint reattivo</b> ({@code vert.x-eventloop-thread-*}): la lettura è delegata
     * a un Vert.x worker thread tramite {@code Vertx#executeBlocking()} — il thread corrente
     * non viene bloccato. {@code VertxInputStream.readBlocking()} funziona correttamente su
     * Vert.x worker thread perché coordina con l'event loop per ricevere i dati.</li>
     * </ul>
     * </li>
     * </ol>
     *
     * <p>
     * <b>Perché NON usare {@code Vertx.currentContext().isEventLoopContext()}</b>: in Vert.x 4.x
     * il contesto Vert.x viene propagato ai worker thread ({@code executor-thread-*}) mantenendo
     * il tipo {@code EventLoopContext}. Di conseguenza {@code isEventLoopContext()} restituisce
     * {@code true} anche su un worker thread fisico, rendendo il check inaffidabile. Il nome del
     * thread fisico ({@code Thread.currentThread().getName()}) è il modo corretto per distinguere
     * I/O event loop thread da worker thread.
     *
     * @param requestContext contesto JAX-RS (EntityStream e ripristino)
     * @param routingContext contesto Vert.x (fast-path e accesso al worker thread pool)
     * @return {@link Uni}{@code <String>} con il corpo della richiesta UTF-8, mai {@code null}
     */
    private Uni<String> readBodyAsync(ContainerRequestContext requestContext,
            RoutingContext routingContext) {
        // Fast-path: body già bufferizzato nel RoutingContext Vert.x (O(1), zero I/O)
        io.vertx.ext.web.RequestBody rb = routingContext.body();
        if (rb != null) {
            io.vertx.core.buffer.Buffer buf = rb.buffer();
            if (buf != null && buf.length() > 0) {
                return Uni.createFrom().item(buf.toString(StandardCharsets.UTF_8.name()));
            }
        }

        // Usa il nome del thread FISICO per determinare se siamo sull'I/O event loop.
        // NON usare Vertx.currentContext().isEventLoopContext(): il contesto Vert.x è propagato
        // anche ai worker thread (executor-thread-*) con tipo EventLoopContext, rendendo
        // isEventLoopContext() sempre true anche su thread bloccanti.
        boolean isOnEventLoopThread = Thread.currentThread().getName().startsWith("vert.x-eventloop");

        if (isOnEventLoopThread) {
            // I/O event loop thread (endpoint reattivo, es. Uni<Response>):
            // Non possiamo bloccare il thread corrente. Deleghiamo la lettura dell'EntityStream
            // a un Vert.x worker thread tramite executeBlocking(). VertxInputStream.readBlocking()
            // funziona correttamente su Vert.x worker thread (WorkerContext.isEventLoopContext()
            // == false): coordina con l'event loop per ricevere i data frame senza bloccarlo.
            log.debugf(
                    "I/O event loop thread (endpoint reattivo): lettura body delegata a worker thread (thread=%s)",
                    Thread.currentThread().getName());
            return Uni.createFrom().completionStage(
                    routingContext.vertx().<String> executeBlocking(
                            () -> readBodyBlocking(requestContext))
                            .toCompletionStage());
        }

        // Worker thread (endpoint blocking, executor-thread-*): lettura bloccante diretta.
        log.debugf("Worker thread (endpoint blocking): lettura body via EntityStream (thread=%s)",
                Thread.currentThread().getName());
        return Uni.createFrom().item(readBodyBlocking(requestContext));
    }

    /**
     * Legge il body dalla EntityStream JAX-RS (bloccante) e ripristina lo stream.
     *
     * <p>
     * Sicuro su Vert.x worker thread (sia {@code executor-thread-*} invocato direttamente,
     * sia il thread di {@code executeBlocking()} per endpoint reattivi). Non deve mai essere
     * chiamato sull'I/O event loop thread.
     *
     * @param requestContext contesto JAX-RS con l'EntityStream da leggere
     * @return Il corpo della richiesta come stringa UTF-8, mai {@code null}
     */
    private String readBodyBlocking(ContainerRequestContext requestContext) {
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
            // Ripristina l'EntityStream affinché l'endpoint possa leggere il body normalmente
            requestContext.setEntityStream(new ByteArrayInputStream(bodyBytes));
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warnf("Impossibile leggere il body della richiesta per il tracing: %s",
                    e.getMessage());
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
