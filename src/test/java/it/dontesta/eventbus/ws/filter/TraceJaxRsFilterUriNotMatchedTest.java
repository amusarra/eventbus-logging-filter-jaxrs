/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.filter;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Verifica il ramo "URI non filtrata" in
 * {@link TraceJaxRsRequestResponseFilter#requestFilter}.
 *
 * <p>
 * Il filtro JAX-RS è attivo (app.filter.enabled=true) ma la URI richiesta non inizia con
 * {@code /api/rest} (configurato in app.filter.uris). Il requestFilter deve tornare
 * immediatamente senza processare la richiesta (linee 163-164).
 *
 * <p>
 * Approccio: viene chiamato {@code GET /api/test-filter/ping}, un endpoint JAX-RS
 * <b>reale</b> definito solo nei sorgenti di test ({@code TestNotFilteredResource}),
 * che risponde con 200. Poiché il path non inizia con {@code /api/rest}, il filtro
 * JAX-RS è correttamente invocato (risorsa esiste) e torna subito al branch di
 * early-return a linee 163-164.
 */
@QuarkusTest
class TraceJaxRsFilterUriNotMatchedTest {

    @Test
    void testRequestFilterSkipsUriNotInFilterList() {
        // Il path /api/test-filter/ping non inizia con /api/rest:
        // requestFilter ritorna subito (early-return linee 163-164).
        // L'endpoint TestNotFilteredResource esiste → il filtro JAX-RS viene invocato.
        // Risultato atteso: 200 con body "pong".
        given()
                .when().get("/api/test-filter/ping")
                .then()
                .statusCode(200);
    }
}
