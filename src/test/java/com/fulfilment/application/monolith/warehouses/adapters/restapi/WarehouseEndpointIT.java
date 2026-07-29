package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

@QuarkusIntegrationTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointIT {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  public void testSimpleListWarehouses() {

    // List all, should have all 3 warehouses the database has initially:
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  public void testSimpleCheckingArchivingWarehouses() {

    // List all, should have all 3 warehouses the database has initially:
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the ZWOLLE-001 one (the archive endpoint accepts the business unit code as well as
    // the surrogate id, so "1" resolves to MWH.001):
    given().when().delete(PATH + "/1").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            not(containsString("ZWOLLE-001")),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));
  }
}
