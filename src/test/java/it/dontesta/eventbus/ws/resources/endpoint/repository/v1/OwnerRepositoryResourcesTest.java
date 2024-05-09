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
class OwnerRepositoryResourcesTest {

  @Test
  @Order(1)
  void getAllOwnersSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/owner/v1")
        .then()
        .statusCode(Response.Status.OK.getStatusCode());
  }

  @Test
  @Order(2)
  void getOwnerByIdSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/owner/v1/1")
        .then()
        .body("name", is("John"))
        .statusCode(Response.Status.OK.getStatusCode());
  }

  @Test
  @Order(3)
  void getOwnerByIdNotFound() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/owner/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode())
        .body("error", is("Owner with id of 100 not found"));
  }

  @Test
  @Order(4)
  void testCreateOwnerBodyNull() {
    given()
        .contentType(ContentType.JSON)
        .when().post("/api/rest/repository/owner/v1")
        .then()
        .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
        .body("title", is("Constraint Violation"));
  }

  @Test
  @Order(5)
  void testCreateOwner() {
    String json = """
        {
          "name": "Rossella",
          "surname": "Germanà",
          "email": "rossella.germana@dontesta.it",
          "phoneNumber": "987654321",
          "address": "Via Palermo 1",
          "city": "Catania",
          "state": "CT",
          "zipCode": "95100",
          "country": "Italy",
          "horses": [{"id": 1}]
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().post("/api/rest/repository/owner/v1")
        .then()
        .statusCode(Response.Status.CREATED.getStatusCode())
        .body("name", is("Rossella"));
  }

  @Test
  @Order(6)
  void testDeleteOwnerByIdNotFound() {
    given()
        .when().delete("/api/rest/repository/owner/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode());
  }
}
