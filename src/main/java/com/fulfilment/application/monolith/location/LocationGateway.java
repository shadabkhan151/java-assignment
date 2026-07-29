package com.fulfilment.application.monolith.location;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
/**
 * In-memory implementation of {@link LocationResolver}.
 *
 * <p>It is annotated with {@code @ApplicationScoped} so that it can be injected as the
 * {@link LocationResolver} port used by the warehouse use cases. In a real system this would be a
 * gateway to a master-data service; the port keeps that swap a one-line change.
 */
@ApplicationScoped
public class LocationGateway implements LocationResolver {

  private static final List<Location> locations = new ArrayList<>();

  static {
    locations.add(new Location("ZWOLLE-001", 1, 40));
    locations.add(new Location("ZWOLLE-002", 2, 50));
    locations.add(new Location("AMSTERDAM-001", 5, 100));
    locations.add(new Location("AMSTERDAM-002", 3, 75));
    locations.add(new Location("TILBURG-001", 1, 40));
    locations.add(new Location("HELMOND-001", 1, 45));
    locations.add(new Location("EINDHOVEN-001", 2, 70));
    locations.add(new Location("VETSBY-001", 1, 90));
  }

  /**
   * Resolves a location by its identification.
   *
   * @param identifier the location code, e.g. {@code ZWOLLE-001}. Matching is case-insensitive and
   *     tolerant to surrounding whitespace.
   * @return the matching {@link Location}, or {@code null} when the identifier is unknown. Returning
   *     {@code null} (instead of throwing) keeps this gateway a plain lookup: deciding that an
   *     unknown location is a business error is the responsibility of the use cases.
   */
  @Override
  public Location resolveByIdentifier(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return null;
    }

    final String normalized = identifier.trim();

    return locations.stream()
            .filter(location -> location.identification.equalsIgnoreCase(normalized))
            .findFirst()
            .orElse(null);
  }
}
