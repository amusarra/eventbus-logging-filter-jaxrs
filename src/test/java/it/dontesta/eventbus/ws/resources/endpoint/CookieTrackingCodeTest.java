/*
 * SPDX-FileCopyrightText: 2024 Antonio Musarra's Blog <https://www.dontesta.it>
 * SPDX-License-Identifier: MIT
 */
package it.dontesta.eventbus.ws.resources.endpoint;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class CookieTrackingCodeTest {

    private static final String COOKIE_USER_TRACKING_NAME = "user_tracking_id";

    @Test
    void testCookieResponseSuccess() {

        final Pattern UUID_PATTERN = Pattern.compile(
                "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$",
                Pattern.CASE_INSENSITIVE);

        String trackingCode = given()
                .contentType(ContentType.JSON)
                .body(
                        "{\"message\": \"Primo test del filtro JAX-RS che aggiunge il cookie di tracciamento \"}")
                .when().post("/api/rest/echo")
                .then()
                .statusCode(200)
                .extract().cookie(COOKIE_USER_TRACKING_NAME);

        Matcher matcher = UUID_PATTERN.matcher(trackingCode);
        assertTrue(matcher.find());
    }

    /**
     * Verifica che il filtro NON sovrascriva il cookie {@code user_tracking_id} quando
     * la richiesta lo porta già. Copre il ramo {@code trackingCookie != null} in
     * {@link it.dontesta.eventbus.ws.filter.TraceJaxRsRequestResponseFilter#setCookieUserTracing}
     * (linea di skip del new-cookie, equivalente alla condizione falsa dell'if a linea 416).
     */
    @Test
    void testCookieNotReplacedWhenAlreadyPresent() {
        final String existingCookieValue = "00000000-0000-4000-8000-000000000001";

        // La richiesta porta già il cookie: il filtro non deve impostare un nuovo Set-Cookie
        // per user_tracking_id; il cookie restituito deve essere assente o uguale all'originale.
        io.restassured.response.Response response = given()
                .contentType(ContentType.JSON)
                .cookie(COOKIE_USER_TRACKING_NAME, existingCookieValue)
                .body(
                        "{\"message\": \"Test filtro con cookie di tracciamento gia presente\"}")
                .when().post("/api/rest/echo")
                .then()
                .statusCode(200)
                .extract().response();

        // Quando il cookie e' gia' presente, il filtro non aggiunge Set-Cookie per quel cookie.
        String setCookieHeader = response.getHeader("Set-Cookie");
        if (setCookieHeader != null) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    setCookieHeader.contains(COOKIE_USER_TRACKING_NAME + "=")
                            && !setCookieHeader.contains(existingCookieValue),
                    "Il filtro non deve sovrascrivere il cookie user_tracking_id gia' presente");
        }
    }
}
