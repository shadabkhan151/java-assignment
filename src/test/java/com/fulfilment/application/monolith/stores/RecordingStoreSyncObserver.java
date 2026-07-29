package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test-only observer registered on the very same transaction phase as the production
 * {@link LegacyStoreSynchronizer}. Recording what it receives lets us assert that a change is
 * propagated when the transaction commits — and, more importantly, that it is <b>not</b>
 * propagated when the transaction rolls back.
 */
@ApplicationScoped
public class RecordingStoreSyncObserver {

  private final List<String> syncRecords = new CopyOnWriteArrayList<>();

  void onStoreChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreSyncEvent event) {
    syncRecords.add(event.getOperation() + ":" + event.getName());
  }

  public List<String> recorded() {
    return syncRecords;
  }

  public void clear() {
    syncRecords.clear();
  }
}
