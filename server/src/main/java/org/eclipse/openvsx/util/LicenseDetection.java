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

import org.spdx.compare.LicenseCompareHelper;
import org.spdx.compare.SpdxCompareException;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.LicenseInfoFactory;

public final class LicenseDetection {

    public static String detectLicense(String file) {
        try {
            String[] licenseIds = {"Apache-2.0", "EPL-2.0", "MIT", "ISC", "BSD-2-Clause", "BSD-3-Clause"};
            for (int i = 0; i < licenseIds.length; i++) {
                var license = LicenseInfoFactory.getListedLicenseById(licenseIds[i]);
                var diff = LicenseCompareHelper.isTextStandardLicense(license, file);
                if (!diff.isDifferenceFound()) {
                    return license.getLicenseId();
                }
            }
            return null;
        } catch (InvalidSPDXAnalysisException | SpdxCompareException e) {
            throw new RuntimeException(e);
        }
    }
}