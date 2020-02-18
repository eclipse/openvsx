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
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.springframework.stereotype.Component;

@Component
public class ExtensionValidator {

    private final static List<String> CATEGORIES_VALUES = Arrays.asList(new String[] {
        "Programming Languages", "Snippets", "Linters", "Themes", "Debuggers", "Formatters",
        "Keymaps", "SCM Providers", "Other", "Extension Packs", "Language Packs"
    });

    private final static List<String> MARKDOWN_VALUES = Arrays.asList(new String[] {
        "github", "standard"
    });

    private final static List<String> GALLERY_THEME_VALUES = Arrays.asList(new String[] {
        "dark", "light"
    });

    private final static List<String> QNA_VALUES = Arrays.asList(new String[] {
        "marketplace", "false"
    });

    private final static int DEFAULT_STRING_SIZE = 255;
    private final static int DESCRIPTION_SIZE = 2048;

    private final Pattern namePattern = Pattern.compile("[\\w\\-\\+\\$~]+");

    public Optional<Issue> validatePublisherName(String publisher) {
        if (Strings.isNullOrEmpty(publisher)) {
            return Optional.of(new Issue("Publisher must not be empty."));
        }
        if (!namePattern.matcher(publisher).matches()) {
            return Optional.of(new Issue("Invalid publisher name: " + publisher));
        }
        if (publisher.length() > DEFAULT_STRING_SIZE) {
            return Optional.of(new Issue("The publisher name exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
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
        if (extVersion.getDisplayName() != null && extVersion.getDisplayName().trim().isEmpty()) {
            extVersion.setDisplayName(null);
        }
        if (hasInvalidCharacter(extVersion.getDisplayName())) {
            issues.add(new Issue("Invalid character found in field 'displayName'."));
        }
        if (extVersion.getDisplayName().length() > DEFAULT_STRING_SIZE) {
            issues.add(new Issue("The field 'displayName' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        if (hasInvalidCharacter(extVersion.getDescription())) {
            issues.add(new Issue("Invalid character found in field 'description'."));
        }
        if (extVersion.getDescription().length() > DESCRIPTION_SIZE) {
            issues.add(new Issue("The field 'description' exceeds the current limit of " + DESCRIPTION_SIZE + " characters."));
        }
        if (extVersion.getCategories().stream().anyMatch(s -> !CATEGORIES_VALUES.contains(s))) {
            issues.add(new Issue("Invalid category: "
                    + extVersion.getCategories().stream().filter(s -> !CATEGORIES_VALUES.contains(s)).findFirst().get()
                    + ". Choose from " + CATEGORIES_VALUES.toString()));
        }
        if (extVersion.getTags().stream().anyMatch(s -> hasInvalidCharacter(s))) {
            issues.add(new Issue("Invalid character found in field 'keywords'."));
        }
        if (extVersion.getTags().stream().anyMatch(s -> s.length() > DEFAULT_STRING_SIZE)) {
            issues.add(new Issue("An entry of the field 'keywords' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        if (hasInvalidCharacter(extVersion.getLicense())) {
            issues.add(new Issue("Invalid character found in field 'license'."));
        }
        if (extVersion.getLicense().length() > DEFAULT_STRING_SIZE) {
            issues.add(new Issue("The field 'license' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        if (isInvalidURL(extVersion.getHomepage())) {
            issues.add(new Issue("Invalid URL: " + extVersion.getHomepage()));
        }
        if (extVersion.getHomepage().length() > DEFAULT_STRING_SIZE) {
            issues.add(new Issue("The field 'homepage' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        if (isInvalidURL(extVersion.getRepository())) {
            issues.add(new Issue("Invalid URL: " + extVersion.getRepository()));
        }
        if (extVersion.getRepository().length() > DEFAULT_STRING_SIZE) {
            issues.add(new Issue("The field 'repository' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        if (isInvalidURL(extVersion.getBugs())) {
            issues.add(new Issue("Invalid URL: " + extVersion.getBugs()));
        }
        if (extVersion.getBugs().length() > DEFAULT_STRING_SIZE) {
            issues.add(new Issue("The field 'bugs' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        if (!Strings.isNullOrEmpty(extVersion.getMarkdown())
                && !MARKDOWN_VALUES.contains(extVersion.getMarkdown())) {
            issues.add(new Issue("Invalid 'markdown' value. Choose one of "
                    + MARKDOWN_VALUES.toString()));
        }
        if (hasInvalidCharacter(extVersion.getGalleryColor())) {
            issues.add(new Issue("Invalid character found in field 'galleryBanner.color'."));
        }
        if (extVersion.getGalleryColor().length() > DEFAULT_STRING_SIZE) {
            issues.add(new Issue("The field 'galleryBanner.color' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        if (!Strings.isNullOrEmpty(extVersion.getGalleryTheme())
                && !GALLERY_THEME_VALUES.contains(extVersion.getGalleryTheme())) {
            issues.add(new Issue("Invalid 'galleryBanner.theme' value. Choose one of "
                    + GALLERY_THEME_VALUES.toString()));
        }
        if (!Strings.isNullOrEmpty(extVersion.getQna())
                && !QNA_VALUES.contains(extVersion.getQna())
                && isInvalidURL(extVersion.getQna())) {
            issues.add(new Issue("Invalid 'qna' value. Choose one of "
                    + QNA_VALUES.toString() + " or a URL."));
        }
        if (extVersion.getQna().length() > DEFAULT_STRING_SIZE) {
            issues.add(new Issue("The field 'qna' exceeds the current limit of " + DEFAULT_STRING_SIZE + " characters."));
        }
        return issues;
    }

    private boolean hasInvalidCharacter(String s) {
        if (s == null) {
            return false;
        }
        for (var i = 0; i < s.length(); i++) {
            var type = Character.getType(s.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT
                    || type == Character.UNASSIGNED || type == Character.PRIVATE_USE
                    || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR) {
                return true;
            }
        }
        return false;
    }

    private boolean isInvalidURL(String s) {
        if (Strings.isNullOrEmpty(s)) {
            return false;
        }
        try {
            new URL(s);
            return false;
        } catch (MalformedURLException exc) {
            return true;
        }
    }


    public static class Issue {

        private final String message;

        private Issue(String message) {
            this.message = message;
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