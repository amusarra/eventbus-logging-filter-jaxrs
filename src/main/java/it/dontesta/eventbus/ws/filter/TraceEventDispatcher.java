/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Dispatcher asincrono con backpressure per gli eventi di tracciamento HTTP.
 *
 * <p>
 * Questo componente è completamente <b>esterno al lifecycle HTTP</b>: il
 * {@link TraceJaxRsRequestResponseFilter filtro} si limita a depositare dati grezzi in due code
 * bounded ({@link ArrayBlockingQueue}), mentre un <b>daemon thread dedicato</b> drena le code,
 * costruisce i {@link JsonObject} e li pubblica sull'Event Bus.
 *
 * <p>
 * <b>Drain thread dedicato</b>: a differenza di {@code @Scheduled} (che condivide il worker
 * thread pool con i thread HTTP e può essere starved sotto carico), il drain thread è un
 * {@link Thread#ofPlatform() platform thread} daemon schedulato direttamente dall'OS. Non compete
 * mai con i thread HTTP e supporta qualsiasi intervallo di sleep (anche &lt;1 ms). Funziona in
 * due modalità:
 * <ul>
 * <li><b>Burst</b>: se le code contengono elementi, drena immediatamente senza sleep fino a
 * svuotarle completamente.</li>
 * <li><b>Idle</b>: quando entrambe le code sono vuote, il thread dorme per
 * {@code app.filter.dispatcher.interval.ms} ms prima di controllare di nuovo,
 * evitando busy-waiting.</li>
 * </ul>
 *
 * <p>
 * <b>Backpressure</b>: le code sono <em>bounded</em>. Se una coda è piena il thread HTTP
 * non viene mai bloccato: l'evento viene scartato immediatamente (drop-on-full) con un warning
 * di log. Il numero cumulativo di eventi scartati è tracciato da contatori atomici
 * ({@link #getDroppedRequests()}, {@link #getDroppedResponses()}) e può essere esposto a
 * sistemi di monitoring esterni. Quando il livello di riempimento supera la soglia
 * {@value #QUEUE_WARN_THRESHOLD_PCT}% viene emesso un warning per segnalare pressione crescente.
 *
 * <p>
 * <b>Perché {@link ArrayBlockingQueue}</b>: struttura dati FIFO bounded con garanzia
 * di ordinamento FIFO, supporto nativo per {@code offer()} non-bloccante e sicurezza thread
 * garantita. A differenza di {@code ConcurrentLinkedQueue} la capacità massima è nota a priori
 * ed è impossibile provocare un OOM in caso di downstream lento.
 *
 * <p>
 * I record interni {@link RequestTrace} e {@link ResponseTrace} sono strutture dati leggere
 * (solo campi stringa/primitivi) che vengono istanziate dal filtro a costo minimo.
 *
 * <p>
 * Proprietà di configurazione rilevanti:
 * <ul>
 * <li>{@code app.filter.dispatcher.interval.ms} — intervallo di idle sleep in ms
 * (default: {@code 20})</li>
 * <li>{@code app.filter.dispatcher.queue.capacity} — capacità massima di ciascuna coda
 * (default: {@value #DEFAULT_QUEUE_CAPACITY})</li>
 * </ul>
 *
 * @author Antonio Musarra
 * @see TraceJaxRsRequestResponseFilter
 */
@ApplicationScoped
public class TraceEventDispatcher {

    /**
     * Capacità di default di ciascuna coda (request e response).
     * Dimensionata per gestire benchmark da 5000+ richieste senza drop anche sotto carico pesante.
     */
    static final int DEFAULT_QUEUE_CAPACITY = 5000;

    /**
     * Soglia percentuale oltre la quale viene emesso un warning di pressione.
     * Calcolata come {@code size / capacity > QUEUE_WARN_THRESHOLD_PCT / 100}.
     */
    static final int QUEUE_WARN_THRESHOLD_PCT = 80;

    /**
     * Snapshot leggero dei dati della richiesta HTTP.
     * Tutti i campi sono stringhe o tipi primitivi: nessun JsonObject, nessuna serializzazione.
     */
    record RequestTrace(
            String correlationId,
            String remoteAddress,
            Map<String, List<String>> headers,
            String body,
            String uri,
            String dateTimeIn,
            String method,
            String mediaType,
            String acceptLang,
            String acceptMedia) {
    }

    /**
     * Snapshot leggero dei dati della risposta HTTP.
     */
    record ResponseTrace(
            String correlationId,
            String dateTimeOut,
            int status,
            String statusFamily,
            String statusReason,
            Map<String, String> responseHeaders,
            String body) {
    }

    // Code bounded (produttori: thread HTTP; consumatore: drain thread dedicato)
    // Inizializzate in @PostConstruct dopo l'injection della configurazione.
    private ArrayBlockingQueue<RequestTrace> requestQueue;
    private ArrayBlockingQueue<ResponseTrace> responseQueue;

    // Contatori di drop (visibili a sistemi di monitoring)
    private final AtomicLong droppedRequests = new AtomicLong(0);
    private final AtomicLong droppedResponses = new AtomicLong(0);

    // Stato del drain thread dedicato
    private volatile boolean running = true;
    private Thread drainThread;

    @Inject
    EventBus eventBus;

    @Inject
    Logger log;

    @ConfigProperty(name = "app.eventbus.consumer.http.request.address")
    String httpRequestVirtualAddress;

    @ConfigProperty(name = "app.eventbus.consumer.http.response.address")
    String httpResponseVirtualAddress;

    /**
     * Intervallo di sleep in millisecondi quando entrambe le code sono vuote (modalità idle).
     * In modalità burst (code non vuote) il drain avviene senza sleep.
     * Configurabile tramite {@code app.filter.dispatcher.interval.ms}.
     */
    @ConfigProperty(name = "app.filter.dispatcher.interval.ms", defaultValue = "20")
    long drainIntervalMs;

    /**
     * Capacità massima di ciascuna coda. Configurabile tramite
     * {@code app.filter.dispatcher.queue.capacity}.
     */
    @ConfigProperty(name = "app.filter.dispatcher.queue.capacity", defaultValue = "" + DEFAULT_QUEUE_CAPACITY)
    int queueCapacity;
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String LOCAL_DATE_TIME_IN = "local-date-time-in";
    private static final String LOCAL_DATE_TIME_OUT = "local-date-time-out";

    /**
     * Inizializza le code bounded e avvia il drain thread dedicato.
     *
     * <p>
     * Il drain thread è un daemon thread completamente separato dal worker thread pool di Quarkus:
     * non compete con i thread HTTP durante i picchi di carico e supporta qualsiasi intervallo
     * di sleep (inclusi valori inferiori a 1000ms, non supportati da {@code SimpleScheduler}).
     */
    @PostConstruct
    void init() {
        requestQueue = new ArrayBlockingQueue<>(queueCapacity);
        responseQueue = new ArrayBlockingQueue<>(queueCapacity);

        drainThread = Thread.ofPlatform()
                .name("trace-event-dispatcher")
                .daemon(true)
                .start(this::drainLoop);

        log.infof(
                "TraceEventDispatcher: avviato — capacity=%d, warn_threshold=%d%%, drain_interval_ms=%d",
                queueCapacity, QUEUE_WARN_THRESHOLD_PCT, drainIntervalMs);
    }

    /**
     * Ferma il drain thread e svuota le code al momento dello shutdown per non perdere gli ultimi
     * eventi accodati.
     */
    @PreDestroy
    void onShutdown() {
        log.debug("Shutdown: interruzione del drain thread e flush finale delle code di trace HTTP");
        running = false;
        if (drainThread != null) {
            drainThread.interrupt();
            try {
                drainThread.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        drainQueues();
    }

    /**
     * Tenta di accodare un evento di richiesta HTTP.
     *
     * <p>
     * Usa {@link ArrayBlockingQueue#offer(Object)} (non-bloccante): se la coda è piena
     * l'evento viene scartato immediatamente e il contatore {@link #getDroppedRequests()}
     * viene incrementato. Il thread HTTP non subisce mai attesa.
     *
     * @param trace snapshot dei dati della richiesta
     */
    void enqueueRequest(RequestTrace trace) {
        if (!requestQueue.offer(trace)) {
            long dropped = droppedRequests.incrementAndGet();
            log.warnf(
                    "Backpressure [request]: evento scartato — queue piena (capacity=%d, dropped_total=%d)",
                    queueCapacity, dropped);
        } else {
            checkPressure(requestQueue.size(), "request");
        }
    }

    /**
     * Tenta di accodare un evento di risposta HTTP.
     *
     * <p>
     * Usa {@link ArrayBlockingQueue#offer(Object)} (non-bloccante): se la coda è piena
     * l'evento viene scartato immediatamente e il contatore {@link #getDroppedResponses()}
     * viene incrementato. Il thread HTTP non subisce mai attesa.
     *
     * @param trace snapshot dei dati della risposta
     */
    void enqueueResponse(ResponseTrace trace) {
        if (!responseQueue.offer(trace)) {
            long dropped = droppedResponses.incrementAndGet();
            log.warnf(
                    "Backpressure [response]: evento scartato — queue piena (capacity=%d, dropped_total=%d)",
                    queueCapacity, dropped);
        } else {
            checkPressure(responseQueue.size(), "response");
        }
    }

    /**
     * Restituisce il numero cumulativo di RequestTrace scartate per backpressure.
     * Può essere esposto via health-check o metriche.
     *
     * @return numero totale di richieste scartate dall'avvio
     */
    public long getDroppedRequests() {
        return droppedRequests.get();
    }

    /**
     * Restituisce il numero cumulativo di ResponseTrace scartate per backpressure.
     *
     * @return numero totale di risposte scartate dall'avvio
     */
    public long getDroppedResponses() {
        return droppedResponses.get();
    }

    /**
     * Loop eseguito dal drain thread dedicato.
     *
     * <p>
     * <b>Modalità burst</b>: quando le code contengono elementi il drain avviene in continuo
     * senza sleep, massimizzando il throughput durante i picchi di carico.
     *
     * <p>
     * <b>Modalità idle</b>: quando entrambe le code sono vuote il thread dorme per
     * {@link #drainIntervalMs} ms prima di controllare di nuovo, evitando busy-waiting a CPU piena.
     */
    private void drainLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                int total = drainQueues();
                if (total == 0) {
                    // Code vuote: sleep configurabile prima del prossimo controllo
                    Thread.sleep(drainIntervalMs);
                }
                // Se c'era qualcosa da drenare, reitera immediatamente senza sleep
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Implementazione del drain: svuota entrambe le code e pubblica sull'Event Bus.
     * Condiviso tra {@link #drainLoop()} e {@link #onShutdown()}.
     *
     * @return numero totale di eventi pubblicati in questa invocazione
     */
    private int drainQueues() {
        int reqCount = 0;
        RequestTrace req;
        while ((req = requestQueue.poll()) != null) {
            eventBus.publish(httpRequestVirtualAddress, toJson(req));
            reqCount++;
        }

        int resCount = 0;
        ResponseTrace res;
        while ((res = responseQueue.poll()) != null) {
            eventBus.publish(httpResponseVirtualAddress, toJson(res));
            resCount++;
        }

        if (reqCount > 0 || resCount > 0) {
            log.debugf("Dispatcher: pubblicati %d request e %d response su Event Bus",
                    reqCount, resCount);
        }
        return reqCount + resCount;
    }

    /**
     * Emette un warning se il livello di riempimento della coda supera
     * {@value #QUEUE_WARN_THRESHOLD_PCT}% della capacità.
     *
     * @param currentSize dimensione corrente della coda
     * @param queueName nome leggibile per il log ("request" o "response")
     */
    private void checkPressure(int currentSize, String queueName) {
        if (currentSize * 100 > queueCapacity * QUEUE_WARN_THRESHOLD_PCT) {
            log.warnf(
                    "Backpressure [%s]: coda in pressione — %d/%d elementi (>%d%% della capacità)",
                    queueName, currentSize, queueCapacity, QUEUE_WARN_THRESHOLD_PCT);
        }
    }

    /**
     * Converte un {@link RequestTrace} in {@link JsonObject}.
     * Eseguito nello scheduler thread, fuori dal lifecycle HTTP.
     */
    private JsonObject toJson(RequestTrace t) {
        return new JsonObject()
                .put(CORRELATION_ID_HEADER, t.correlationId())
                .put("remote-ip-address", t.remoteAddress())
                .put("headers", t.headers())
                .put("body", t.body())
                .put("uri-info", t.uri())
                .put(LOCAL_DATE_TIME_IN, t.dateTimeIn())
                .put("method", t.method())
                .put("media-type", t.mediaType())
                .put("acceptable-language", t.acceptLang())
                .put("acceptable-media-types", t.acceptMedia());
    }

    /**
     * Converte un {@link ResponseTrace} in {@link JsonObject}.
     * Eseguito nello scheduler thread, fuori dal lifecycle HTTP.
     */
    private JsonObject toJson(ResponseTrace t) {
        return new JsonObject()
                .put(CORRELATION_ID_HEADER, t.correlationId())
                .put(LOCAL_DATE_TIME_OUT, t.dateTimeOut())
                .put("status", t.status())
                .put("status-info-family-name", t.statusFamily())
                .put("status-info-reason", t.statusReason())
                .put("headers", t.responseHeaders())
                .put("body", t.body());
    }
}
