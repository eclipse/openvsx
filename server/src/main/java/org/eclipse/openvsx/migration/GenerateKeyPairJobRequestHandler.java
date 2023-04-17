/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.openvsx.admin.RemoveFileJobRequest;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.SignatureKeyPair;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD_SIG;
import static org.eclipse.openvsx.entities.FileResource.STORAGE_DB;
import static org.eclipse.openvsx.entities.SignatureKeyPair.*;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "false", matchIfMissing = true)
public class GenerateKeyPairJobRequestHandler implements JobRequestHandler<HandlerJobRequest<?>> {

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    JobRequestScheduler scheduler;

    @Value("${ovsx.integrity.key-pair:}")
    String keyPairMode;

    @Override
    @Transactional
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        switch (keyPairMode) {
            case KEYPAIR_MODE_CREATE:
                createKeyPair();
                break;
            case KEYPAIR_MODE_RENEW:
                renewKeyPair();
                break;
            case KEYPAIR_MODE_DELETE:
                deleteKeyPairs();
                break;
        }
    }

    private void createKeyPair() {
        var activeKeyPair = repositories.findActiveKeyPair();
        Streamable<ExtensionVersion> extVersions;
        if(activeKeyPair == null) {
            generateKeyPair();
            extVersions = repositories.findVersions();
        } else {
            extVersions = repositories.findVersionsWithout(activeKeyPair);
        }

        extVersions.forEach(this::enqueueCreateSignatureJob);
    }

    private void renewKeyPair() {
        var activeKeyPair = repositories.findActiveKeyPair();
        generateKeyPair();
        repositories.findVersions().forEach(this::enqueueCreateSignatureJob);
        if(activeKeyPair != null) {
            activeKeyPair.setActive(false);
        }
    }

    private void deleteKeyPairs() {
        repositories.deleteAllKeyPairs();
        repositories.findFilesByType(DOWNLOAD_SIG).forEach(this::enqueueDeleteSignatureJob);
        repositories.deleteDownloadSigFiles();
    }

    private void generateKeyPair() {
        var generator = new Ed25519KeyPairGenerator();
        generator.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        var pair = generator.generateKeyPair();

        var keyPair = new SignatureKeyPair();
        keyPair.setPublicId(UUID.randomUUID().toString());
        keyPair.setPrivateKey(((Ed25519PrivateKeyParameters) pair.getPrivate()).getEncoded());
        keyPair.setPublicKeyText(getPublicKeyText(pair));
        keyPair.setCreated(LocalDateTime.now());
        keyPair.setActive(true);
        entityManager.persist(keyPair);
    }

    private String getPublicKeyText(AsymmetricCipherKeyPair pair) {
        PemObject pemObject;
        try {
            var publicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(pair.getPublic());
            pemObject = new PemObject("PUBLIC KEY", publicKeyInfo.getEncoded());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try (
                var output = new ByteArrayOutputStream();
                var writer = new PemWriter(new OutputStreamWriter(output))
        ) {
            writer.writeObject(pemObject);
            writer.flush();
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void enqueueCreateSignatureJob(ExtensionVersion extVersion) {
        var handler = ExtensionVersionSignatureJobRequestHandler.class;
        var jobRequest = new MigrationJobRequest<>(handler, extVersion.getId());
        scheduler.schedule(LocalDateTime.now().plusSeconds(30), jobRequest);
    }

    private void enqueueDeleteSignatureJob(FileResource resource) {
        if(!resource.getStorageType().equals(STORAGE_DB)) {
            scheduler.schedule(LocalDateTime.now().plusSeconds(30), new RemoveFileJobRequest(resource));
        }
    }
}
