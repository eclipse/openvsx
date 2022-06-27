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
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.ArchiveUtil;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.LicenseDetection;
import org.eclipse.openvsx.util.TargetPlatform;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

/**
 * Processes uploaded extension files and extracts their metadata.
 */
public class ExtensionProcessor implements AutoCloseable {

    private static final String VSIX_MANIFEST = "extension.vsixmanifest";
    private static final String PACKAGE_JSON = "extension/package.json";
    private static final String[] README = { "extension/README.md", "extension/README", "extension/README.txt" };
    private static final String[] LICENSE = { "extension/LICENSE.md", "extension/LICENSE", "extension/LICENSE.txt" };
    private static final String[] CHANGELOG = { "extension/CHANGELOG.md", "extension/CHANGELOG", "extension/CHANGELOG.txt" };

    private static final int MAX_CONTENT_SIZE = 512 * 1024 * 1024;
    private static final Pattern LICENSE_PATTERN = Pattern.compile("SEE( (?<license>\\S+))? LICENSE IN (?<file>\\S+)");

    private static final String WEB_EXTENSION_TAG = "__web_extension";

    protected final Logger logger = LoggerFactory.getLogger(ExtensionProcessor.class);

    private final InputStream inputStream;
    private byte[] content;
    private ZipFile zipFile;
    private JsonNode packageJson;
    private JsonNode vsixManifest;

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
        if (zipFile != null) {
            return;
        }
        try {
            if (inputStream != null)
                content = ByteStreams.toByteArray(inputStream);
            if (content.length > MAX_CONTENT_SIZE)
                throw new ErrorResultException("The extension package exceeds the size limit of 512 MB.", HttpStatus.PAYLOAD_TOO_LARGE);
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
        if (packageJson != null) {
            return;
        }
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
    }

    private void loadVsixManifest() {
        if (vsixManifest != null) {
            return;
        }

        readInputStream();

        // Read extension.vsixmanifest
        var bytes = ArchiveUtil.readEntry(zipFile, VSIX_MANIFEST);
        if (bytes == null)
            throw new ErrorResultException("Entry not found: " + VSIX_MANIFEST);
        
        try {
            var mapper = new XmlMapper();
            vsixManifest = mapper.readTree(bytes);
        } catch (JsonParseException exc) {
            throw new ErrorResultException("Invalid JSON format in " + VSIX_MANIFEST
                    + ": " + exc.getMessage());
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    private JsonNode findByIdInArray(Iterable<JsonNode> iter, String id) {
        for(JsonNode node : iter){
            var idNode = node.get("Id");
            if(idNode != null && idNode.asText().equals(id)){
                return node;
            }
        }
        return MissingNode.getInstance();
    }

    public String getExtensionName() {
        loadVsixManifest();
        return vsixManifest.path("Metadata").path("Identity").path("Id").asText();
    }

    public String getNamespace() {
        loadVsixManifest();
        return vsixManifest.path("Metadata").path("Identity").path("Publisher").asText();
    }

    public List<String> getExtensionDependencies() {
        loadVsixManifest();
        var extDepenNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Code.ExtensionDependencies");
        return asStringList(extDepenNode.path("Value").asText(), ",");
    }

    public List<String> getBundledExtensions() {
        loadVsixManifest();
        var extPackNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Code.ExtensionPack");
        return asStringList(extPackNode.path("Value").asText(), ",");
    }

    public List<String> getExtensionKinds() {
        loadVsixManifest();
        var extKindNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Code.ExtensionKind");
        return asStringList(extKindNode.path("Value").asText(), ",");
    }

    public String getHomepage() {
        loadVsixManifest();
        var extKindNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Services.Links.Learn");
        return extKindNode.path("Value").asText();
    }

    public String getRepository() {
        loadVsixManifest();
        var sourceNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Services.Links.Source");
        return sourceNode.path("Value").asText();
    }

    public String getBugs() {
        loadVsixManifest();
        var supportNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Services.Links.Support");
        return supportNode.path("Value").asText();
    }

    public String getGalleryColor() {
        loadVsixManifest();
        var colorNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Services.Branding.Color");
        return colorNode.path("Value").asText();
    }

    public String getGalleryTheme() {
        loadVsixManifest();
        var themeNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Services.Branding.Theme");
        return themeNode.path("Value").asText();
    }

    public boolean isPreview() {
        loadVsixManifest();
        var galleryFlags = vsixManifest.path("Metadata").path("GalleryFlags");
        return asStringList(galleryFlags.asText(), " ").contains("Preview");
    }

    public boolean isPreRelease() {
        loadVsixManifest();
        var preReleaseNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Code.PreRelease");
        return preReleaseNode.path("Value").asBoolean(false);
    }

    public ExtensionVersion getMetadata() {
        loadPackageJson();
        loadVsixManifest();
        var extension = new ExtensionVersion();
        extension.setVersion(vsixManifest.path("Metadata").path("Identity").path("Version").asText());
        extension.setTargetPlatform(getTargetPlatform());
        extension.setPreview(isPreview());
        extension.setPreRelease(isPreRelease());
        extension.setDisplayName(vsixManifest.path("Metadata").path("DisplayName").asText());
        extension.setDescription(vsixManifest.path("Metadata").path("Description").path("").asText());
        extension.setEngines(getEngines(packageJson.path("engines")));
        extension.setCategories(asStringList(vsixManifest.path("Metadata").path("Categories").asText(), ","));
        extension.setExtensionKind(getExtensionKinds());
        extension.setTags(getTags());
        extension.setLicense(packageJson.path("license").textValue());
        extension.setHomepage(getHomepage());
        extension.setRepository(getRepository());
        extension.setBugs(getBugs());
        extension.setMarkdown(packageJson.path("markdown").textValue());
        extension.setGalleryColor(getGalleryColor());
        extension.setGalleryTheme(getGalleryTheme());
        extension.setQna(packageJson.path("qna").textValue());

        return extension;
    }

    private String getTargetPlatform() {
        var targetPlatform = vsixManifest.path("Metadata").path("Identity").path("TargetPlatform").asText();
        if(targetPlatform.isEmpty()) {
            targetPlatform = TargetPlatform.NAME_UNIVERSAL;
        }

        return targetPlatform;
    }

    private List<String> getTags() {
        var tags = vsixManifest.path("Metadata").path("Tags").asText();
        return asStringList(tags, ",").stream()
                .collect(Collectors.groupingBy(String::toLowerCase))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue().get(0))
                .collect(Collectors.toList());
    }

    private List<String> asStringList(String value, String sep){
        if (Strings.isNullOrEmpty(value)){
            return new ArrayList<String>();
        }

        return Arrays.asList(value.split(sep));
    }

    private List<String> getEngines(JsonNode node) {
        if (node.isObject()) {
            var result = new ArrayList<String>();
            var fieldIter = node.fields();
            while (fieldIter.hasNext()) {
                var entry = fieldIter.next();
                result.add(entry.getKey() + "@" + entry.getValue().textValue());
            }
            return result;
        }
        return null;
    }

    public List<FileResource> getResources(ExtensionVersion extension) {
        var resources = new ArrayList<>(getAllResources(extension).collect(Collectors.toList()));
        var binary = getBinary(extension);
        if (binary != null)
            resources.add(binary);
        var manifest = getManifest(extension);
        if (manifest != null)
            resources.add(manifest);
        var readme = getReadme(extension);
        if (readme != null)
            resources.add(readme);
        var changelog = getChangelog(extension);
        if (changelog != null)
            resources.add(changelog);
        var license = getLicense(extension);
        if (license != null)
            resources.add(license);
        var icon = getIcon(extension);
        if (icon != null)
            resources.add(icon);

        return resources;
    }

    public void processEachResource(ExtensionVersion extension, Consumer<FileResource> processor) {
        getAllResources(extension).forEach(processor);
    }

    protected Stream<FileResource> getAllResources(ExtensionVersion extension) {
        readInputStream();
        return zipFile.stream()
                .map(zipEntry -> {
                    byte[] bytes;
                    try {
                        bytes = ArchiveUtil.readEntry(zipFile, zipEntry);
                    } catch(ErrorResultException exc) {
                        logger.warn("Failed to read entry", exc);
                        bytes = null;
                    }
                    if (bytes == null) {
                        return null;
                    }
                    var resource = new FileResource();
                    resource.setExtension(extension);
                    resource.setName(zipEntry.getName());
                    resource.setType(FileResource.RESOURCE);
                    resource.setContent(bytes);
                    return resource;
                })
                .filter(Objects::nonNull);
    }

    protected FileResource getBinary(ExtensionVersion extension) {
        var binary = new FileResource();
        binary.setExtension(extension);
        binary.setType(FileResource.DOWNLOAD);
        binary.setContent(content);
        return binary;
    }

    protected FileResource getManifest(ExtensionVersion extension) {
        readInputStream();
        var bytes = ArchiveUtil.readEntry(zipFile, PACKAGE_JSON);
        if (bytes == null) {
            return null;
        }
        var manifest = new FileResource();
        manifest.setExtension(extension);
        manifest.setName("package.json");
        manifest.setType(FileResource.MANIFEST);
        manifest.setContent(bytes);
        return manifest;
    }

    protected FileResource getReadme(ExtensionVersion extension) {
        readInputStream();
        var result = readFromAlternateNames(README);
        if (result == null) {
            return null;
        }
        var readme = new FileResource();
        readme.setExtension(extension);
        readme.setName(result.getSecond());
        readme.setType(FileResource.README);
        readme.setContent(result.getFirst());
        return readme;
    }

    public FileResource getChangelog(ExtensionVersion extension) {
        readInputStream();
        var result = readFromAlternateNames(CHANGELOG);
        if (result == null) {
            return null;
        }
        var changelog = new FileResource();
        changelog.setExtension(extension);
        changelog.setName(result.getSecond());
        changelog.setType(FileResource.CHANGELOG);
        changelog.setContent(result.getFirst());
        return changelog;
    }

    protected FileResource getLicense(ExtensionVersion extension) {
        readInputStream();
        var license = new FileResource();
        license.setExtension(extension);
        license.setType(FileResource.LICENSE);
        // Parse specifications in the form "SEE MIT LICENSE IN LICENSE.txt"
        if (!Strings.isNullOrEmpty(extension.getLicense())) {
            var matcher = LICENSE_PATTERN.matcher(extension.getLicense());
            if (matcher.find()) {
                extension.setLicense(matcher.group("license"));
                var fileName = matcher.group("file");
                var bytes = ArchiveUtil.readEntry(zipFile, "extension/" + fileName);
                if (bytes != null) {
                    var lastSegmentIndex = fileName.lastIndexOf('/');
                    var lastSegment = fileName.substring(lastSegmentIndex + 1);
                    license.setName(lastSegment);
                    license.setContent(bytes);
                    detectLicense(bytes, extension);
                    return license;
                }
            }
        }

        var result = readFromAlternateNames(LICENSE);
        if (result == null) {
            return null;
        }
        license.setName(result.getSecond());
        license.setContent(result.getFirst());
        detectLicense(result.getFirst(), extension);
        return license;
    }

    private void detectLicense(byte[] content, ExtensionVersion extension) {
        if (Strings.isNullOrEmpty(extension.getLicense())) {
            var detection = new LicenseDetection();
            extension.setLicense(detection.detectLicense(content));
        }
    }

    private Pair<byte[], String> readFromAlternateNames(String[] names) {
        for (var name : names) {
            var entry = ArchiveUtil.getEntryIgnoreCase(zipFile, name);
            if (entry != null) {
                var bytes = ArchiveUtil.readEntry(zipFile, entry);
                var lastSegmentIndex = entry.getName().lastIndexOf('/');
                var lastSegment = entry.getName().substring(lastSegmentIndex + 1);
                return Pair.of(bytes, lastSegment);
            }
        }
        return null;
    }

    protected FileResource getIcon(ExtensionVersion extension) {
        loadPackageJson();
        var iconPath = packageJson.get("icon");
        if (iconPath == null || !iconPath.isTextual())
            return null;
        var iconPathStr = iconPath.asText().replace('\\', '/');
        var bytes = ArchiveUtil.readEntry(zipFile, "extension/" + iconPathStr);
        if (bytes == null)
            return null;
        var icon = new FileResource();
        icon.setExtension(extension);
        var fileNameIndex = iconPathStr.lastIndexOf('/');
        if (fileNameIndex >= 0)
            icon.setName(iconPathStr.substring(fileNameIndex + 1));
        else
            icon.setName(iconPathStr);
        icon.setType(FileResource.ICON);
        icon.setContent(bytes);
        return icon;
    }

}