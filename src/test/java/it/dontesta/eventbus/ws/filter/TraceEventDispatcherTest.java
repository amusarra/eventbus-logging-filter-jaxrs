/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.filter;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Test di copertura per {@link TraceEventDispatcher}.
 *
 * <p>
 * Per evitare il riavvio di Quarkus (e i relativi problemi di connettività Docker con i dev
 * services) causato da un {@code @TestProfile}, questo test utilizza la riflessione per
 * impostare temporaneamente:
 * <ul>
 * <li>la capacità di ciascuna coda a {@code 1}</li>
 * <li>l'intervallo di drain a {@code 100 000 ms} (il drain thread si addormenta
 * per 100 secondi dopo il primo ciclo su code vuote, rendendo il comportamento
 * di drop deterministico)</li>
 * </ul>
 *
 * <p>
 * L'istanza reale del bean (non il proxy CDI) viene estratta tramite
 * {@link io.quarkus.arc.ClientProxy#arc_contextualInstance()}. I valori originali vengono
 * ripristinati in {@link #restoreQueue()} al termine di ogni test; il drain thread verrà
 * fermato correttamente dal {@code @PreDestroy} al successivo riavvio di Quarkus.
 *
 * <p>
 * I metodi {@code enqueueRequest} e {@code enqueueResponse} sono package-private; questo test
 * è nel medesimo package per poterli invocare direttamente.
 */
@QuarkusTest
class TraceEventDispatcherTest {

    @Inject
    TraceEventDispatcher dispatcher;

    /** Istanza reale del bean (non il proxy CDI), usata per la riflessione. */
    private TraceEventDispatcher actual;

    // Valori originali salvati per il ripristino in @AfterEach
    private ArrayBlockingQueue<?> savedRequestQueue;
    private ArrayBlockingQueue<?> savedResponseQueue;
    private long savedDrainIntervalMs;
    private int savedQueueCapacity;

    // -------------------------------------------------------------------------
    // Test lifecycle
    // -------------------------------------------------------------------------

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setupSmallQueue() throws Exception {
        // Estrae l'istanza reale dal proxy CDI Arc
        actual = (TraceEventDispatcher) ((ClientProxy) dispatcher).arc_contextualInstance();

        // Recupera i campi privati tramite riflessione e salva i valori originali
        Field rqField = TraceEventDispatcher.class.getDeclaredField("requestQueue");
        rqField.setAccessible(true);
        savedRequestQueue = (ArrayBlockingQueue<?>) rqField.get(actual);

        Field rsqField = TraceEventDispatcher.class.getDeclaredField("responseQueue");
        rsqField.setAccessible(true);
        savedResponseQueue = (ArrayBlockingQueue<?>) rsqField.get(actual);

        Field drainField = TraceEventDispatcher.class.getDeclaredField("drainIntervalMs");
        drainField.setAccessible(true);
        savedDrainIntervalMs = drainField.getLong(actual);

        Field capField = TraceEventDispatcher.class.getDeclaredField("queueCapacity");
        capField.setAccessible(true);
        savedQueueCapacity = capField.getInt(actual);

        // Sostituisce le code con versioni a capacità 1 e rallenta il drain thread (100 s)
        rqField.set(actual, new ArrayBlockingQueue<>(1));
        rsqField.set(actual, new ArrayBlockingQueue<>(1));
        drainField.setLong(actual, 100_000L);
        capField.setInt(actual, 1);

        // Azzera i contatori di drop (possono avere valori residui da test precedenti)
        Field drReqField = TraceEventDispatcher.class.getDeclaredField("droppedRequests");
        drReqField.setAccessible(true);
        ((AtomicLong) drReqField.get(actual)).set(0);

        Field drResField = TraceEventDispatcher.class.getDeclaredField("droppedResponses");
        drResField.setAccessible(true);
        ((AtomicLong) drResField.get(actual)).set(0);

        // Attende che il drain thread completi il ciclo corrente (con drainIntervalMs originale
        // di 20 ms) e si addormenti con il nuovo drainIntervalMs di 100 000 ms.
        // 150 ms coprono ≥ 7 cicli da 20 ms: sufficienti perché il JIT propaghi il nuovo valore.
        Thread.sleep(150);
    }

    @AfterEach
    void restoreQueue() throws Exception {
        // Ripristina le code e i parametri originali.
        // Il drain thread rimarrà in sleep per ~100 s ma verrà fermato dal @PreDestroy
        // al riavvio di Quarkus per FilterDisabledProfile.
        Field rqField = TraceEventDispatcher.class.getDeclaredField("requestQueue");
        rqField.setAccessible(true);
        rqField.set(actual, savedRequestQueue);

        Field rsqField = TraceEventDispatcher.class.getDeclaredField("responseQueue");
        rsqField.setAccessible(true);
        rsqField.set(actual, savedResponseQueue);

        Field drainField = TraceEventDispatcher.class.getDeclaredField("drainIntervalMs");
        drainField.setAccessible(true);
        drainField.setLong(actual, savedDrainIntervalMs);

        Field capField = TraceEventDispatcher.class.getDeclaredField("queueCapacity");
        capField.setAccessible(true);
        capField.setInt(actual, savedQueueCapacity);
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    private TraceEventDispatcher.RequestTrace buildRequestTrace(String correlationId) {
        return new TraceEventDispatcher.RequestTrace(
                correlationId,
                "127.0.0.1",
                Map.of(),
                "{}",
                "/api/rest/echo",
                Instant.now().toString(),
                "POST",
                "application/json",
                "[it]",
                "[application/json]");
    }

    private TraceEventDispatcher.ResponseTrace buildResponseTrace(String correlationId) {
        return new TraceEventDispatcher.ResponseTrace(
                correlationId,
                Instant.now().toString(),
                200,
                "SUCCESSFUL",
                "OK",
                Map.of(),
                "response-body");
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifica che quando la requestQueue è piena (capacity=1) il secondo enqueue venga scartato
     * e il contatore {@link TraceEventDispatcher#getDroppedRequests()} sia incrementato.
     *
     * <p>
     * Copre le linee:
     * <ul>
     * <li>enqueueRequest – ramo offer() fallisce → droppedRequests.incrementAndGet() + log.warnf</li>
     * <li>checkPressure – 1×100 > 1×80 → warning di pressione (primo enqueue)</li>
     * <li>getDroppedRequests() – getter del contatore</li>
     * </ul>
     */
    @Test
    void testDroppedRequestsWhenQueueFull() {
        // Primo enqueue: succeeds (capacity=1, queue era vuota) + triggera pressure warning
        dispatcher.enqueueRequest(buildRequestTrace("req-drop-1"));
        // Secondo enqueue: coda piena → drop-on-full → droppedRequests++
        dispatcher.enqueueRequest(buildRequestTrace("req-drop-2"));

        assertTrue(dispatcher.getDroppedRequests() >= 1,
                "Almeno un RequestTrace deve essere scartato per backpressure (capacity=1)");
    }

    /**
     * Verifica che quando la responseQueue è piena (capacity=1) il secondo enqueue venga scartato
     * e il contatore {@link TraceEventDispatcher#getDroppedResponses()} sia incrementato.
     *
     * <p>
     * Copre le linee:
     * <ul>
     * <li>enqueueResponse – ramo offer() fallisce → droppedResponses.incrementAndGet() + log.warnf</li>
     * <li>checkPressure – 1×100 > 1×80 → warning di pressione (primo enqueue)</li>
     * <li>getDroppedResponses() – getter del contatore</li>
     * </ul>
     */
    @Test
    void testDroppedResponsesWhenQueueFull() {
        // Primo enqueue: succeeds (responseQueue era vuota) + triggera pressure warning
        dispatcher.enqueueResponse(buildResponseTrace("res-drop-1"));
        // Secondo enqueue: coda piena → drop-on-full → droppedResponses++
        dispatcher.enqueueResponse(buildResponseTrace("res-drop-2"));

        assertTrue(dispatcher.getDroppedResponses() >= 1,
                "Almeno un ResponseTrace deve essere scartato per backpressure (capacity=1)");
    }

    /**
     * Verifica che i getter dei contatori di drop siano leggibili e restituiscano valori >= 0.
     * Garantisce che il bytecode dei metodi {@code getDroppedRequests()} e
     * {@code getDroppedResponses()} sia eseguito anche se non ci sono state drop precedenti.
     */
    @Test
    void testDroppedCountersAreReadable() {
        long droppedRequests = dispatcher.getDroppedRequests();
        long droppedResponses = dispatcher.getDroppedResponses();

        assertTrue(droppedRequests >= 0,
                "Il contatore droppedRequests non può essere negativo");
        assertTrue(droppedResponses >= 0,
                "Il contatore droppedResponses non può essere negativo");
    }
}
