package it.dontesta.eventbus.ws.filter;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Dispatcher asincrono con backpressure per gli eventi di tracciamento HTTP.
 *
 * <p>Questo componente è completamente <b>esterno al lifecycle HTTP</b>: il
 * {@link TraceJaxRsRequestResponseFilter filtro} si limita a depositare dati grezzi in due code
 * bounded ({@link ArrayBlockingQueue}), mentre questo bean drena le code a intervalli
 * regolari tramite un {@link Scheduled task schedulato}, costruisce i {@link JsonObject} e li
 * pubblica sull'Event Bus.
 *
 * <p><b>Backpressure</b>: le code sono <em>bounded</em>. Se una coda è piena il thread HTTP
 * non viene mai bloccato: l'evento viene scartato immediatamente (drop-on-full) con un warning
 * di log. Il numero cumulativo di eventi scartati è tracciato da contatori atomici
 * ({@link #getDroppedRequests()}, {@link #getDroppedResponses()}) e può essere esposto a
 * sistemi di monitoring esterni. Quando il livello di riempimento supera la soglia
 * {@value #QUEUE_WARN_THRESHOLD_PCT}% viene emesso un warning per segnalare pressione crescente.
 *
 * <p><b>Perché {@link ArrayBlockingQueue}</b>: struttura dati FIFO bounded con garanzia
 * di ordinamento FIFO, supporto nativo per {@code offer()} non-bloccante e sicurezza thread
 * garantita. A differenza di {@code ConcurrentLinkedQueue} la capacità massima è nota a priori
 * ed è impossibile provocare un OOM in caso di downstream lento.
 *
 * <p>I record interni {@link RequestTrace} e {@link ResponseTrace} sono strutture dati leggere
 * (solo campi stringa/primitivi) che vengono istanziate dal filtro a costo minimo.
 *
 * <p>Proprietà di configurazione rilevanti:
 * <ul>
 *   <li>{@code app.filter.dispatcher.interval} — intervallo di drain (default: {@code 100ms})</li>
 *   <li>{@code app.filter.dispatcher.queue.capacity} — capacità massima di ciascuna coda
 *       (default: {@value #DEFAULT_QUEUE_CAPACITY})</li>
 * </ul>
 *
 * @author Antonio Musarra
 * @see TraceJaxRsRequestResponseFilter
 */
@ApplicationScoped
public class TraceEventDispatcher {

  /** Capacità di default di ciascuna coda (request e response). */
  static final int DEFAULT_QUEUE_CAPACITY = 1000;

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

  // Code bounded (produttori: thread HTTP; consumatore: scheduler)
  // Inizializzate in @PostConstruct dopo l'injection della configurazione.
  private ArrayBlockingQueue<RequestTrace>  requestQueue;
  private ArrayBlockingQueue<ResponseTrace> responseQueue;

  // Contatori di drop (visibili a sistemi di monitoring)
  private final AtomicLong droppedRequests  = new AtomicLong(0);
  private final AtomicLong droppedResponses = new AtomicLong(0);

  @Inject
  EventBus eventBus;

  @Inject
  Logger log;

  @ConfigProperty(name = "app.eventbus.consumer.http.request.address")
  String httpRequestVirtualAddress;

  @ConfigProperty(name = "app.eventbus.consumer.http.response.address")
  String httpResponseVirtualAddress;

  /**
   * Capacità massima di ciascuna coda. Configurabile tramite
   * {@code app.filter.dispatcher.queue.capacity}.
   */
  @ConfigProperty(name = "app.filter.dispatcher.queue.capacity",
      defaultValue = "" + DEFAULT_QUEUE_CAPACITY)
  int queueCapacity;

  // Costanti JSON (mirror di quelle nel filtro)
  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String LOCAL_DATE_TIME_IN    = "local-date-time-in";
  private static final String LOCAL_DATE_TIME_OUT   = "local-date-time-out";

  /**
   * Inizializza le code bounded dopo l'injection dei valori di configurazione.
   * Non può essere fatto a livello di campo perché {@code queueCapacity} è iniettato da CDI.
   */
  @PostConstruct
  void init() {
    requestQueue  = new ArrayBlockingQueue<>(queueCapacity);
    responseQueue = new ArrayBlockingQueue<>(queueCapacity);
    log.infof(
        "TraceEventDispatcher: code inizializzate — capacity=%d, warn_threshold=%d%%",
        queueCapacity, QUEUE_WARN_THRESHOLD_PCT);
  }

  /**
   * Svuota le code al momento dello shutdown per non perdere gli ultimi eventi accodati.
   */
  @PreDestroy
  void onShutdown() {
    log.debug("Shutdown: flush finale delle code di trace HTTP");
    drainQueues();
  }

  /**
   * Tenta di accodare un evento di richiesta HTTP.
   *
   * <p>Usa {@link ArrayBlockingQueue#offer(Object)} (non-bloccante): se la coda è piena
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
   * <p>Usa {@link ArrayBlockingQueue#offer(Object)} (non-bloccante): se la coda è piena
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
   * Drena entrambe le code e pubblica gli eventi sull'Event Bus.
   *
   * <p>Eseguito periodicamente fuori da qualsiasi ciclo HTTP. L'intervallo è configurabile
   * tramite {@code app.filter.dispatcher.interval} (default: {@code 100ms}).
   * Se le code sono vuote il metodo termina in O(1) senza overhead.
   */
  @Scheduled(every = "${app.filter.dispatcher.interval:100ms}")
  void drain() {
    drainQueues();
  }

  /**
   * Implementazione del drain: svuota entrambe le code e pubblica sull'Event Bus.
   * Condiviso tra {@link #drain()} e {@link #onShutdown()}.
   */
  private void drainQueues() {
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
  }

  /**
   * Emette un warning se il livello di riempimento della coda supera
   * {@value #QUEUE_WARN_THRESHOLD_PCT}% della capacità.
   *
   * @param currentSize dimensione corrente della coda
   * @param queueName   nome leggibile per il log ("request" o "response")
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
        .put(CORRELATION_ID_HEADER,    t.correlationId())
        .put("remote-ip-address",      t.remoteAddress())
        .put("headers",                t.headers())
        .put("body",                   t.body())
        .put("uri-info",               t.uri())
        .put(LOCAL_DATE_TIME_IN,       t.dateTimeIn())
        .put("method",                 t.method())
        .put("media-type",             t.mediaType())
        .put("acceptable-language",    t.acceptLang())
        .put("acceptable-media-types", t.acceptMedia());
  }

  /**
   * Converte un {@link ResponseTrace} in {@link JsonObject}.
   * Eseguito nello scheduler thread, fuori dal lifecycle HTTP.
   */
  private JsonObject toJson(ResponseTrace t) {
    return new JsonObject()
        .put(CORRELATION_ID_HEADER,     t.correlationId())
        .put(LOCAL_DATE_TIME_OUT,        t.dateTimeOut())
        .put("status",                  t.status())
        .put("status-info-family-name", t.statusFamily())
        .put("status-info-reason",      t.statusReason())
        .put("headers",                 t.responseHeaders())
        .put("body",                    t.body());
  }
}
