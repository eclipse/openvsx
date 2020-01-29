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
            boolean leftIsNumber = !left.isEmpty() && Character.isDigit(left.charAt(0));
            boolean rightIsNumber = !right.isEmpty() && Character.isDigit(right.charAt(0));
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