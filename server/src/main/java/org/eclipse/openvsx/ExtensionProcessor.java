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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.adapter.ExtensionQueryResult;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Processes uploaded extension files and extracts their metadata.
 */
public class ExtensionProcessor implements AutoCloseable {

    private static final String VSIX_MANIFEST = "extension.vsixmanifest";
    private static final String PACKAGE_JSON = "extension/package.json";
    private static final String[] README = { "extension/README.md", "extension/README", "extension/README.txt" };
    private static final String[] LICENSE = { "extension/LICENSE.md", "extension/LICENSE", "extension/LICENSE.txt" };
    private static final String[] CHANGELOG = { "extension/CHANGELOG.md", "extension/CHANGELOG", "extension/CHANGELOG.txt" };

    private static final Pattern LICENSE_PATTERN = Pattern.compile("SEE( (?<license>\\S+))? LICENSE IN (?<file>\\S+)");

    protected final Logger logger = LoggerFactory.getLogger(ExtensionProcessor.class);

    private final TempFile extensionFile;
    private ZipFile zipFile;
    private JsonNode packageJson;
    private JsonNode vsixManifest;

    public ExtensionProcessor(TempFile extensionFile) {
        this.extensionFile = extensionFile;
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
            zipFile = new ZipFile(extensionFile.getPath().toFile());
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

    public List<String> getLocalizedLanguages() {
        loadVsixManifest();
        var languagesNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Code.LocalizedLanguages");
        return asStringList(languagesNode.path("Value").asText(), ",");
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

    public String getSponsorLink() {
        loadVsixManifest();
        var sponsorLinkNode = findByIdInArray(vsixManifest.path("Metadata").path("Properties").path("Property"), "Microsoft.VisualStudio.Code.SponsorLink");
        return sponsorLinkNode.path("Value").asText();
    }

    public ExtensionVersion getMetadata() {
        loadPackageJson();
        loadVsixManifest();
        var extVersion = new ExtensionVersion();
        extVersion.setVersion(getVersion());
        extVersion.setTargetPlatform(getTargetPlatform());
        extVersion.setPreview(isPreview());
        extVersion.setPreRelease(isPreRelease());
        extVersion.setDisplayName(vsixManifest.path("Metadata").path("DisplayName").asText());
        extVersion.setDescription(vsixManifest.path("Metadata").path("Description").path("").asText());
        extVersion.setEngines(getEngines(packageJson.path("engines")));
        extVersion.setCategories(asStringList(vsixManifest.path("Metadata").path("Categories").asText(), ","));
        extVersion.setExtensionKind(getExtensionKinds());
        extVersion.setTags(getTags());
        extVersion.setLicense(packageJson.path("license").textValue());
        extVersion.setHomepage(getHomepage());
        extVersion.setRepository(getRepository());
        extVersion.setSponsorLink(getSponsorLink());
        extVersion.setBugs(getBugs());
        extVersion.setMarkdown(packageJson.path("markdown").textValue());
        extVersion.setGalleryColor(getGalleryColor());
        extVersion.setGalleryTheme(getGalleryTheme());
        extVersion.setLocalizedLanguages(getLocalizedLanguages());
        extVersion.setQna(packageJson.path("qna").textValue());

        return extVersion;
    }

    public String getVersion() {
        return vsixManifest.path("Metadata").path("Identity").path("Version").asText();
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
        if (StringUtils.isEmpty(value)){
            return new ArrayList<>();
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

    public List<FileResource> getFileResources(ExtensionVersion extVersion) {
        var resources = new ArrayList<FileResource>();
        var mappers = List.<Function<ExtensionVersion, FileResource>>of(
                this::getManifest, this::getReadme, this::getChangelog, this::getLicense, this::getIcon, this::getVsixManifest
        );

        mappers.forEach(mapper -> Optional.of(extVersion).map(mapper).ifPresent(resources::add));
        return resources;
    }

    public void processEachResource(ExtensionVersion extVersion, Consumer<FileResource> processor) {
        readInputStream();
        zipFile.stream()
                .filter(zipEntry -> !zipEntry.isDirectory())
                .map(zipEntry -> {
                    byte[] bytes;
                    try {
                        bytes = ArchiveUtil.readEntry(zipFile, zipEntry);
                    } catch(ErrorResultException exc) {
                        logger.warn(exc.getMessage());
                        bytes = null;
                    }
                    if (bytes == null) {
                        return null;
                    }
                    var resource = new FileResource();
                    resource.setExtension(extVersion);
                    resource.setName(zipEntry.getName());
                    resource.setType(FileResource.RESOURCE);
                    resource.setContent(bytes);
                    return resource;
                })
                .filter(Objects::nonNull)
                .forEach(processor);
    }

    public FileResource getBinary(ExtensionVersion extVersion, String binaryName) {
        if(binaryName == null) {
            binaryName = NamingUtil.toFileFormat(extVersion, ".vsix");
        }

        var binary = new FileResource();
        binary.setExtension(extVersion);
        binary.setName(binaryName);
        binary.setType(FileResource.DOWNLOAD);
        binary.setContent(null);
        return binary;
    }

    public FileResource generateSha256Checksum(ExtensionVersion extVersion) {
        String hash = null;
        try(var input = Files.newInputStream(extensionFile.getPath())) {
            hash = DigestUtils.sha256Hex(input);
        } catch (IOException e) {
            logger.error("Failed to read extensionFile", e);
        }

        if(hash == null) {
            return null;
        }

        var sha256 = new FileResource();
        sha256.setExtension(extVersion);
        sha256.setName(NamingUtil.toFileFormat(extVersion, ".sha256"));
        sha256.setType(FileResource.DOWNLOAD_SHA256);
        sha256.setContent(hash.getBytes(StandardCharsets.UTF_8));
        return sha256;
    }

    protected FileResource getManifest(ExtensionVersion extVersion) {
        readInputStream();
        var bytes = ArchiveUtil.readEntry(zipFile, PACKAGE_JSON);
        if (bytes == null) {
            return null;
        }
        var manifest = new FileResource();
        manifest.setExtension(extVersion);
        manifest.setName("package.json");
        manifest.setType(FileResource.MANIFEST);
        manifest.setContent(bytes);
        return manifest;
    }

    protected FileResource getReadme(ExtensionVersion extVersion) {
        var result = readFromVsixPackage(ExtensionQueryResult.ExtensionFile.FILE_DETAILS, README);
        if (result == null) {
            return null;
        }

        var readme = new FileResource();
        readme.setExtension(extVersion);
        readme.setName(result.getSecond());
        readme.setType(FileResource.README);
        readme.setContent(result.getFirst());
        return readme;
    }

    public FileResource getChangelog(ExtensionVersion extVersion) {
        var result = readFromVsixPackage(ExtensionQueryResult.ExtensionFile.FILE_CHANGELOG, CHANGELOG);
        if (result == null) {
            return null;
        }

        var changelog = new FileResource();
        changelog.setExtension(extVersion);
        changelog.setName(result.getSecond());
        changelog.setType(FileResource.CHANGELOG);
        changelog.setContent(result.getFirst());
        return changelog;
    }

    public FileResource getLicense(ExtensionVersion extVersion) {
        readInputStream();
        var license = new FileResource();
        license.setExtension(extVersion);
        license.setType(FileResource.LICENSE);
        // Parse specifications in the form "SEE MIT LICENSE IN LICENSE.txt"
        if (!StringUtils.isEmpty(extVersion.getLicense())) {
            var matcher = LICENSE_PATTERN.matcher(extVersion.getLicense());
            if (matcher.find()) {
                extVersion.setLicense(matcher.group("license"));
                var fileName = matcher.group("file");
                var bytes = ArchiveUtil.readEntry(zipFile, "extension/" + fileName);
                if (bytes != null) {
                    var lastSegmentIndex = fileName.lastIndexOf('/');
                    var lastSegment = fileName.substring(lastSegmentIndex + 1);
                    license.setName(lastSegment);
                    license.setContent(bytes);
                    detectLicense(bytes, extVersion);
                    return license;
                }
            }
        }

        var result = readFromVsixPackage(ExtensionQueryResult.ExtensionFile.FILE_LICENSE, LICENSE);
        if (result == null) {
            return null;
        }

        license.setName(result.getSecond());
        license.setContent(result.getFirst());
        detectLicense(result.getFirst(), extVersion);
        return license;
    }

    private void detectLicense(byte[] content, ExtensionVersion extVersion) {
        if (StringUtils.isEmpty(extVersion.getLicense())) {
            var detection = new LicenseDetection();
            extVersion.setLicense(detection.detectLicense(content));
        }
    }

    private Pair<byte[], String> readFromVsixPackage(String assetType, String[] alternateNames) {
        var assetPath = tryGetAssetPath(assetType);
        if(StringUtils.isNotEmpty(assetPath)) {
            var bytes = ArchiveUtil.readEntry(zipFile, assetPath);
            var lastSegmentIndex = assetPath.lastIndexOf('/');
            var lastSegment = assetPath.substring(lastSegmentIndex + 1);
            return Pair.of(bytes, lastSegment);
        } else {
            readInputStream();
            return readFromAlternateNames(alternateNames);
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

    private String tryGetAssetPath(String type) {
        loadVsixManifest();
        for(var asset : vsixManifest.path("Assets").path("Asset")) {
            if(Optional.ofNullable(asset.findValue("Type")).map(JsonNode::asText).orElse("").equals(type)) {
                return Optional.ofNullable(asset.findValue("Path"))
                        .map(JsonNode::asText)
                        .orElse(null);
            }
        }

        return null;
    }

    protected FileResource getIcon(ExtensionVersion extVersion) {
        var iconPath = tryGetAssetPath(ExtensionQueryResult.ExtensionFile.FILE_ICON);
        if(StringUtils.isEmpty(iconPath)) {
            loadPackageJson();
            var iconPathNode = packageJson.get("icon");
            iconPath = iconPathNode != null && iconPathNode.isTextual()
                    ? "extension/" + iconPathNode.asText().replace('\\', '/')
                    : null;
        }

        if (iconPath == null) {
            return null;
        }

        var bytes = ArchiveUtil.readEntry(zipFile, iconPath);
        if (bytes == null) {
            return null;
        }

        var icon = new FileResource();
        icon.setExtension(extVersion);
        var fileNameIndex = iconPath.lastIndexOf('/');
        var iconName = fileNameIndex >= 0
                ? iconPath.substring(fileNameIndex + 1)
                : iconPath;

        icon.setName(iconName);
        icon.setType(FileResource.ICON);
        icon.setContent(bytes);
        return icon;
    }

    public FileResource getVsixManifest(ExtensionVersion extVersion) {
        readInputStream();
        var vsixManifest = new FileResource();
        vsixManifest.setExtension(extVersion);
        vsixManifest.setName(VSIX_MANIFEST);
        vsixManifest.setType(FileResource.VSIXMANIFEST);
        vsixManifest.setContent(ArchiveUtil.readEntry(zipFile, VSIX_MANIFEST));
        return vsixManifest;
    }
}