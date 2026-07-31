/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.publish;

import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.UserJson;

/**
 * Deployment-specific publisher agreement rules. All methods default to no-ops;
 * registries that require publishers to sign an agreement contribute an
 * implementation, typically through Spring Boot auto-configuration.
 */
public interface PublisherAgreementService {

    /**
     * Check whether the given user is allowed to publish extensions.
     * @throws org.eclipse.openvsx.util.ErrorResultException if publishing is not allowed
     */
    default void checkPublisherAgreement(UserData user) {
    }

    /**
     * Add agreement status to the user's own profile data.
     */
    default void enrichUserJsonWithPublisherAgreement(UserJson json, UserData user) {
    }

    /**
     * Add agreement status to the admin view of a user.
     */
    default void adminEnrichUserJson(UserJson json, UserData user) {
    }

    /**
     * Revoke the user's agreement; called when an admin revokes a publisher's
     * contributions. Implementations decide themselves whether there is anything
     * to revoke for the given user.
     */
    default void revokePublisherAgreement(UserData user, UserData admin) {
    }
}
