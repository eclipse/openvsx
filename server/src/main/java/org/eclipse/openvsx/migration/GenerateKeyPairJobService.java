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
import org.eclipse.openvsx.entities.SignatureKeyPair;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class GenerateKeyPairJobService {
    
    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Transactional
    public void renewKeyPair() {
        var activeKeyPair = repositories.findActiveKeyPair();
        if(activeKeyPair != null) {
            activeKeyPair.setActive(false);
        }

        generateKeyPair();
    }

    @Transactional
    public void generateKeyPair() {
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

    @Transactional
    public void deleteSignaturesAndKeyPairs() {
        repositories.deleteAllKeyPairs();
        repositories.deleteDownloadSigFiles();
    }
}