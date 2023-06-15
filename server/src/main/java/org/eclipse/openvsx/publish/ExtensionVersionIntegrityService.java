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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

import static org.eclipse.openvsx.entities.SignatureKeyPair.KEYPAIR_MODE_CREATE;
import static org.eclipse.openvsx.entities.SignatureKeyPair.KEYPAIR_MODE_RENEW;

@Component
public class ExtensionVersionIntegrityService {

    protected final Logger logger = LoggerFactory.getLogger(ExtensionVersionIntegrityService.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    CacheService cache;

    @Value("${ovsx.integrity.key-pair:}")
    String keyPairMode;

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

        var privateKeyParameters = new Ed25519PrivateKeyParameters(keyPair.getPrivateKey(), 0);
        try {
            var signer = new Ed25519Signer();
            signer.init(true,  privateKeyParameters);
            var fileBytes = Files.readAllBytes(extensionFile.getPath());
            signer.update(fileBytes, 0, fileBytes.length);
            resource.setContent(signer.generateSignature());
        } catch (IOException e) {
            throw new ErrorResultException("Failed to sign extension file", e);
        }

        return resource;
    }
}
