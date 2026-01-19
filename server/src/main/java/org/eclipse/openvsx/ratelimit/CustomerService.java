
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
package org.eclipse.openvsx.ratelimit;

import inet.ipaddr.IPAddressString;
import org.eclipse.openvsx.entities.Customer;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.eclipse.openvsx.ratelimit.config.TieredRateLimitConfig.CACHE_RATE_LIMIT_CUSTOMER;

@Component
public class CustomerService {

    private final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    private final RepositoryService repositories;

    public CustomerService(RepositoryService repositories) {
        this.repositories = repositories;
    }

    @Cacheable(value = CACHE_RATE_LIMIT_CUSTOMER, key = "'id_' + #id", cacheManager = "rateLimitCacheManager")
    public Optional<Customer> getCustomerById(long id) {
        return repositories.findCustomerById(id);
    }

    @Cacheable(value = CACHE_RATE_LIMIT_CUSTOMER, cacheManager = "rateLimitCacheManager")
    public Optional<Customer> getCustomerByIpAddress(String ipAddress) {
        for (Customer customer : repositories.findAllCustomers()) {
            for (String cidrBlock : customer.getCidrBlocks()) {
                if (containsIP(cidrBlock, ipAddress)) {
                    return Optional.of(customer);
                }
            }
        }

        return Optional.empty();
    }

    private boolean containsIP(String cidrBlock, String ipAddress) {
        var block = new IPAddressString(cidrBlock);
        var ip = new IPAddressString(ipAddress);

        return block.contains(ip);
    }
}
