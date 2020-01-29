/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Publisher;

public interface ExtensionRepository extends Repository<Extension, Long> {

    Streamable<Extension> findByPublisher(Publisher publisher);

    Streamable<Extension> findByPublisherOrderByNameAsc(Publisher publisher);

    Extension findByNameAndPublisher(String name, Publisher publisher);

    Extension findByNameAndPublisherName(String name, String publisherName);

    Streamable<Extension> findAll();

}