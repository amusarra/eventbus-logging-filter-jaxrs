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
          "country": "Italy"
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
  void testCreateOwnerWithId() {
    String json = """
        {
          "id": 1,
          "name": "Rossella",
          "surname": "Germanà",
          "email": "rossella.germana@dontesta.it",
          "phoneNumber": "987654321",
          "address": "Via Palermo 1",
          "city": "Catania",
          "state": "CT",
          "zipCode": "95100",
          "country": "Italy"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().post("/api/rest/repository/owner/v1")
        .then()
        .statusCode(422)
        .body("error", is("Id was invalidly set on request."));
  }

  @Test
  @Order(7)
  void testUpdateOwner() {
    String json = """
        {
          "id": 1,
          "name": "John Updated 3",
          "surname": "Doe",
          "email": "john2.doe@dontesta.it",
          "phoneNumber": "123456789",
          "address": "Via Roma 3",
          "city": "Rome",
          "state": "RM",
          "zipCode": "00100",
          "country": "Italy"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().put("/api/rest/repository/owner/v1")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body("name", is("John Updated 3"));
  }
  @Test
  @Order(8)
  void testUpdateOwnerWithoutId() {
    String json = """
        {
          "name": "John Updated 3",
          "surname": "Doe",
          "email": "john2.doe@dontesta.it",
          "phoneNumber": "123456789",
          "address": "Via Roma 3",
          "city": "Rome",
          "state": "RM",
          "zipCode": "00100",
          "country": "Italy"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().put("/api/rest/repository/owner/v1")
        .then()
        .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
        .body("error", is("Owner ID was not set on request."));
  }

  @Test
  @Order(9)
  void testUpdateOwnerNotFoundId() {
    String json = """
        {
          "id": 100,
          "name": "John Updated 3",
          "surname": "Doe",
          "email": "john2.doe@dontesta.it",
          "phoneNumber": "123456789",
          "address": "Via Roma 3",
          "city": "Rome",
          "state": "RM",
          "zipCode": "00100",
          "country": "Italy"
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().put("/api/rest/repository/owner/v1")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode())
        .body("error", is("Owner with id of 100 not found"));
  }

  @Test
  @Order(10)
  void testDeleteOwnerByIdNotFound() {
    given()
        .when().delete("/api/rest/repository/owner/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  @Order(11)
  void testDeleteOwnerByIdSuccess() {
    given()
        .when().delete("/api/rest/repository/owner/v1/1")
        .then()
        .statusCode(Response.Status.NO_CONTENT.getStatusCode());
  }

}
