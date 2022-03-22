/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import java.util.ArrayList;
import java.util.List;

public class SemanticVersion implements Comparable<SemanticVersion> {

    // source: https://semver.org/, search for: https://regex101.com/r/vkijKf/1/
    // has been modified to only use non-capturing groups (?:.*), so that it can be used as a URI template regex
    public static final String VERSION_PATH_PARAM_REGEX = "(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)(?:-(?:(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?:[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?";

    private final String original;
    private final List<String> parts = new ArrayList<>();

    public SemanticVersion(String s) {
        this.original = s;
        int suffixIndex = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c) && c != '.') {
                suffixIndex = i;
                break;
            }
        }
        var main = s.substring(0, suffixIndex);
        var split = main.split("\\.");
        for (int i = 0; i < split.length; i++) {
            parts.add(split[i]);
        }
        if (suffixIndex < s.length()) {
            parts.add(s.substring(suffixIndex));
        }
    }

    public int getMajor() {
        return getNumberPart(0);
    }

    public int getMinor() {
        return getNumberPart(1);
    }

    private int getNumberPart(int index) {
        if(index >= parts.size()) {
            return 0;
        }

        var part = parts.get(index);
        return isNumber(part) ? Integer.parseInt(part) : 0;
    }

    private boolean isNumber(String part) {
        var isNumber = false;
        for(var i = 0; i < part.length(); i++) {
            isNumber = Character.isDigit(part.charAt(i));
            if(!isNumber) {
                break;
            }
        }

        return isNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SemanticVersion))
            return false;
        var other = (SemanticVersion) obj;
        return this.parts.equals(other.parts);
    }

    @Override
    public int hashCode() {
        return parts.hashCode();
    }

	@Override
	public int compareTo(SemanticVersion other) {
        int minSize = Math.min(this.parts.size(), other.parts.size());
        for (int i = 0; i < minSize; i++) {
            String left = this.parts.get(i);
            String right = other.parts.get(i);
            boolean leftIsNumber = isNumber(left);
            boolean rightIsNumber = isNumber(right);
            if (!leftIsNumber && !rightIsNumber)
                // Regard versions as equal in terms of sorting if they differ only in their suffix
                return 0;
            int compare;
            if (leftIsNumber && rightIsNumber)
                compare = Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
            else
                compare = left.compareTo(right);
            if (compare != 0)
                return compare;
        }
		return -Integer.compare(this.parts.size(), other.parts.size());
	}

    @Override
    public String toString() {
        return original;
    }

}