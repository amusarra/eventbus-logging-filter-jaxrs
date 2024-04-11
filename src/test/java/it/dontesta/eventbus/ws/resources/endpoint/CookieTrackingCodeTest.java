package it.dontesta.eventbus.ws.resources.endpoint;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class CookieTrackingCodeTest {

  @Test
  void testCookieResponseSuccess() {
    final String COOKIE_USER_TRACKING_NAME = "user_tracking_id";

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

}
