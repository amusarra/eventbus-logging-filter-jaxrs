package it.dontesta.eventbus.ws.resources.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

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

}
