package org.eclipse.openvsx.events;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Namespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class NamespaceMembershipChangedListener {
    private final CacheService cacheService;

    @Autowired
    public NamespaceMembershipChangedListener(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNamespaceMembershipChanged(NamespaceMembershipChangedEvent event) {
        Namespace namespace = event.getNamespace();
        cacheService.evictNamespaceDetails(namespace);
    }
}