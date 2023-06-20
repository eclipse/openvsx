/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.web;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.UrlUtil;
import org.eclipse.openvsx.util.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@RestController
public class SitemapController {

    private static final String NAMESPACE_URI = "http://www.sitemaps.org/schemas/sitemap/0.9";

    @Autowired
    RepositoryService repositories;

    @Autowired
    VersionService versions;

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @GetMapping(path = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<StreamingResponseBody> getSitemap() throws ParserConfigurationException {
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        document.setXmlStandalone(true);
        var urlset = document.createElementNS(NAMESPACE_URI, "urlset");
        document.appendChild(urlset);
        
        var baseUrl = getBaseUrl();
        var timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        repositories.findAllActiveExtensions().forEach(extension -> {
            var entry = document.createElement("url");
            var loc = document.createElement("loc");
            var namespaceName = extension.getNamespace().getName();
            loc.setTextContent(UrlUtil.createApiUrl(baseUrl, "extension", namespaceName, extension.getName()));
            entry.appendChild(loc);

            var lastmod = document.createElement("lastmod");
            var latest = versions.getLatestTrxn(extension, null, false, true);
            lastmod.setTextContent(latest.getTimestamp().format(timestampFormatter));
            entry.appendChild(lastmod);
            urlset.appendChild(entry);
        });

        StreamingResponseBody stream = out -> {
            try {
                var transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                transformer.transform(new DOMSource(document), new StreamResult(out));
            } catch (TransformerException exc) {
                throw new RuntimeException(exc);
            }
        };
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .body(stream);
    }

    private String getBaseUrl() {
        if (StringUtils.isEmpty(webuiUrl))
            return UrlUtil.getBaseUrl();
        else if (URI.create(webuiUrl).isAbsolute())
            return webuiUrl;
        else
            return UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), webuiUrl.split("/"));
    }

}