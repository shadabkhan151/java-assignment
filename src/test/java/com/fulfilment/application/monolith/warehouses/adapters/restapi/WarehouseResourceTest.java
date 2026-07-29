package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;

/**
 * End-to-end tests of the Warehouse API, running against the real stack (REST + use cases +
 * database) started by {@code @QuarkusTest}.
 *
 * <p>The methods are ordered because they tell one story on a shared database: MWH.100 is created,
 * queried, replaced and finally archived. Each negative test is written so that it leaves no trace.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseResourceTest {

  private static final String PATH = "warehouse";

  private static String warehouseJson(String buCode, String location, Integer capacity, Integer stock) {
    return String.format(
        "{\"businessUnitCode\": \"%s\", \"location\": \"%s\", \"capacity\": %s, \"stock\": %s}",
        buCode, location, capacity, stock);
  }

  @Test
  @Order(1)
  void shouldListTheSeededWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"));
  }

  @Test
  @Order(2)
  void shouldCreateAWarehouse() {
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.100", "EINDHOVEN-001", 40, 10))
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("MWH.100"))
        .body("location", is("EINDHOVEN-001"))
        .body("capacity", is(40));
  }

  @Test
  @Order(3)
  void shouldRejectAnAlreadyUsedBusinessUnitCode() {
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.001", "AMSTERDAM-002", 10, 0))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("already exists"));
  }

  @Test
  @Order(4)
  void shouldRejectAnUnknownLocation() {
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.404", "MARS-001", 10, 0))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("not a valid location"));
  }

  @Test
  @Order(5)
  void shouldRejectALocationWithoutAFreeSlot() {
    // TILBURG-001 accepts a single warehouse and MWH.023 already occupies it
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.101", "TILBURG-001", 5, 0))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("maximum number of warehouses"));
  }

  @Test
  @Order(6)
  void shouldRejectACapacityAboveTheLocationMaximum() {
    // VETSBY-001 tops at 90
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.102", "VETSBY-001", 200, 0))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("remaining capacity"));
  }

  @Test
  @Order(7)
  void shouldRejectAStockAboveTheWarehouseCapacity() {
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.103", "HELMOND-001", 40, 50))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("exceeds the warehouse capacity"));
  }

  @Test
  @Order(8)
  void shouldGetAWarehouseByBusinessUnitCode() {
    given()
        .when()
        .get(PATH + "/MWH.100")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("MWH.100"))
        .body("capacity", is(40));
  }

  @Test
  @Order(9)
  void shouldReturn404ForAnUnknownWarehouse() {
    given().when().get(PATH + "/MWH.999").then().statusCode(404);
  }

  @Test
  @Order(10)
  void shouldRejectAReplacementWithADifferentStock() {
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.100", "EINDHOVEN-001", 60, 99))
        .when()
        .post(PATH + "/MWH.100/replacement")
        .then()
        .statusCode(400)
        .body("error", containsString("must match"));

    // the transaction rolled back: the original warehouse is untouched and still active
    given().when().get(PATH + "/MWH.100").then().statusCode(200).body("capacity", is(40));
  }

  @Test
  @Order(11)
  void shouldReplaceAWarehouseReusingTheBusinessUnitCode() {
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.100", "EINDHOVEN-001", 60, 10))
        .when()
        .post(PATH + "/MWH.100/replacement")
        .then()
        .statusCode(200)
        .body("businessUnitCode", is("MWH.100"))
        .body("capacity", is(60));

    // a single active warehouse holds the business unit code, with the new capacity
    given().when().get(PATH + "/MWH.100").then().statusCode(200).body("capacity", is(60));
  }

  @Test
  @Order(12)
  void shouldRejectAReplacementForAnUnknownBusinessUnitCode() {
    given()
        .contentType("application/json")
        .body(warehouseJson("MWH.999", "EINDHOVEN-001", 10, 0))
        .when()
        .post(PATH + "/MWH.999/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(13)
  void shouldArchiveAWarehouse() {
    given().when().delete(PATH + "/MWH.100").then().statusCode(204);

    given().when().get(PATH + "/MWH.100").then().statusCode(404);

    given().when().get(PATH).then().statusCode(200).body(not(containsString("MWH.100")));
  }

  @Test
  @Order(14)
  void shouldReturn404WhenArchivingTwice() {
    given().when().delete(PATH + "/MWH.100").then().statusCode(404);
  }
}
