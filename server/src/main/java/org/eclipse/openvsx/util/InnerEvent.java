/********************************************************************************
 * Copyright (c) 2024 Ericsson and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import java.util.logging.LogRecord;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.TraceCompassLogRecord;

class InnerEvent {

    public static @Nullable InnerEvent create(LogRecord lRecord) {
      if (lRecord instanceof TraceCompassLogUtils.TraceCompassLogRecord) {
        TraceCompassLogUtils.TraceCompassLogRecord rec = (TraceCompassLogRecord) lRecord;
        double ts = (double) rec.getParameters()[0];
        String phase = (String) rec.getParameters()[1];
        String pid = (String) rec.getParameters()[2];
        String tid = (String) rec.getParameters()[2];
        return new InnerEvent(rec, ts, phase, pid, tid);
      }
      return null;
    }

  private final LogRecord message;
  private final double ts;
  private final String tid;
  private final String pid;
  private final String phase;

  public InnerEvent(LogRecord message, double ts, String phase, String pid, String tid) {
    this.message = message;
    this.ts = ts;
    this.phase = phase;
    this.tid = tid;
    this.pid = pid;
  }

  public String getMessage() {
    return message.getMessage();
  }

  public double getTs() {
    return ts;
  }

  public String getTid() {
    return tid;
  }

  public String getPid() {
    return pid;
  }

  public String getPhase() {
    return phase;
  }
}
