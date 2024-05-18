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
  void getAllHorsesLimitSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1?limit=10")
        .then()
        .statusCode(Response.Status.OK.getStatusCode());
  }

  @Test
  @Order(3)
  void getCountSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1/count")
        .then()
        .statusCode(Response.Status.OK.getStatusCode());
  }

  @Test
  @Order(4)
  void getHorseByIdSuccess() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1/1")
        .then()
        .body("name", is("Thunder"))
        .statusCode(Response.Status.OK.getStatusCode());
  }

  @Test
  @Order(5)
  void getHorseByIdNotFound() {
    given()
        .contentType(ContentType.JSON)
        .when().get("/api/rest/repository/horse/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode())
        .body("error", is("Horse with id of 100 not found"));
  }

  @Test
  @Order(6)
  void testCreateHorseBodyNull() {
    given()
        .contentType(ContentType.JSON)
        .when().post("/api/rest/repository/horse/v1")
        .then()
        .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
        .body("title", is("Constraint Violation"));
  }

  @Test
  @Order(7)
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
  @Order(8)
  void testCreateHorseWithId() {
    String json = """
        {
          "id": 100,
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
        .statusCode(422)
        .body("error", is("Id was invalidly set on request."));
  }

  @Test
  @Order(9)
  void testCreateHorses() {
    String json = """
        {
          "horses": [
            {
              "name": "Thunder",
              "coat": "Black",
              "breed": "PSA",
              "sex": "M",
              "dateOfBirth": "2024-05-11",
              "owners": [
                {
                  "id": 3
                }
              ]
            },
            {
              "name": "Furia",
              "coat": "White",
              "breed": "Quarter",
              "sex": "F",
              "dateOfBirth": "2024-05-12",
              "owners": [
                {
                  "id": 1
                }
              ]
            },
            {
              "name": "Focoso",
              "coat": "White",
              "breed": "Quarter",
              "sex": "F",
              "dateOfBirth": "2024-05-12",
              "owners": [
                {
                  "id": 1
                }
              ]
            }
          ]
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().post("/api/rest/repository/horse/v1/list")
        .then()
        .statusCode(Response.Status.CREATED.getStatusCode())
        .body("horses[0].name", is("Thunder"))
        .body("horses[1].name", is("Furia"));
  }

  @Test
  @Order(10)
  void testCreateHorsesWhitId() {
    String json = """
        {
          "horses": [
            {
              "id": 100,
              "name": "Thunder",
              "coat": "Black",
              "breed": "PSA",
              "sex": "M",
              "dateOfBirth": "2024-05-11",
              "owners": [
                {
                  "id": 3
                }
              ]
            },
            {
              "name": "Furia",
              "coat": "White",
              "breed": "Quarter",
              "sex": "F",
              "dateOfBirth": "2024-05-12",
              "owners": [
                {
                  "id": 1
                }
              ]
            }
          ]
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().post("/api/rest/repository/horse/v1/list")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(11)
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
  @Order(12)
  void testUpdateHorseNotFoundId() {
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
        .when().put("/api/rest/repository/horse/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode())
        .body("error", is("Horse with id of 100 not found"));
  }

  @Test
  @Order(13)
  void testUpdateHorseNewOwnerId() {
    String json = """
        {
          "name": "Whisper updated",
          "coat": "Gray",
          "breed": "Pura Raza Española - PRE",
          "sex": "M",
          "dateOfBirth": "2024-05-01",
          "owners": [{"id": 1}, {"id": 3}]
        }
        """;

    given()
        .contentType(ContentType.JSON)
        .body(json)
        .when().put("/api/rest/repository/horse/v1/1")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body("name", is("Whisper updated"));
  }

  @Test
  @Order(14)
  void testDeleteHorseByIdSuccess() {
    given()
        .when().delete("/api/rest/repository/horse/v1/1")
        .then()
        .statusCode(Response.Status.NO_CONTENT.getStatusCode());
  }

  @Test
  @Order(15)
  void testDeleteHorseByIdNotFound() {
    given()
        .when().delete("/api/rest/repository/horse/v1/100")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode());
  }

  @Test
  @Order(16)
  void testDeleteHorseAll() {
    given()
        .when().delete("/api/rest/repository/horse/v1/all")
        .then()
        .statusCode(Response.Status.NO_CONTENT.getStatusCode());
  }
}
