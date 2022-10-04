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
import org.eclipse.openvsx.schedule.SchedulerService;
import org.eclipse.openvsx.util.TimeUtil;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.openvsx.schedule.JobUtil.completed;
import static org.eclipse.openvsx.schedule.JobUtil.starting;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@PersistJobDataAfterExecution
public class MirrorSitemapJob implements Job {
    private static final String LAST_EXECUTED = "lastExecuted";
    protected final Logger logger = LoggerFactory.getLogger(MirrorSitemapJob.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    SchedulerService schedulerService;

    @Autowired
    List<String> excludedExtensions;

    @Value("${ovsx.data.mirror.server-url}")
    String serverUrl;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        starting(context, logger);
        var lastExecuted = (LocalDate) context.getJobDetail().getJobDataMap().get(LAST_EXECUTED);
        logger.info("LAST EXECUTED: {}", lastExecuted);
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var timestamp = TimeUtil.getCurrentUTC().toEpochSecond(ZoneOffset.UTC);
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
                var extensionId = String.join(".", namespace, extension);
                if(excludedExtensions.contains(namespace + ".*") || excludedExtensions.contains(extensionId)) {
                    logger.info("Excluded {} extension, skipping", extensionId);
                    continue;
                }

                var lastModified = url.getElementsByTagName("lastmod").item(0).getTextContent();
                if(lastExecuted == null || !LocalDate.parse(lastModified, dateFormatter).isBefore(lastExecuted)) {
                    schedulerService.mirrorExtension(namespace, extension, lastModified);
                }

                extensionIds.add(extensionId);
            }
        } catch (ParserConfigurationException | IOException | SAXException | SchedulerException e) {
            throw new JobExecutionException(e);
        }

        var notMatchingExtensions = repositories.findAllNotMatchingByExtensionId(extensionIds);
        for(var extension : notMatchingExtensions) {
            try {
                schedulerService.mirrorDeleteExtension(extension.getNamespace().getName(), extension.getName(), timestamp);
            } catch (SchedulerException e) {
                throw new JobExecutionException(e);
            }
        }

        context.getJobDetail().getJobDataMap().put(LAST_EXECUTED, LocalDate.now());
        completed(context, logger);
    }

    private String getSitemap() {
        var requestUrl = URI.create(createApiUrl(serverUrl, "sitemap.xml"));
        var request = new RequestEntity<Void>(HttpMethod.GET, requestUrl);
        var response = restTemplate.exchange(request, String.class);
        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }
}
