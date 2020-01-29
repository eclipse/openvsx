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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.openvsx.entities.ExtensionReadme;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.util.ArchiveUtil;
import org.eclipse.openvsx.util.ErrorResultException;

/**
 * Processes uploaded extension files and extracts their metadata.
 */
public class ExtensionProcessor implements AutoCloseable {

    private static final String PACKAGE_JSON = "extension/package.json";
    private static final String README = "extension/README";
    private static final String README_MD = "extension/README.md";

    private final byte[] content;
    private final ZipFile zipFile;
    private JsonNode packageJson;

    public ExtensionProcessor(InputStream stream) {
        try {
            content = ByteStreams.toByteArray(stream);
            var tempFile = File.createTempFile("extension_", ".vsix");
            Files.write(content, tempFile);
            zipFile = new ZipFile(tempFile);
        } catch (ZipException exc) {
            throw new ErrorResultException("Could not read zip file: " + exc.getMessage());
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
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

    private void loadPackageJson() {
        if (packageJson == null) {
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
        }
    }

    public String getExtensionName() {
        loadPackageJson();
        return packageJson.path("name").asText();
    }

    public String getPublisherName() {
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
        extension.setDisplayName(packageJson.path("displayName").textValue());
        extension.setDescription(packageJson.path("description").textValue());
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
        var fileName = "README.md";
        var bytes = ArchiveUtil.readEntry(zipFile, README_MD);
        if (bytes == null) {
            fileName = "README";
            bytes = ArchiveUtil.readEntry(zipFile, README);
        }
        if (bytes == null)
            return null;
        var readme = new ExtensionReadme();
        readme.setExtension(extension);
        readme.setContent(bytes);
        extension.setReadmeFileName(fileName);
        return readme;
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