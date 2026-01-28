/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.scanning;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Scanner.Invocation} nested sealed interface.
 */
class ScannerInvocationTest {

    @Test
    void completed_storesResult() {
        var result = Scanner.Result.clean();
        var invocation = new Scanner.Invocation.Completed(result);

        assertEquals(result, invocation.result());
        assertInstanceOf(Scanner.Invocation.Completed.class, invocation);
    }

    @Test
    void submitted_storesSubmission() {
        var submission = new Scanner.Submission("job-123");
        var invocation = new Scanner.Invocation.Submitted(submission);

        assertEquals(submission, invocation.submission());
        assertInstanceOf(Scanner.Invocation.Submitted.class, invocation);
    }

    @Test
    void patternMatching_worksForCompleted() {
        Scanner.Invocation invocation = new Scanner.Invocation.Completed(
                Scanner.Result.clean()
        );

        String type = switch (invocation) {
            case Scanner.Invocation.Completed c -> "completed";
            case Scanner.Invocation.Submitted s -> "submitted";
        };

        assertEquals("completed", type);
    }

    @Test
    void patternMatching_worksForSubmitted() {
        Scanner.Invocation invocation = new Scanner.Invocation.Submitted(
                new Scanner.Submission("job-123")
        );

        String type = switch (invocation) {
            case Scanner.Invocation.Completed c -> "completed";
            case Scanner.Invocation.Submitted s -> "submitted";
        };

        assertEquals("submitted", type);
    }

    @Test
    void completed_canAccessResultDetails() {
        var threat = new Scanner.Threat("virus", "desc", "HIGH");
        var result = Scanner.Result.withThreats(List.of(threat));
        var invocation = new Scanner.Invocation.Completed(result);

        // Access result through pattern matching
        if (invocation instanceof Scanner.Invocation.Completed completed) {
            assertFalse(completed.result().isClean());
            assertEquals(1, completed.result().getThreats().size());
        } else {
            fail("Should be Completed");
        }
    }

    @Test
    void submitted_canAccessSubmissionDetails() {
        var submission = new Scanner.Submission("external-job-456");
        var invocation = new Scanner.Invocation.Submitted(submission);

        // Access submission through pattern matching
        if (invocation instanceof Scanner.Invocation.Submitted submitted) {
            assertEquals("external-job-456", submitted.submission().externalJobId());
        } else {
            fail("Should be Submitted");
        }
    }
}
