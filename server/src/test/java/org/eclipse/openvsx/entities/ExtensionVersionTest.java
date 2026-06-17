/** ******************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExtensionVersionTest {

    @Test
    void setStateRejectsTransitionAwayFromDeleted() {
        var version = new ExtensionVersion();
        version.setState(ExtensionVersion.State.DELETED);

        assertThrows(IllegalStateException.class, () -> version.setState(ExtensionVersion.State.ACTIVE));
        assertThrows(IllegalStateException.class, () -> version.setState(ExtensionVersion.State.INACTIVE));
        assertThat(version.getState()).isEqualTo(ExtensionVersion.State.DELETED);
        assertThat(version.isActive()).isFalse();
    }

    @Test
    void setStateDeletedIsIdempotent() {
        var version = new ExtensionVersion();
        version.setState(ExtensionVersion.State.DELETED);

        version.setState(ExtensionVersion.State.DELETED);
        assertThat(version.getState()).isEqualTo(ExtensionVersion.State.DELETED);
    }

    @Test
    void setActiveOnDeletedVersionThrows() {
        var version = new ExtensionVersion();
        version.setState(ExtensionVersion.State.DELETED);

        assertThrows(IllegalStateException.class, () -> version.setActive(false));
        assertThrows(IllegalStateException.class, () -> version.setActive(true));
        assertThat(version.getState()).isEqualTo(ExtensionVersion.State.DELETED);
        assertThat(version.isActive()).isFalse();
    }

    @Test
    void setActiveTransitionsBetweenActiveAndInactive() {
        var version = new ExtensionVersion();

        version.setActive(false);
        assertThat(version.getState()).isEqualTo(ExtensionVersion.State.INACTIVE);
        assertThat(version.isActive()).isFalse();

        version.setActive(true);
        assertThat(version.getState()).isEqualTo(ExtensionVersion.State.ACTIVE);
        assertThat(version.isActive()).isTrue();
    }
}
