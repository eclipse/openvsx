/** ******************************************************************************
 * Copyright (c) 2026 Eclipse Foundation AISBL
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import java.util.function.Function;

import org.eclipse.openvsx.json.ResultJson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class to centralize pagination parameter validation across endpoints.
 */
public class PaginationUtil {

	public static final int MAX_PAGINATION_SIZE = 100;

	/**
	 * Checks parameters for pagination to ensure that they are valid values. This
	 * checks for negative values as well as size that exceeds a shared maximum
	 * value.
	 * 
	 * @param <T> type of internal ResponseEntity body.
	 * @param size the pagination size to validate
	 * @param offset pagination offset to validate
	 * @param errorGenerator function used to create the error body for the current request if there is a validation issue.
	 * @return a populated response entity if there is a validation error, otherwise null.
	 */
	public static <T extends ResultJson> ResponseEntity<T> validatePaginationParameters(int size, int offset,
			Function<String, T> errorGenerator) {
		if (size < 0) {
			var json = errorGenerator.apply(negativeSizeMessage());
			return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
		} else if (size > MAX_PAGINATION_SIZE) {
			var json = errorGenerator.apply("Parameter 'size' must not be over max size of: " + MAX_PAGINATION_SIZE);
			return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
		}
		if (offset < 0) {
			var json = errorGenerator.apply(negativeOffsetMessage());
			return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
		}

		return null;
	}

	private static String negativeSizeMessage() {
		return negativeParameterMessage("size");
	}

	private static String negativeOffsetMessage() {
		return negativeParameterMessage("offset");
	}

	private static String negativeParameterMessage(String field) {
		return "The parameter '" + field + "' must not be negative.";
	}
}
