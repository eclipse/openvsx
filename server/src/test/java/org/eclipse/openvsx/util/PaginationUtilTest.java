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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.openvsx.json.ResultJson;
import org.junit.jupiter.api.Test;

class PaginationUtilTest {

	@Test
	void shouldReturnErrorForNegativeSize() {
		var results = PaginationUtil.validatePaginationParameters(-1, 0, ResultJson::error);
		assertThat(results).isNotNull();
		assertThat(results.getBody().getError()).contains("'size' must not be negative.");
	}

	@Test
	void shouldReturnErrorForNegativeOffset() {
		var results = PaginationUtil.validatePaginationParameters(0, -1, ResultJson::error);
		assertThat(results).isNotNull();
		assertThat(results.getBody().getError()).contains("'offset' must not be negative.");
	}

	@Test
	void shouldReturnErrorForTooLargePageSize() {
		var results = PaginationUtil.validatePaginationParameters(Integer.MAX_VALUE, 0, ResultJson::error);
		assertThat(results).isNotNull();
		assertThat(results.getBody().getError()).contains("'size' must not be over max size");
	}

	@Test
	void shouldReturnNullForNormalParameters() {
		var results = PaginationUtil.validatePaginationParameters(1, 1, ResultJson::error);
		assertThat(results).isNull();
	}
}
