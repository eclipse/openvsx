/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.extension_control;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.ExtensionId;
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.CACHE_MALICIOUS_EXTENSIONS;

@Component
public class ExtensionControlService {

    protected final Logger logger = LoggerFactory.getLogger(ExtensionControlService.class);

    private final JobRequestScheduler scheduler;
    private final RepositoryService repositories;
    private final EntityManager entityManager;
    private final SearchUtilService search;
    private final CacheService cache;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorEnabled;

    @Value("${ovsx.extension-control.enabled:true}")
    boolean enabled;

    @Value("${ovsx.extension-control.update-on-start:false}")
    boolean updateOnStart;

    @Value("${ovsx.migrations.delay.seconds:0}")
    long delay;

    public ExtensionControlService(
            JobRequestScheduler scheduler,
            RepositoryService repositories,
            EntityManager entityManager,
            SearchUtilService search,
            CacheService cache
    ) {
        this.scheduler = scheduler;
        this.repositories = repositories;
        this.entityManager = entityManager;
        this.search = search;
        this.cache = cache;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        if(!enabled || mirrorEnabled) {
            return;
        }
        if(updateOnStart) {
            scheduler.schedule(TimeUtil.getCurrentUTC().plusSeconds(delay), new HandlerJobRequest<>(ExtensionControlJobRequestHandler.class));
        }

        scheduler.scheduleRecurrently("UpdateExtensionControl", Cron.daily(1, 8), ZoneId.of("UTC"), new HandlerJobRequest<>(ExtensionControlJobRequestHandler.class));
    }

    @Transactional
    public UserData createExtensionControlUser() {
        var userName = "ExtensionControlUser";
        var user = repositories.findUserByLoginName("system", userName);
        if(user == null) {
            user = new UserData();
            user.setProvider("system");
            user.setLoginName(userName);
            entityManager.persist(user);
        }
        return user;
    }

    @Transactional
    public void updateExtension(ExtensionId extensionId, boolean deprecated, ExtensionId replacementId, boolean downloadable) {
        var extension = repositories.findExtension(extensionId.extension(), extensionId.namespace());
        if(extension == null) {
            return;
        }

        var wasDeprecated = extension.isDeprecated();
        extension.setDeprecated(deprecated);
        extension.setDownloadable(downloadable);
        if(replacementId != null) {
            var replacement = repositories.findExtension(replacementId.extension(), replacementId.namespace());
            extension.setReplacement(replacement);
        }
        if(deprecated != wasDeprecated) {
            cache.evictNamespaceDetails(extension);
            cache.evictLatestExtensionVersion(extension);
            cache.evictExtensionJsons(extension);
            search.updateSearchEntry(extension);
        }
    }

    public JsonNode getExtensionControlJson() throws IOException {
        var url = new URL("https://github.com/open-vsx/publish-extensions/raw/master/extension-control/extensions.json");
        return new ObjectMapper().readValue(url, JsonNode.class);
    }

    @Cacheable(CACHE_MALICIOUS_EXTENSIONS)
    public List<String> getMaliciousExtensionIds() throws IOException {
        var json = getExtensionControlJson();
        var malicious = json.get("malicious");
        if(!malicious.isArray()) {
            logger.error("field 'malicious' is not an array");
            return Collections.emptyList();
        }

        var list = new ArrayList<String>();
        malicious.forEach(node -> list.add(node.asText()));
        return list;
    }
}
