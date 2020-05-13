/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.compare.LicenseCompareHelper;
import org.spdx.compare.SpdxCompareException;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.LicenseInfoFactory;

public class LicenseDetection {

    protected static final String[] DEFAULT_LICENSE_IDS = {
        "MIT", "Apache-2.0", "Apache-1.1", "EPL-2.0", "EPL-1.0", "ISC", "BSD-2-Clause", "BSD-3-Clause"
    };

    protected final Logger logger = LoggerFactory.getLogger(LicenseDetection.class);

    private final List<String> licenseIds;

    public LicenseDetection() {
        this(Collections.emptyList());
    }

    public LicenseDetection(List<String> additionalLicenseIds) {
        this.licenseIds = new ArrayList<>(additionalLicenseIds.size() + DEFAULT_LICENSE_IDS.length);
        this.licenseIds.addAll(additionalLicenseIds);
        for (var i = 0; i < DEFAULT_LICENSE_IDS.length; i++) {
            this.licenseIds.add(DEFAULT_LICENSE_IDS[i]);
        }
    }

    public String detectLicense(byte[] content) {
        try {
            return detectLicense(new String(content, "utf-8"));
        } catch (UnsupportedEncodingException exc) {
            throw new RuntimeException(exc);
        }
    }

    public String detectLicense(String content) {
        try {
            for (var licenseId : licenseIds) {
                var license = LicenseInfoFactory.getListedLicenseById(licenseId);
                var diff = LicenseCompareHelper.isTextStandardLicense(license, content);
                if (!diff.isDifferenceFound()) {
                    return license.getLicenseId();
                }
            }
        } catch (InvalidSPDXAnalysisException | SpdxCompareException exc) {
            logger.error("Failed to detect the license", exc);
        }
        return null;
    }

}