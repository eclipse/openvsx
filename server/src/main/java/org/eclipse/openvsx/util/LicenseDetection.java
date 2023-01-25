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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.compare.LicenseCompareHelper;
import org.spdx.compare.SpdxCompareException;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.SpdxListedLicense;

import com.google.common.io.CharStreams;

public class LicenseDetection {

    /** Default set of detected licenses, roughly ordered by frequency. */
    protected static final String[] DEFAULT_LICENSE_IDS = {
        "MIT", "Apache-2.0", "BSD-3-Clause", "GPL-3.0", "ISC", "EPL-2.0", "EPL-1.0", "LGPL-3.0",
        "MPL-2.0", "GPL-2.0", "AGPL-3.0", "Apache-1.1", "BSD-2-Clause", "CDDL-1.0", "LGPL-2.1"
    };

    protected final Logger logger = LoggerFactory.getLogger(LicenseDetection.class);

    private final List<String> licenseIds;
    private final Map<String, SpdxListedLicense> resolvedLicenses;

    public LicenseDetection() {
        this(Arrays.asList(DEFAULT_LICENSE_IDS));
    }

    public LicenseDetection(List<String> licenseIds) {
        this.licenseIds = licenseIds;
        this.resolvedLicenses = new ConcurrentHashMap<>(licenseIds.size());
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
            for (var licenseId : licenseIds) {
                var license = getLicense(licenseId);
                if (license != null) {
                    var diff = LicenseCompareHelper.isTextStandardLicense(license, content);
                    if (!diff.isDifferenceFound() || licenseId.equals("MIT") && matchesMIT(content)) {
                        return licenseId;
                    }
                }
            }
        } catch (SpdxCompareException exc) {
            logger.error("Failed to detect the license.", exc);
        }
        return null;
    }

    protected SpdxListedLicense getLicense(String licenseId) {
        return resolvedLicenses.computeIfAbsent(licenseId, id -> {
            SpdxListedLicense license = null;
            try {
                license = LicenseInfoFactory.getListedLicenseById(id);
                loadTemplate(license);
            } catch (InvalidSPDXAnalysisException exc) {
                logger.error("Failed to load listed licence: " + id, exc);
            }
            return license;
        });
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


    //-------------------- Special handling for the MIT license --------------------//

    private static String MIT_START = "------------------------------------------ START OF LICENSE -----------------------------------------";
    private static String MIT_END = "----------------------------------------------- END OF LICENSE -----------------------------------------";
    private static String MIT_RIGHTS = "All rights reserved.";
    private static String MIT_TITLE = "MIT License";
    private static String MIT_TEXT = "Permission is hereby granted, free of charge, to any person obtaining a copy of this software"
            + " and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without"
            + " limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,"
            + " and to permit persons to whom the Software is furnished to do so, subject to the following conditions:"
            + " The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software."
            + " THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE"
            + " WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS"
            + " OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR"
            + " OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.";

    private boolean matchesMIT(String content) {
        content = content.trim();
        var pos = 0;
        var contentLength = content.length();
        // Remove leading start sequence
        if (content.startsWith(MIT_START))
            pos += MIT_START.length();
        // Remove trailing end sequence
        if (content.endsWith(MIT_END))
            contentLength -= MIT_END.length();

        var isFirstLine = true;
        var copyrightMatched = false;
        var rightsMatched = false;
        var titleMatched = false;
        while (pos < contentLength) {
            var nextLineEnd = nextLineEnd(content, pos);
            var nextLine = content.substring(pos, nextLineEnd).trim();
            if (!nextLine.isEmpty()) {
                if (!copyrightMatched && nextLine.startsWith("Copyright ")) {
                    // Match "Copyright <YEAR> <COPYRIGHT HOLDER>"
                    copyrightMatched = true;
                } else if (!rightsMatched && nextLine.equals(MIT_RIGHTS)) {
                    // Match "All rights reserved"
                    rightsMatched = true;
                } else if (!titleMatched && nextLine.equals(MIT_TITLE)) {
                    // Match "MIT License"
                    titleMatched = true;
                } else if (nextLine.startsWith("Permission ")) {
                    // Match main license text
                    var licenseText = content.substring(pos, contentLength).trim();
                    var copyrightIndex = licenseText.indexOf("Copyright (c)");
                    if (copyrightIndex > 0) {
                        // The main text may contain another copyright statement
                        var copyrightEnd = nextLineEnd(licenseText, copyrightIndex);
                        licenseText = licenseText.substring(0, copyrightIndex) + licenseText.substring(copyrightEnd);
                    }
                    if (!licenseText.endsWith(".")) {
                        // Treat a missing period gracefully at the end.
                        licenseText += ".";
                    }
                    // Collapse all consecutive whitespace to a single space
                    licenseText = licenseText.replaceAll("\\s+", " ");
                    // Treat variations of quoting
                    licenseText = licenseText.replaceAll("\"\"|\\*", "\"");
                    // Ignore reference to last paragraph
                    licenseText = licenseText.replace("this permission notice (including the next paragraph)", "this permission notice");
                    return licenseText.equals(MIT_TEXT);
                } else if (!isFirstLine) {
                    // Ignore undetected text on the first line, but fail on any following line
                    return false;
                }
                isFirstLine = false;
            }
            pos = nextLineEnd;
            while (pos < contentLength && isLineBreak(content.charAt(pos))) {
                pos++;
            }
        }
        return false;
    }

    private int nextLineEnd(String content, int offset) {
        for (var i = offset; i < content.length(); i++) {
            var c = content.charAt(i);
            if (isLineBreak(c))
                return i;
        }
        return content.length();
    }

    private boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

}