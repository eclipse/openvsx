/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.adapter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExtensionQueryParam {

	public static final int FLAG_INCLUDE_VERSIONS = 0x1;
	public static final int FLAG_INCLUDE_FILES = 0x2;
	public static final int FLAG_INCLUDE_CATEGORY_AND_TAGS = 0x4;
	public static final int FLAG_INCLUDE_SHARED_ACCOUNTS = 0x8;
	public static final int FLAG_INCLUDE_VERSION_PROPERTIES = 0x10;
	public static final int FLAG_EXCLUDE_NON_VALIDATED = 0x20;
	public static final int FLAG_INCLUDE_INSTALLATION_TARGETS = 0x40;
	public static final int FLAG_INCLUDE_ASSET_URI = 0x80;
	public static final int FLAG_INCLUDE_STATISTICS = 0x100;
	public static final int FLAG_INCLUDE_LATEST_VERSION_ONLY = 0x200;
	public static final int FLAG_UNPUBLISHED = 0x1000;

    public List<Filter> filters;
    public int flags;

    public static class Filter {
        public List<Criterion> criteria;
        public int pageNumber;
        public int pageSize;
        public int sortBy;
        public int sortOrder;

        public String findCriterion(int type) {
            if (criteria == null || criteria.isEmpty())
                return null;
            return criteria.stream()
                    .filter(c -> c.filterType == type)
                    .findFirst()
                    .map(c -> c.value)
                    .orElse(null);
        }

        public List<String> findCriteria(int type) {
            if (criteria == null || criteria.isEmpty())
                return Collections.emptyList();
            return criteria.stream()
                    .filter(c -> c.filterType == type && c.value != null)
                    .map(c -> c.value)
                    .collect(Collectors.toList());
        }
    }

    public static class Criterion {
        public static final int FILTER_TAG = 1;
        public static final int FILTER_EXTENSION_ID = 4;
        public static final int FILTER_CATEGORY = 5;
        public static final int FILTER_EXTENSION_NAME = 7;
        public static final int FILTER_TARGET = 8;
        public static final int FILTER_FEATURED = 9;
        public static final int FILTER_SEARCH_TEXT = 10;
        public static final int FILTER_EXCLUDE_WITH_FLAGS = 12;

        public int filterType;
        public String value;
    }

}