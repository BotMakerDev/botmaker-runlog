// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.runlog;

import java.time.Instant;

/**
 * One comment of an issue, as GitHub gives it: its text and when it was made. A comment {@link IssueText#comment}
 * wrote is one more occurrence of the issue's problem, dated by GitHub; any other is skipped.
 *
 * @param body    its text
 * @param created when GitHub says it was made
 */
public record IssueComment(String body, Instant created) {
}
