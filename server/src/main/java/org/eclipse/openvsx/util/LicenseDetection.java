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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.CharStreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.compare.LicenseCompareHelper;
import org.spdx.compare.SpdxCompareException;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.SpdxListedLicense;

public class LicenseDetection {

    /** Default set of detected licenses, roughly ordered by frequency. */
    protected static final String[] DEFAULT_LICENSE_IDS = {
        "MIT", "Apache-2.0", "BSD-3-Clause", "GPL-3.0", "ISC", "EPL-2.0", "EPL-1.0", "LGPL-3.0",
        "MPL-2.0", "GPL-2.0", "AGPL-3.0", "Apache-1.1", "BSD-2-Clause", "CDDL-1.0", "LGPL-2.1"
    };

    protected final Logger logger = LoggerFactory.getLogger(LicenseDetection.class);

    private final List<String> licenseIds;
    private List<SpdxListedLicense> resolvedLicenses;

    public LicenseDetection() {
        this(Arrays.asList(DEFAULT_LICENSE_IDS));
    }

    public LicenseDetection(List<String> licenseIds) {
        this.licenseIds = licenseIds;
    }

    public String detectLicense(byte[] content) {
        try {
            return detectLicense(new String(content, "UTF-8"));
        } catch (UnsupportedEncodingException exc) {
            throw new RuntimeException(exc);
        }
    }

    public String detectLicense(String content) {
        try {
            for (var license : getLicenses()) {
                var diff = LicenseCompareHelper.isTextStandardLicense(license, content);
                if (!diff.isDifferenceFound()) {
                    return license.getLicenseId();
                }
            }
        } catch (SpdxCompareException exc) {
            logger.error("Failed to detect the license.", exc);
        }
        return null;
    }

    protected List<SpdxListedLicense> getLicenses() {
        if (resolvedLicenses == null) {
            resolvedLicenses = CollectionUtil.map(licenseIds, id -> {
                try {
                    var license = LicenseInfoFactory.getListedLicenseById(id);
                    loadTemplate(license);
                    return license;
                } catch (InvalidSPDXAnalysisException exc) {
                    logger.error("Failed to load listed licence: " + id, exc);
                    return null;
                }
            });
        }
        return resolvedLicenses;
    }

    private void loadTemplate(SpdxListedLicense license) {
        var resource = getClass().getResource("/spdx-templates/" + license.getLicenseId() + ".txt");
        if (resource != null) {
            try {
                var data = CharStreams.toString(new InputStreamReader(resource.openStream()));
                license.setStandardLicenseTemplate(data);
            } catch (IOException exc) {
                logger.error("Failed to load SPDX template.", exc);
            } 
        }
    }

}