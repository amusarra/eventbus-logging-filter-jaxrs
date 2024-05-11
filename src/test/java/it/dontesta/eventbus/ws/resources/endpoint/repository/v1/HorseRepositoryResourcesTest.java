package it.dontesta.eventbus.ws.resources.endpoint.repository.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HorseRepositoryResourcesTest {

  @Test
  @Order(1)
  void getAllHorsesSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1")
        .then()
        .statusCode(Response.Status.OK.getStatusCode());
  }

  @Test
  @Order(2)
  void getHorseByIdSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1/1")
        .then()
        .body("name", is("Thunder"))
        .statusCode(Response.Status.OK.getStatusCode());
  }

  @Test
  @Order(3)
  void getHorseByIdNotFound() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode())
        .body("error", is("Horse with id of 100 not found"));
  }

  @Test
  @Order(4)
  void testCreateHorseBodyNull() {
    given()
        .contentType(ContentType.JSON)
        .when().post("/api/rest/repository/horse/v1")
        .then()
        .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
        .body("title", is("Constraint Violation"));
  }

  @Test
  @Order(5)
  void testCreateHorse() {
    String json = """
        {
          "name": "Santos XVVI",
          "coat": "Gray",
          "breed": "Pura Raza Española - PRE",
          "sex": "M",
          "dateOfBirth": "2024-05-01",
          "owners": [{"id": 3}]
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().post("/api/rest/repository/horse/v1")
        .then()
        .statusCode(Response.Status.CREATED.getStatusCode())
        .body("name", is("Santos XVVI"));
  }

  @Test
  @Order(6)
  void testUpdateHorse() {
    String json = """
        {
          "name": "Whisper updated",
          "coat": "Gray",
          "breed": "Pura Raza Española - PRE",
          "sex": "M",
          "dateOfBirth": "2024-05-01",
          "owners": [{"id": 3}]
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().put("/api/rest/repository/horse/v1/4")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body("name", is("Whisper updated"));
  }

  @Test
  @Order(7)
  void testDeleteHorseByIdSuccess() {
    given()
        .when().delete("/api/rest/repository/horse/v1/1")
        .then()
        .statusCode(Response.Status.NO_CONTENT.getStatusCode());
  }

  @Test
  @Order(8)
  void testDeleteHorseByIdNotFound() {
    given()
        .when().delete("/api/rest/repository/horse/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode());
  }
}
