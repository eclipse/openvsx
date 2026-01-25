package org.eclipse.openvsx.events;

import org.eclipse.openvsx.entities.Namespace;
import org.springframework.context.ApplicationEvent;

public class NamespaceMembershipChangedEvent extends ApplicationEvent {
    private final Namespace namespace;

    public NamespaceMembershipChangedEvent(Object source, Namespace namespace) {
        super(source);
        this.namespace = namespace;
    }

    public Namespace getNamespace() {
        return namespace;
    }
}
