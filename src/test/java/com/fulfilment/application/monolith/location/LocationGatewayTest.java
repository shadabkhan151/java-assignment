package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LocationGatewayTest {

  private final LocationGateway locationGateway = new LocationGateway();

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    // when
    Location location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    // then
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  public void testWhenResolveUnknownLocationShouldReturnNull() {
    assertNull(locationGateway.resolveByIdentifier("MARS-001"));
  }

  @Test
  public void testWhenResolveWithNullOrBlankShouldReturnNull() {
    assertNull(locationGateway.resolveByIdentifier(null));
    assertNull(locationGateway.resolveByIdentifier("  "));
  }

  @Test
  public void testResolveIsCaseInsensitiveAndTrimmed() {
    Location location = locationGateway.resolveByIdentifier("  eindhoven-001 ");

    assertEquals("EINDHOVEN-001", location.identification);
  }
}
