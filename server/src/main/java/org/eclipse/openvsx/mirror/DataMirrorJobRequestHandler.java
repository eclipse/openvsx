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

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.admin.AdminService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@Component
public class DataMirrorJobRequestHandler implements JobRequestHandler<DataMirrorJobRequest> {

    protected final Logger logger = LoggerFactory.getLogger(DataMirrorJobRequestHandler.class);

    @Autowired(required = false)
    DataMirrorService data;

    @Value("${ovsx.data.mirror.schedule:}")
    String schedule;

    @Autowired
    RepositoryService repositories;

    @Autowired
    RestTemplate backgroundRestTemplate;
    
    @Autowired
    UrlConfigService urlConfigService;

    @Autowired
    AdminService admin;

    @Autowired
    MirrorExtensionService mirrorExtensionService;

    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Job(name="Data Mirror")
    public void run(DataMirrorJobRequest jobRequest) throws Exception {
        if (data == null) {
            return;
        }

        logger.debug(">> Starting DataMirrorJob");
        try {
            var mirrorUser = data.createMirrorUser();
                
            var extensionIds = new ArrayList<String>();
            try(var reader = new StringReader(getSitemap())) {
                var factory = DocumentBuilderFactory.newInstance();
                var builder = factory.newDocumentBuilder();
                var sitemap = builder.parse(new InputSource(reader));
                var urls = sitemap.getElementsByTagName("url");
                var progress = jobContext().progressBar(urls.getLength());
                for(var i = 0; i < urls.getLength(); i++) {
                    var url = (Element) urls.item(i);
                    var location = URI.create(url.getElementsByTagName("loc").item(0).getTextContent());
                    var pathParams = location.getPath().split("/");
                    var namespace = pathParams[pathParams.length - 2];
                    var extension = pathParams[pathParams.length - 1];
                    var extensionId = NamingUtil.toExtensionId(namespace, extension);
                    if (!data.match(namespace, extension)) {
                        jobContext().logger().info("excluded, skipping " + extensionId + " (" + (i+1) + "/" +  urls.getLength() + ")");
                        continue;
                    }
                    jobContext().logger().info("mirroring " + extensionId + " (" + (i+1) + "/" +  urls.getLength() + ")");
                    
                    LocalDate lastModified = null;
                    try {
                        var lastModifiedString = url.getElementsByTagName("lastmod").item(0).getTextContent();
                        lastModified = LocalDate.parse(lastModifiedString, dateFormatter);
                    } catch(Throwable t) {
                        logger.error("failed to resolve last modified date " + extensionId, t);
                    }
                    try {
                        mirrorExtensionService.mirrorExtension(namespace, extension, mirrorUser, lastModified, jobContext());
                    } catch (Throwable t) {
                        logger.error("failed to mirror " + extensionId, t);
                    }
                    extensionIds.add(extensionId);
                    progress.increaseByOne();
                }
            }

            var notMatchingExtensions = repositories.findAllNotMatchingByExtensionId(extensionIds);
            if (!notMatchingExtensions.isEmpty()) {
                for(var extension : notMatchingExtensions) {
                    var extensionId = NamingUtil.toExtensionId(extension);
                    jobContext().logger().info("deleting " + extensionId);
                    try {
                        var namespace = extension.getNamespace();
                        admin.deleteExtension(namespace.getName(), extension.getName(), mirrorUser);
                    } catch (ErrorResultException t) { 
                        if (t.getStatus() != HttpStatus.NOT_FOUND) {
                            logger.warn("mirror: failed to delete extension " + extensionId, t);
                        }
                    } catch (Throwable t) {
                        logger.error("mirror: failed to delete extension " + extensionId,  t);
                    }
                }
            }
        } catch (Throwable t) {
            logger.error("failed to mirror data", t);
            throw t;
        }finally {
            logger.debug("<< Completed DataMirrorJob");
        } 
    }

    private String getSitemap() {
        var requestUrl = URI.create(createApiUrl(urlConfigService.getMirrorServerUrl(), "sitemap.xml"));
        var request = new RequestEntity<Void>(HttpMethod.GET, requestUrl);
        var response = backgroundRestTemplate.exchange(request, String.class);
        return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
    }
}
