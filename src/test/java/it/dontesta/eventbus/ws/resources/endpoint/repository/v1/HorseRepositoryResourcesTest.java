package it.dontesta.eventbus.ws.resources.endpoint.repository.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HorseRepositoryResourcesTest {

  @Test
  void getAllHorsesSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1")
        .then()
        .statusCode(200);
  }

  @Test
  void getHorseByIdSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1/1")
        .then()
        .body("name", is("Judio XXXV"))
        .statusCode(200);
  }

  @Test
  void getHorseByIdNotFound() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1/100")
        .then()
        .statusCode(404)
        .body("error", is("Horse with id of 100 not found"));
  }

  @Test
  void testCreateHorseBodyNull() {
    given()
        .contentType(ContentType.JSON)
        .when().post("/api/rest/repository/horse/v1")
        .then()
        .statusCode(400)
        .body("title", is("Constraint Violation"));
  }

  @Test
  void testDeleteHorseByIdSuccess() {
    given()
        .when().delete("/api/rest/repository/horse/v1/1")
        .then()
        .statusCode(204);
  }

  @Test
  void testDeleteHorseByIdNotFound() {
    given()
        .when().delete("/api/rest/repository/horse/v1/100")
        .then()
        .statusCode(404);
  }

}
