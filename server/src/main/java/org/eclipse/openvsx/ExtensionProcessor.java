/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.eclipse.openvsx.entities.ExtensionBinary;
import org.eclipse.openvsx.entities.ExtensionIcon;
import org.eclipse.openvsx.entities.ExtensionLicense;
import org.eclipse.openvsx.entities.ExtensionReadme;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.util.ArchiveUtil;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.LicenseDetection;
import org.springframework.data.util.Pair;

/**
 * Processes uploaded extension files and extracts their metadata.
 */
public class ExtensionProcessor implements AutoCloseable {

    private static final String PACKAGE_JSON = "extension/package.json";
    private static final String PACKAGE_NLS_JSON = "extension/package.nls.json";
    private static final String[] README = { "extension/README.md", "extension/README", "extension/README.txt" };
    private static final String[] LICENSE = { "extension/LICENSE.md", "extension/LICENSE", "extension/LICENSE.txt" };

    private final InputStream inputStream;
    private byte[] content;
    private ZipFile zipFile;
    private JsonNode packageJson;
    private JsonNode packageNlsJson;

    public ExtensionProcessor(InputStream stream) {
        this.inputStream = stream;
    }

    @Override
    public void close() {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    private void readInputStream() {
        try {
            content = ByteStreams.toByteArray(inputStream);
            var tempFile = File.createTempFile("extension_", ".vsix");
            Files.write(content, tempFile);
            zipFile = new ZipFile(tempFile);
        } catch (ZipException exc) {
            throw new ErrorResultException("Could not read zip file: " + exc.getMessage());
        } catch (EOFException exc) {
            throw new ErrorResultException("Could not read from input stream: " + exc.getMessage());
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    private void loadPackageJson() {
        if (packageJson == null) {
            readInputStream();

            // Read package.json
            var bytes = ArchiveUtil.readEntry(zipFile, PACKAGE_JSON);
            if (bytes == null)
                throw new ErrorResultException("Entry not found: " + PACKAGE_JSON);
            try {
                var mapper = new ObjectMapper();
                packageJson = mapper.readTree(bytes);
            } catch (JsonParseException exc) {
                throw new ErrorResultException("Invalid JSON format in " + PACKAGE_JSON
                        + ": " + exc.getMessage());
            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }

            // Read package.nls.json
            bytes = ArchiveUtil.readEntry(zipFile, PACKAGE_NLS_JSON);
            if (bytes != null) {
                try {
                    var mapper = new ObjectMapper();
                    packageNlsJson = mapper.readTree(bytes);
                } catch (JsonParseException exc) {
                    throw new ErrorResultException("Invalid JSON format in " + PACKAGE_NLS_JSON
                            + ": " + exc.getMessage());
                } catch (IOException exc) {
                    throw new RuntimeException(exc);
                }
            }
        }
    }

    public String getExtensionName() {
        loadPackageJson();
        return packageJson.path("name").asText();
    }

    public String getNamespace() {
        loadPackageJson();
        return packageJson.path("publisher").asText();
    }

    public List<String> getExtensionDependencies() {
        loadPackageJson();
        var result = getStringList(packageJson.path("extensionDependencies"));
        return result != null ? result : Collections.emptyList();
    }

    public List<String> getBundledExtensions() {
        loadPackageJson();
        var result = getStringList(packageJson.path("extensionPack"));
        return result != null ? result : Collections.emptyList();
    }

    public ExtensionVersion getMetadata() {
        loadPackageJson();
        var extension = new ExtensionVersion();
        extension.setVersion(packageJson.path("version").textValue());
        extension.setPreview(packageJson.path("preview").booleanValue());
        extension.setDisplayName(getNlsValue(packageJson.path("displayName")));
        extension.setDescription(getNlsValue(packageJson.path("description")));
        extension.setCategories(getStringList(packageJson.path("categories")));
        extension.setTags(getStringList(packageJson.path("keywords")));
        extension.setLicense(packageJson.path("license").textValue());
        extension.setHomepage(getUrl(packageJson.path("homepage")));
        extension.setRepository(getUrl(packageJson.path("repository")));
        extension.setBugs(getUrl(packageJson.path("bugs")));
        extension.setMarkdown(packageJson.path("markdown").textValue());
        var galleryBanner = packageJson.path("galleryBanner");
        if (galleryBanner.isObject()) {
            extension.setGalleryColor(galleryBanner.path("color").textValue());
            extension.setGalleryTheme(galleryBanner.path("theme").textValue());
        }
        extension.setQna(packageJson.path("qna").textValue());
        return extension;
    }

    private List<String> getStringList(JsonNode node) {
        if (node.isArray()) {
            var list = new ArrayList<String>();
            for (var element : node) {
                if (element.isTextual())
                    list.add(element.textValue());
            }
            return list;
        }
        return null;
    }

    private String getNlsValue(JsonNode node) {
        var value = node.textValue();
        if (packageNlsJson != null && value.length() > 2 && value.startsWith("%") && value.endsWith("%")) {
            var key = value.substring(1, value.length() - 1);
            return packageNlsJson.path(key).textValue();
        }
        return value;
    }

    private String getUrl(JsonNode node) {
        if (node.isTextual())
            return node.textValue();
        if (node.isObject())
            return node.path("url").textValue();
        return null;
    }

    public ExtensionBinary getBinary(ExtensionVersion extension) {
        var binary = new ExtensionBinary();
        binary.setExtension(extension);
        binary.setContent(content);
        return binary;
    }

    public ExtensionReadme getReadme(ExtensionVersion extension) {
        var result = readFromAlternateNames(README);
        if (result == null) {
            return null;
        }
        var readme = new ExtensionReadme();
        readme.setExtension(extension);
        readme.setContent(result.getFirst());
        extension.setReadmeFileName(result.getSecond());
        return readme;
    }

    public ExtensionLicense getLicense(ExtensionVersion extension) {
        var license = new ExtensionLicense();
        String licenseId;
        license.setExtension(extension);
        if (extension.getLicense() != null && extension.getLicense().toUpperCase().startsWith("SEE LICENSE IN ")) {
            var fileName = extension.getLicense().substring("SEE LICENSE IN ".length()).trim();
            extension.setLicense(null);
            var bytes = ArchiveUtil.readEntry(zipFile, "extension/" + fileName);
            if (bytes != null) {
                licenseId = this.getLicenseId(bytes);
                extension.setLicense(licenseId);
                license.setContent(bytes);
                var lastSegmentIndex = fileName.lastIndexOf('/');
                var lastSegment = fileName.substring(lastSegmentIndex + 1);
                extension.setLicenseFileName(lastSegment);
                return license;
            }
        }

        var result = readFromAlternateNames(LICENSE);
        if (result == null) {
            return null;
        }
        if (extension.getLicense() == null || extension.getLicense().isEmpty()) {
            licenseId = this.getLicenseId(result.getFirst());
            extension.setLicense(licenseId);
        }
        license.setContent(result.getFirst());
        extension.setLicenseFileName(result.getSecond());
        return license;
    }

    private String getLicenseId(byte[] text) {
        try {
            return LicenseDetection.detectLicense(new String(text, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private Pair<byte[], String> readFromAlternateNames(String[] names) {
        for (var name : names) {
            var bytes = ArchiveUtil.readEntry(zipFile, name);
            if (bytes != null) {
                var lastSegmentIndex = name.lastIndexOf('/');
                var lastSegment = name.substring(lastSegmentIndex + 1);
                return Pair.of(bytes, lastSegment);
            }
        }
        return null;
    }

    public ExtensionIcon getIcon(ExtensionVersion extension) {
        loadPackageJson();
        var iconPath = packageJson.get("icon");
        if (iconPath == null || !iconPath.isTextual())
            return null;
        var iconPathStr = iconPath.asText().replace('\\', '/');
        var bytes = ArchiveUtil.readEntry(zipFile, "extension/" + iconPathStr);
        if (bytes == null)
            return null;
        var icon = new ExtensionIcon();
        icon.setExtension(extension);
        icon.setContent(bytes);
        var fileNameIndex = iconPathStr.lastIndexOf('/');
        if (fileNameIndex >= 0)
            extension.setIconFileName(iconPathStr.substring(fileNameIndex + 1));
        else
            extension.setIconFileName(iconPathStr);
        return icon;
    }

}