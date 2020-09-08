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

import java.net.URI;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.google.common.base.Strings;

import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class SitemapController {

    private static final String NAMESPACE_URI = "http://www.sitemaps.org/schemas/sitemap/0.9";

    @Autowired
    RepositoryService repositories;

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
        repositories.findAllExtensions().forEach(extension -> {
            var entry = document.createElement("url");
            var loc = document.createElement("loc");
            loc.setTextContent(UrlUtil.createApiUrl(baseUrl, "extension", extension.getNamespace().getName(), extension.getName()));
            entry.appendChild(loc);
            var lastmod = document.createElement("lastmod");
            lastmod.setTextContent(extension.getLatest().getTimestamp().format(timestampFormatter));
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
        return new ResponseEntity<>(stream, HttpStatus.OK);
    }

    private String getBaseUrl() {
        if (Strings.isNullOrEmpty(webuiUrl))
            return UrlUtil.getBaseUrl();
        else if (URI.create(webuiUrl).isAbsolute())
            return webuiUrl;
        else
            return UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), webuiUrl.split("/"));
    }

}