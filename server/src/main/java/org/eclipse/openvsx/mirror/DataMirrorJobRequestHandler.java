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

import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.admin.AdminService;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@Component
public class DataMirrorJobRequestHandler implements JobRequestHandler<DataMirrorJobRequest> {

    protected final Logger logger = LoggerFactory.getLogger(DataMirrorJobRequestHandler.class);

    private DataMirrorService data;
    private final RepositoryService repositories;
    private final RestTemplate backgroundRestTemplate;
    private final UrlConfigService urlConfigService;
    private final AdminService admin;
    private final MirrorExtensionService mirrorExtensionService;
    private final DateTimeFormatter dateFormatter;

    @Value("${ovsx.data.mirror.schedule:}")
    String schedule;

    public DataMirrorJobRequestHandler(
            Optional<DataMirrorService> dataMirrorService,
            RepositoryService repositories,
            RestTemplate backgroundRestTemplate,
            UrlConfigService urlConfigService,
            AdminService admin,
            MirrorExtensionService mirrorExtensionService
    ) {
        dataMirrorService.ifPresent(service -> this.data = service);
        this.repositories = repositories;
        this.backgroundRestTemplate = backgroundRestTemplate;
        this.urlConfigService = urlConfigService;
        this.admin = admin;
        this.mirrorExtensionService = mirrorExtensionService;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @Override
    @Job(name="Data Mirror")
    public void run(DataMirrorJobRequest jobRequest) throws Exception {
        if (data == null) {
            return;
        }

        logger.debug(">> Starting DataMirrorJob");
        var sitemap = getSitemap();
        if(sitemap == null) {
            logger.error("failed to fetch sitemap");
            return;
        }

        try {
            var mirrorUser = data.createMirrorUser();
            var extensionIds = processUrls(sitemap.getElementsByTagName("url"), mirrorUser);
            deleteOtherExtensions(extensionIds, mirrorUser);
        } catch (Exception e) {
            logger.error("failed to mirror data", e);
            throw e;
        }finally {
            logger.debug("<< Completed DataMirrorJob");
        } 
    }

    private List<String> processUrls(NodeList urls, UserData mirrorUser) {
        var extensionIds = new ArrayList<String>();
        var progress = jobContext().progressBar(urls.getLength());
        for(var i = 0; i < urls.getLength(); i++) {
            var url = (Element) urls.item(i);
            var extensionId = getExtensionId(url);
            var id = NamingUtil.fromExtensionId(extensionId);
            var namespace = id.namespace();
            var extension = id.extension();
            if (!data.match(namespace, extension)) {
                jobContext().logger().info("excluded, skipping " + extensionId + " (" + (i+1) + "/" +  urls.getLength() + ")");
                continue;
            }

            jobContext().logger().info("mirroring " + extensionId + " (" + (i+1) + "/" +  urls.getLength() + ")");
            try {
                LocalDate lastModified = getLastModified(url, extensionId);
                mirrorExtensionService.mirrorExtension(namespace, extension, mirrorUser, lastModified, jobContext());
            } catch (Exception e) {
                logger.error("failed to mirror {}", extensionId, e);
            }
            extensionIds.add(extensionId);
            progress.increaseByOne();
        }

        return extensionIds;
    }

    private void deleteOtherExtensions(List<String> extensionIds, UserData mirrorUser) {
        var notMatchingExtensions = repositories.findAllNotMatchingByExtensionId(extensionIds);
        for(var extension : notMatchingExtensions) {
            var extensionId = NamingUtil.toExtensionId(extension);
            jobContext().logger().info("deleting " + extensionId);
            try {
                var namespace = extension.getNamespace();
                admin.deleteExtension(namespace.getName(), extension.getName(), mirrorUser);
            } catch (ErrorResultException e) {
                if (e.getStatus() != HttpStatus.NOT_FOUND) {
                    logger.warn("mirror: failed to delete extension {}", extensionId, e);
                }
            } catch (Exception e) {
                logger.error("mirror: failed to delete extension {}", extensionId, e);
            }
        }
    }

    private Document getSitemap() throws IOException, SAXException, ParserConfigurationException {
        var requestUrl = URI.create(createApiUrl(urlConfigService.getMirrorServerUrl(), "sitemap.xml"));
        var request = new RequestEntity<Void>(HttpMethod.GET, requestUrl);
        var response = backgroundRestTemplate.exchange(request, String.class);
        var body = response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        if(body == null) {
            return null;
        }

        try(var reader = new StringReader(body)) {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(reader));
        }
    }

    private String getExtensionId(Element url) {
        var location = URI.create(url.getElementsByTagName("loc").item(0).getTextContent());
        var pathParams = location.getPath().split("/");
        var namespace = pathParams[pathParams.length - 2];
        var extension = pathParams[pathParams.length - 1];
        return NamingUtil.toExtensionId(namespace, extension);
    }

    private LocalDate getLastModified(Element url, String extensionId) {
        try {
            var lastModifiedString = url.getElementsByTagName("lastmod").item(0).getTextContent();
            return LocalDate.parse(lastModifiedString, dateFormatter);
        } catch(Exception e) {
            logger.error("failed to resolve last modified date {}", extensionId, e);
        }

        return null;
    }
}
