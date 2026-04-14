/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.resources.endpoint;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class EchoResourceEndPointTest {

    @Test
    void testEchoSuccess() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"message\": \"Hello or Ciao, world!\"}")
                .when().post("/api/rest/echo")
                .then()
                .statusCode(200)
                .body(is("{\"message\": \"Hello or Ciao, world!\"}"));
    }

    @Test
    void testEchoValidation() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"message\": \"Hello, world!\"}")
                .when().post("/api/rest/echo")
                .then()
                .statusCode(400)
                .body("title", is("Constraint Violation"));
    }

    @Test
    void testEchoBodyEmpty() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/api/rest/echo")
                .then()
                .statusCode(400);
    }

    @Test
    void testEchoBodyEmptyWithoutContentType() {
        given()
                .when().post("/api/rest/echo")
                .then()
                .statusCode(415);
    }

    /**
     * Verifica che l'endpoint reattivo {@code POST /api/rest/echo/reactive} restituisca 200
     * con il body identico all'input. Questo test attiva anche la lettura del body sull'event
     * loop thread nel filtro (path executeBlocking, linee 304-316).
     */
    @Test
    void testEchoReactiveSuccess() {
        String body = "{\"message\": \"Hello or Ciao, reactive world!\"}";
        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/rest/echo/reactive")
                .then()
                .statusCode(200)
                .body(is(body));
    }

    /**
     * Verifica che l'endpoint reattivo restituisca 400 per input inferiore a 32 byte.
     */
    @Test
    void testEchoReactiveValidation() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"message\": \"Hi\"}")
                .when().post("/api/rest/echo/reactive")
                .then()
                .statusCode(400)
                .body("title", is("Constraint Violation"));
    }

    /**
     * Verifica che l'endpoint reattivo con body vuoto restituisca 400.
     * Il filtro gira sull'event-loop thread (endpoint reattivo): con body vuoto il fast-path
     * RoutingContext fallisce e la lettura viene delegata a un worker thread via executeBlocking
     * (linee 304-316 di TraceJaxRsRequestResponseFilter).
     */
    @Test
    void testEchoReactiveBodyEmpty() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/api/rest/echo/reactive")
                .then()
                .statusCode(400);
    }

    /**
     * Verifica che {@code GET /api/rest/test-multi-header} restituisca 200 e che il responseFilter
     * processi correttamente un header di risposta con più valori.
     *
     * <p>
     * L'endpoint {@link TestRestMultiHeaderResource} aggiunge {@code X-Test-Multi: value1} e
     * {@code X-Test-Multi: value2} alla risposta. Quando il responseFilter chiama
     * {@code getResponseHeaders}, la seconda iterazione sull'header {@code X-Test-Multi}
     * trova {@code sb} non vuoto ed esegue {@code sb.append(", ")} (linea 386 di
     * {@link it.dontesta.eventbus.ws.filter.TraceJaxRsRequestResponseFilter}).
     */
    @Test
    void testResponseHeaderMultiValue() {
        given()
                .when().get("/api/rest/test-multi-header")
                .then()
                .statusCode(200)
                .body(is("pong"));
    }
}
