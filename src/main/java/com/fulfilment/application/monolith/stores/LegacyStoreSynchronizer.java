package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Propagates {@link Store} changes to the legacy system <b>only after the database transaction has
 * been successfully committed</b>.
 *
 * <p>This is achieved with a transactional CDI observer: {@code TransactionPhase.AFTER_SUCCESS}
 * makes the container register a JTA {@code Synchronization} and deliver the event in
 * {@code afterCompletion(STATUS_COMMITTED)}. If the transaction rolls back — a constraint
 * violation on {@code name}, a failure later in the same request, an exception mapper turning the
 * call into a 4xx — the observer is simply never invoked, so the legacy system never sees data that
 * does not exist on our side.
 *
 * <p>The alternative would be to inject {@code TransactionSynchronizationRegistry} and register a
 * {@code Synchronization} by hand in the resource. The event based version keeps the resource free
 * of transaction plumbing and makes it trivial to add more downstream listeners later.
 *
 * <p>Note the remaining caveat, which is inherent to any "call after commit" approach: the commit
 * and the downstream call are not atomic, so a crash in between loses the notification. If that
 * matters, the next step is a transactional outbox (write the message in the same transaction,
 * relay it asynchronously with retries and idempotency keys).
 */
@ApplicationScoped
public class LegacyStoreSynchronizer {

    private static final Logger LOGGER = Logger.getLogger(LegacyStoreSynchronizer.class.getName());

    @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

    public void onStoreChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreSyncEvent event) {
        Store store = event.toDetachedStore();

        try {
            switch (event.getOperation()) {
                case CREATED:
                    legacyStoreManagerGateway.createStoreOnLegacySystem(store);
                    break;
                case UPDATED:
                    legacyStoreManagerGateway.updateStoreOnLegacySystem(store);
                    break;
                default:
                    LOGGER.warnf("Unknown store synchronization operation: %s", event.getOperation());
            }
        } catch (RuntimeException e) {
            // The transaction is already committed at this point: failing here must not corrupt the
            // response the user already earned. Log it and let a retry/outbox mechanism deal with it.
            LOGGER.errorf(e, "Failed to synchronize store %s with the legacy system", event.getId());
        }
    }
}
