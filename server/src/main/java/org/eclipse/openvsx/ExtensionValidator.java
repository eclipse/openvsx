/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.util.TargetPlatform;
import org.springframework.stereotype.Component;

@Component
public class ExtensionValidator {

    private final static List<String> MARKDOWN_VALUES = List.of("github", "standard");

    private final static List<String> GALLERY_THEME_VALUES = List.of("dark", "light");

    private final static List<String> QNA_VALUES = List.of("marketplace", "false");

    private final static List<Character> VERSION_CHARS = List.of('$', '+', '-', ',', '.', ':', ';', '_');

    private final static int DEFAULT_STRING_SIZE = 255;
    private final static int DESCRIPTION_SIZE = 2048;
    private final static int GALLERY_COLOR_SIZE = 16;

    private final Pattern namePattern = Pattern.compile("[\\w\\-\\+\\$~]+");

    public Optional<Issue> validateNamespace(String namespace) {
        if (Strings.isNullOrEmpty(namespace) || namespace.equals("-")) {
            return Optional.of(new Issue("Namespace name must not be empty."));
        }
        if (!namePattern.matcher(namespace).matches()) {
            return Optional.of(new Issue("Invalid namespace name: " + namespace));
        }
        if (namespace.length() > DEFAULT_STRING_SIZE) {
            return Optional.of(new Issue("The namespace name exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        return Optional.empty();
    }

    public Optional<Issue> validateExtensionName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return Optional.of(new Issue("Name must not be empty."));
        }
        if (!namePattern.matcher(name).matches()) {
            return Optional.of(new Issue("Invalid extension name: " + name));
        }
        if (name.length() > DEFAULT_STRING_SIZE) {
            return Optional.of(new Issue("The extension name exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        return Optional.empty();
    }

    public List<Issue> validateMetadata(ExtensionVersion extVersion) {
        var issues = new ArrayList<Issue>();
        checkVersion(extVersion.getVersion(), issues);
        checkTargetPlatform(extVersion.getTargetPlatform(), issues);
        checkCharacters(extVersion.getDisplayName(), "displayName", issues);
        checkFieldSize(extVersion.getDisplayName(), DEFAULT_STRING_SIZE, "displayName", issues);
        checkCharacters(extVersion.getDescription(), "description", issues);
        checkFieldSize(extVersion.getDescription(), DESCRIPTION_SIZE, "description", issues);
        checkCharacters(extVersion.getCategories(), "categories", issues);
        checkFieldSize(extVersion.getCategories(), DEFAULT_STRING_SIZE, "categories", issues);
        checkCharacters(extVersion.getTags(), "keywords", issues);
        checkFieldSize(extVersion.getTags(), DEFAULT_STRING_SIZE, "keywords", issues);
        checkCharacters(extVersion.getLicense(), "license", issues);
        checkFieldSize(extVersion.getLicense(), DEFAULT_STRING_SIZE, "license", issues);
        checkURL(extVersion.getHomepage(), "homepage", issues);
        checkFieldSize(extVersion.getHomepage(), DEFAULT_STRING_SIZE, "homepage", issues);
        checkURL(extVersion.getRepository(), "repository", issues);
        checkFieldSize(extVersion.getRepository(), DEFAULT_STRING_SIZE, "repository", issues);
        checkURL(extVersion.getBugs(), "bugs", issues);
        checkFieldSize(extVersion.getBugs(), DEFAULT_STRING_SIZE, "bugs", issues);
        checkInvalid(extVersion.getMarkdown(), s -> !MARKDOWN_VALUES.contains(s), "markdown", issues,
                MARKDOWN_VALUES.toString());
        checkCharacters(extVersion.getGalleryColor(), "galleryBanner.color", issues);
        checkFieldSize(extVersion.getGalleryColor(), GALLERY_COLOR_SIZE, "galleryBanner.color", issues);
        checkInvalid(extVersion.getGalleryTheme(), s -> !GALLERY_THEME_VALUES.contains(s), "galleryBanner.theme", issues,
                GALLERY_THEME_VALUES.toString());
        checkInvalid(extVersion.getQna(), s -> !QNA_VALUES.contains(s) && !isURL(s), "qna", issues,
                QNA_VALUES.toString() + " or a URL");
        checkFieldSize(extVersion.getQna(), DEFAULT_STRING_SIZE, "qna", issues);
        return issues;
    }

    private void checkVersion(String version, List<Issue> issues) {
        if (Strings.isNullOrEmpty(version)) {
            issues.add(new Issue("Version must not be empty."));
            return;
        }
        if (version.equals("latest") || version.equals("pre-release") || version.equals("reviews")) {
            issues.add(new Issue("The version string '" + version + "' is reserved."));
        }
        for (var i = 0; i < version.length(); i++) {
            var c = version.charAt(i);
            if (!(Character.isLetterOrDigit(c) || VERSION_CHARS.contains(c))) {
                issues.add(new Issue("Invalid character '" + c + "' found in version (index " + i + ")."));
                return;
            }
        }
    }

    private void checkTargetPlatform(String targetPlatform, List<Issue> issues) {
        if(!TargetPlatform.isValid(targetPlatform)) {
            issues.add(new Issue("Unsupported target platform '" + targetPlatform + "'"));
        }
    }

    private void checkCharacters(String value, String field, List<Issue> issues) {
        if (value == null) {
            return;
        }
        for (var i = 0; i < value.length(); i++) {
            var type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT
                    || type == Character.UNASSIGNED || type == Character.PRIVATE_USE
                    || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR) {
                issues.add(new Issue("Invalid character found in field '" + field + "': " + value + " (index " + i + ")"));
                return;
            }
        }
    }

    private void checkCharacters(List<String> values, String field, List<Issue> issues) {
        if (values == null) {
            return;
        }
        for (var value : values) {
            checkCharacters(value, field, issues);
        }
    }

    private void checkFieldSize(String value, int limit, String field, List<Issue> issues) {
        if (value != null && value.length() > limit) {
            issues.add(new Issue("The field '" + field + "' exceeds the current limit of " + limit + " characters."));
        }
    }

    private void checkFieldSize(List<String> values, int limit, String field, List<Issue> issues) {
        if (values == null) {
            return;
        }
        for (var value : values) {
            checkFieldSize(value, limit, field, issues);
        }
    }

    private void checkInvalid(String value, Predicate<String> isInvalid, String field, List<Issue> issues, String allowedValues) {
        if (Strings.isNullOrEmpty(value)) {
            return;
        }
        if (isInvalid.test(value)) {
            issues.add(new Issue("Invalid value in field '" + field + "': " + value
                    + ". Allowed values: " + allowedValues));
        }
    }

    private void checkURL(String value, String field, List<Issue> issues) {
        if (Strings.isNullOrEmpty(value)) {
            return;
        }
        if (!isURL(value)) {
            issues.add(new Issue("Invalid URL in field '" + field + "': " + value));
        }
    }

    private boolean isURL(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }
        try {
            if (value.startsWith("git+") && value.length() > 4)
                new URL(value.substring(4));
            else
                new URL(value);
            return true;
        } catch (MalformedURLException exc) {
            return false;
        }
    }


    public static class Issue {

        private final String message;

        Issue(String message) {
            this.message = message;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Issue))
                return false;
            var other = (Issue) obj;
            if (!Objects.equals(this.message, other.message))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return message.hashCode();
        }

        @Override
        public String toString() {
            return message;
        }

        public String getMessage() {
            return message;
        }
    }

}