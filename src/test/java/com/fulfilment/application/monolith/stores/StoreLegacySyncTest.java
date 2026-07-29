package com.fulfilment.application.monolith.stores;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies task 2: the legacy system is notified only for changes that actually got committed. */
@QuarkusTest
public class StoreLegacySyncTest {

  @Inject RecordingStoreSyncObserver observer;

  @BeforeEach
  void setUp() {
    observer.clear();
  }

  @Test
  void shouldNotifyTheLegacySystemAfterASuccessfulCommit() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"SYNC-OK\", \"quantityProductsInStock\": 7}")
        .when()
        .post("store")
        .then()
        .statusCode(201);

    assertTrue(observer.recorded().contains("CREATED:SYNC-OK"));
  }

  @Test
  void shouldNotNotifyTheLegacySystemWhenTheTransactionRollsBack() {
    // "TONSTAD" is already taken and Store.name is unique: the insert blows up at flush/commit
    // time, i.e. *after* the resource method returned. With the previous implementation the legacy
    // system would already have been called at that point.
    given()
        .contentType("application/json")
        .body("{\"name\": \"TONSTAD\", \"quantityProductsInStock\": 1}")
        .when()
        .post("store")
        .then()
        .statusCode(greaterThanOrEqualTo(400));

    assertFalse(observer.recorded().contains("CREATED:TONSTAD"));
  }

  @Test
  void shouldNotifyTheLegacySystemOnUpdate() {
    String id =
        given()
            .contentType("application/json")
            .body("{\"name\": \"SYNC-UPDATE\", \"quantityProductsInStock\": 1}")
            .when()
            .post("store")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    observer.clear();

    given()
        .contentType("application/json")
        .body("{\"name\": \"SYNC-UPDATE\", \"quantityProductsInStock\": 42}")
        .when()
        .put("store/" + id)
        .then()
        .statusCode(200);

    assertTrue(observer.recorded().contains("UPDATED:SYNC-UPDATE"));
  }
}
