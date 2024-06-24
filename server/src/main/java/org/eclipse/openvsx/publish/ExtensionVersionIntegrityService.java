/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.SignatureKeyPair;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.eclipse.openvsx.entities.SignatureKeyPair.KEYPAIR_MODE_CREATE;
import static org.eclipse.openvsx.entities.SignatureKeyPair.KEYPAIR_MODE_RENEW;

@Component
public class ExtensionVersionIntegrityService {

    protected final Logger logger = LoggerFactory.getLogger(ExtensionVersionIntegrityService.class);

    private final EntityManager entityManager;
    private final CacheService cache;

    @Value("${ovsx.integrity.key-pair:}")
    String keyPairMode;

    public ExtensionVersionIntegrityService(EntityManager entityManager, CacheService cache) {
        this.entityManager = entityManager;
        this.cache = cache;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        if(!isEnabled()) {
            return;
        }

        cache.evictLatestExtensionVersions();
        cache.evictExtensionJsons();
        cache.evictNamespaceDetails();
    }

    public boolean isEnabled() {
        return keyPairMode.equals(KEYPAIR_MODE_CREATE) || keyPairMode.equals(KEYPAIR_MODE_RENEW);
    }

    public boolean verifyExtensionVersion(TempFile extensionFile, TempFile signatureFile, TempFile publicKeyFile) {
        AsymmetricKeyParameter publicKeyParameters;
        try (var inReader = new InputStreamReader(Files.newInputStream(publicKeyFile.getPath()))) {
            var pemParser = new PEMParser(inReader);
            var publicKeyInfo = (SubjectPublicKeyInfo) pemParser.readObject();
            publicKeyParameters = PublicKeyFactory.createKey(publicKeyInfo);
        } catch (IOException e) {
            throw new ErrorResultException("Failed to read private key file", e);
        }

        boolean verified;
        try {
            var signer = new Ed25519Signer();
            signer.init(false,  publicKeyParameters);
            var fileBytes = Files.readAllBytes(extensionFile.getPath());
            signer.update(fileBytes, 0, fileBytes.length);
            verified = signer.verifySignature(Files.readAllBytes(signatureFile.getPath()));
        } catch (IOException e) {
            throw new ErrorResultException("Failed to verify extension file", e);
        }

        return verified;
    }

    @Transactional
    public void setSignatureKeyPair(ExtensionVersion extVersion, SignatureKeyPair keyPair) {
        extVersion = entityManager.merge(extVersion);
        extVersion.setSignatureKeyPair(keyPair);
    }

    public FileResource generateSignature(FileResource download, TempFile extensionFile, SignatureKeyPair keyPair) {
        var resource = new FileResource();
        resource.setExtension(download.getExtension());
        resource.setName(NamingUtil.toFileFormat(download.getExtension(), ".sigzip"));
        resource.setType(FileResource.DOWNLOAD_SIG);
        try (var out = new ByteArrayOutputStream()) {
            try (var zip = new ZipOutputStream(out)) {
                var sigEntry = new ZipEntry(".signature.sig");
                zip.putNextEntry(sigEntry);
                zip.write(generateSignature(extensionFile, keyPair));
                zip.closeEntry();

                var manifestEntry = new ZipEntry(".signature.manifest");
                zip.putNextEntry(manifestEntry);
                zip.write(generateSignatureManifest(extensionFile));
                zip.closeEntry();

                // Add dummy file to the archive because VS Code checks if it exists
                var dummyEntry = new ZipEntry(".signature.p7s");
                zip.putNextEntry(dummyEntry);
                zip.write(new byte[0]);
                zip.closeEntry();
            }

            resource.setContent(out.toByteArray());
        } catch (IOException e) {
            throw new ErrorResultException("Failed to sign extension file", e);
        }

        return resource;
    }

    private byte[] generateSignature(TempFile extensionFile, SignatureKeyPair keyPair) throws IOException {
        var privateKeyParameters = new Ed25519PrivateKeyParameters(keyPair.getPrivateKey(), 0);
        var signer = new Ed25519Signer();
        signer.init(true, privateKeyParameters);
        try (var in = Files.newInputStream(extensionFile.getPath())) {
            int len;
            var buffer = new byte[1024];
            while ((len = in.read(buffer)) > 0) {
                signer.update(buffer, 0, len);
            }
        }

        return signer.generateSignature();
    }

    private byte[] generateSignatureManifest(TempFile extensionFile) throws IOException {
        var base64 = new Base64();
        var mapper = new ObjectMapper();
        var manifestEntries = mapper.createObjectNode();
        try(var zip = new ZipFile(extensionFile.getPath().toFile())) {
            zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .forEach(entry -> {
                        try (var entryStream = zip.getInputStream(entry)) {
                            var manifestEntry = generateManifestEntry(entryStream, entry.getSize(), mapper, base64);
                            manifestEntries.set(new String(base64.encode(entry.getName().getBytes(StandardCharsets.UTF_8))), manifestEntry);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

        var manifest = mapper.createObjectNode();
        try (var extensionStream = Files.newInputStream(extensionFile.getPath())) {
            manifest.set("package", generateManifestEntry(extensionStream, Files.size(extensionFile.getPath()), mapper, base64));
        }
        manifest.set("entries", manifestEntries);
        return mapper.writeValueAsBytes(manifest);
    }

    private JsonNode generateManifestEntry(InputStream stream, long size, ObjectMapper mapper, Base64 base64) throws IOException {
        var manifestEntry = mapper.createObjectNode();
        manifestEntry.put("size", size);

        var manifestEntryDigests = mapper.createObjectNode();
        var sha256 = new String(base64.encode(DigestUtils.sha256(stream)));
        manifestEntryDigests.put("sha256", sha256);
        manifestEntry.set("digests", manifestEntryDigests);
        return manifestEntry;
    }
}
