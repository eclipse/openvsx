/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.openvsx.entities;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = { "name" }),
})
public class Customer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "customerSeq")
    @SequenceGenerator(name = "customerSeq", sequenceName = "customer_seq")
    private long id;

    private String name;

    @ManyToOne
    private Tier tier;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    private List<String> cidrBlocks = Collections.emptyList();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tier getTier() {
        return tier;
    }

    public void setTier(Tier tier) {
        this.tier = tier;
    }

    public List<String> getCidrBlocks() {
        return cidrBlocks;
    }

    public void setCidrs(List<String> cidrBlocks) {
        this.cidrBlocks = cidrBlocks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer that = (Customer) o;
        return id == that.id
                && Objects.equals(name, that.name)
                && Objects.equals(tier, that.tier)
                && Objects.equals(cidrBlocks, that.cidrBlocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, tier, cidrBlocks);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "name='" + name + '\'' +
                ", tier=" + tier +
                ", cidrBlocks=" + cidrBlocks +
                '}';
    }
}
