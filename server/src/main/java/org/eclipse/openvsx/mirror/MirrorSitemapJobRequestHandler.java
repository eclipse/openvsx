/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.schedule.Scheduler;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@Component
public class MirrorSitemapJobRequestHandler implements JobRequestHandler<MirrorSitemapJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorSitemapJobRequestHandler.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    Scheduler scheduler;

    @Value("${ovsx.data.mirror.server-url:}")
    String serverUrl;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean enabled;

    @Override
    @Job(name="Mirror Sitemap", retries=10)
    public void run(MirrorSitemapJobRequest jobRequest) throws Exception {
        if(!enabled) {
            return;
        }

        LOGGER.info(">> Starting MirrorSitemapJob");
        var extensionIds = new ArrayList<String>();
        try(var reader = new StringReader(getSitemap())) {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            var sitemap = builder.parse(new InputSource(reader));
            var urls = sitemap.getElementsByTagName("url");
            for(var i = 0; i < urls.getLength(); i++) {
                var url = (Element) urls.item(i);
                var location = URI.create(url.getElementsByTagName("loc").item(0).getTextContent());
                var pathParams = location.getPath().split("/");
                var namespace = pathParams[pathParams.length - 2];
                var extension = pathParams[pathParams.length - 1];
                var lastModified = url.getElementsByTagName("lastmod").item(0).getTextContent();
                scheduler.enqueueMirrorExtension(namespace, extension, lastModified);
                extensionIds.add(String.join(".", namespace, extension));
            }
        }

        repositories.findAllNotMatchingByExtensionId(extensionIds).forEach(extension -> {
            scheduler.enqueueDeleteExtension(extension.getNamespace().getName(), extension.getName());
        });
        LOGGER.info("<< Completed MirrorSitemapJob");
    }

    private String getSitemap() {
        var requestUrl = URI.create(createApiUrl(serverUrl, "sitemap.xml"));
        var request = new RequestEntity<Void>(HttpMethod.GET, requestUrl);
        var response = restTemplate.exchange(request, String.class);
        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }
}
