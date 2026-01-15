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
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = { "customer_id", "windowStart" }),
})
public class UsageStats implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "usageStatsSeq")
    @SequenceGenerator(name = "usageStatsSeq", sequenceName = "usage_stats_seq")
    private long id;

    @ManyToOne
    private Customer customer;

    @Column(nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    @Convert(converter = DurationSecondsConverter.class)
    private Duration duration;

    @Column(nullable = false)
    private long count = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsageStats that = (UsageStats) o;
        return id == that.id
            && Objects.equals(customer, that.customer)
            && Objects.equals(windowStart, that.windowStart)
            && Objects.equals(duration, that.duration)
            && Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customer, windowStart, duration, count);
    }

    @Override
    public String toString() {
        return "UsageStats{" +
                "customer='" + customer.getName() + '\'' +
                ", windowStart=" + windowStart +
                ", duration=" + duration +
                ", count=" + count +
                '}';
    }
}
