/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.filter;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;

/**
 * Verifica il comportamento del filtro JAX-RS quando {@code app.filter.enabled=false}.
 *
 * <p>
 * Copre i rami di early-return:
 * <ul>
 * <li>requestFilter - linee 155-156 (filterEnabled=false)</li>
 * <li>responseFilter - linee 224-225 (filterEnabled=false)</li>
 * </ul>
 */
@QuarkusTest
@TestProfile(TraceJaxRsFilterDisabledTest.FilterDisabledProfile.class)
class TraceJaxRsFilterDisabledTest {

    private static final String VALID_BODY = "{\"message\": \"Hello or Ciao, world!\"}";

    public static class FilterDisabledProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("app.filter.enabled", "false");
        }
    }

    @Test
    void testCorrelationIdHeaderAbsentWhenFilterDisabled() {
        io.restassured.response.Response response = given()
                .contentType(ContentType.JSON)
                .body(VALID_BODY)
                .when().post("/api/rest/echo")
                .then()
                .statusCode(200)
                .extract().response();

        assertNull(response.getHeader("X-Correlation-ID"),
                "Con filtro disabilitato l'header X-Correlation-ID non deve essere presente");
    }

    @Test
    void testTrackingCookieAbsentWhenFilterDisabled() {
        io.restassured.response.Response response = given()
                .contentType(ContentType.JSON)
                .body(VALID_BODY)
                .when().post("/api/rest/echo")
                .then()
                .statusCode(200)
                .extract().response();

        assertNull(response.getCookie("user_tracking_id"),
                "Con filtro disabilitato il cookie user_tracking_id non deve essere impostato");
    }
}
