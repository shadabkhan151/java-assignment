package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasSize;

/**
 * Covers the bonus feature. The fixtures (store, products, warehouses) are created through the API
 * instead of relying on {@code import.sql}, so this test does not depend on what the other test
 * classes did to the seeded rows.
 *
 * <p>The four warehouses are created in AMSTERDAM-001, which allows 5 warehouses and 100 of
 * capacity in total (50 of which are already taken by the seeded MWH.012).
 */
@QuarkusTest
public class FulfillmentResourceTest {

  private static String storeId;
  private static final String[] PRODUCT_IDS = new String[6];

  private static String createStore(String name) {
    return given()
        .contentType("application/json")
        .body(String.format("{\"name\": \"%s\", \"quantityProductsInStock\": 0}", name))
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  private static String createProduct(String name) {
    return given()
        .contentType("application/json")
        .body(String.format("{\"name\": \"%s\", \"stock\": 0}", name))
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  private static void createWarehouse(String businessUnitCode) {
    given()
        .contentType("application/json")
        .body(
            String.format(
                "{\"businessUnitCode\": \"%s\", \"location\": \"AMSTERDAM-001\", \"capacity\": 10,"
                    + " \"stock\": 0}",
                businessUnitCode))
        .when()
        .post("warehouse")
        .then()
        .statusCode(200);
  }

  private static io.restassured.response.Response associate(
      String storeId, String productId, String businessUnitCode) {
    return given()
        .contentType("application/json")
        .body(
            String.format(
                "{\"storeId\": %s, \"productId\": %s, \"warehouseBusinessUnitCode\": \"%s\"}",
                storeId, productId, businessUnitCode))
        .when()
        .post("fulfillment");
  }

  @Test
  void shouldEnforceTheFulfillmentConstraints() {
    // ---------- fixtures ----------
    storeId = createStore("FULFIL-STORE");
    for (int i = 0; i < PRODUCT_IDS.length; i++) {
      PRODUCT_IDS[i] = createProduct("FULFIL-PRODUCT-" + (i + 1));
    }
    createWarehouse("FUL.001");
    createWarehouse("FUL.002");
    createWarehouse("FUL.003");
    createWarehouse("FUL.004");

    // ---------- constraint 1: max 2 warehouses per product per store ----------
    associate(storeId, PRODUCT_IDS[0], "FUL.001").then().statusCode(201);
    associate(storeId, PRODUCT_IDS[0], "FUL.002").then().statusCode(201);
    associate(storeId, PRODUCT_IDS[0], "FUL.003")
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 2 warehouses"));

    // the same association twice is rejected as well
    associate(storeId, PRODUCT_IDS[0], "FUL.001")
        .then()
        .statusCode(400)
        .body("error", containsString("already fulfils"));

    // ---------- constraint 2: max 3 warehouses per store ----------
    // third distinct warehouse for this store: allowed
    associate(storeId, PRODUCT_IDS[1], "FUL.003").then().statusCode(201);
    // a fourth one is not
    associate(storeId, PRODUCT_IDS[2], "FUL.004")
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 3 warehouses"));

    // ---------- constraint 3: max 5 product types per warehouse ----------
    // FUL.001 already stores product 1; add 4 more to reach the limit
    associate(storeId, PRODUCT_IDS[1], "FUL.001").then().statusCode(201);
    associate(storeId, PRODUCT_IDS[2], "FUL.001").then().statusCode(201);
    associate(storeId, PRODUCT_IDS[3], "FUL.001").then().statusCode(201);
    associate(storeId, PRODUCT_IDS[4], "FUL.001").then().statusCode(201);
    associate(storeId, PRODUCT_IDS[5], "FUL.001")
        .then()
        .statusCode(400)
        .body("error", containsString("maximum of 5 product types"));

    // ---------- unknown references ----------
    associate(storeId, PRODUCT_IDS[0], "MWH.999").then().statusCode(404);
    associate("999999", PRODUCT_IDS[0], "FUL.001").then().statusCode(404);

    // ---------- listing ----------
    given()
        .when()
        .get("fulfillment/store/" + storeId)
        .then()
        .statusCode(200)
        .body("$", hasSize(7));
  }
}
