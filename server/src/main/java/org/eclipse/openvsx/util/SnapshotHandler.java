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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.log.TraceCompassLogUtils.TraceCompassLogRecord;


/**
 * Snapshot handler, will write to disk in a deferred way. Parameters to
 * configure:
 * <ul>
 * <li>maxEvents (maximum amount of events to write)</li>
 * <li>timeout (maximum amount of time in seconds before this snapshot is
 * triggered)</li>
 * <li>filePath (pattern to write file names e.g. "request-" will yield
 * "request-23.json", "request-24.json"...)</li>
 * <li>isEnabled (set to false to disable)</li>
 * </ul>
 */
public class SnapshotHandler extends FileHandler {

  // the following can be configured by Logging.properties
  private final int maxEvents = 1000000;
  private final double timeout = 30.0;
  protected final String filePath = "request-"; //$NON-NLS-1$
  // Enable scope logs by default
  private volatile boolean isEnabled = true;

  private Deque<InnerEvent> fData = new ArrayDeque<>();
  private Map<String, Map<String, List<InnerEvent>>> fStacks = new HashMap<>();

  /**
   * Snapshot handler constructor
   *
   * @throws IOException
   *             If there is a file error
   * @throws SecurityException
   *             if we don't have the permissions
   */
  public SnapshotHandler() throws IOException, SecurityException {
    super();
  }

  @Override
  public boolean isLoggable(@Nullable LogRecord record) {
    return (isEnabled && record != null
            && super.isLoggable(record)
            && (record.getLevel().intValue() <= Level.FINE.intValue())
            && (record instanceof TraceCompassLogRecord)); // add feature switch here
  }



  private boolean addToSnapshot(LogRecord message) {
    InnerEvent event = InnerEvent.create(message);
    if (event == null) {
      return false;
    }
    fData.add(event);
    while (fData.size() > maxEvents) {
      fData.remove();
    }
    Map<String, List<InnerEvent>> pidMap =
        fStacks.computeIfAbsent(event.getPid(), unused -> new HashMap<>());
    List<InnerEvent> stack = pidMap.computeIfAbsent(event.getTid(), unused -> new ArrayList<>());
    String phase = event.getPhase();
    switch (phase) {
      case "B": //$NON-NLS-1$
        {
          stack.add(event);
          break;
        }
      case "E": //$NON-NLS-1$
        {
          InnerEvent lastEvent = stack.remove(stack.size() - 1);
          if (stack.isEmpty()) {
            double delta = event.getTs() - lastEvent.getTs();
            if (delta > timeout) {
              drain(fData);
            }
          }
          break;
        }
      default:
        // do nothing
    }
    return true;
  }

  @Override
    public synchronized void publish(@Nullable LogRecord record) {
        if(record != null) {
            addToSnapshot(record);
        }
        super.publish(record);
    }

  private void drain(Deque<InnerEvent> data) {
    Thread thread =
        new Thread(
            () -> {
              Path path = new File(filePath + data.getFirst().getTs() + ".json").toPath(); //$NON-NLS-1$
              try (BufferedWriter fw = Files.newBufferedWriter(path, Charset.defaultCharset())) {
                fw.write('[');
                boolean first = true;
                for (InnerEvent event : data) {
                  if (first) {
                    first = false;
                  } else {
                    fw.write(',');
                    fw.write('\n');
                  }
                  fw.write(event.getMessage());
                }
                data.clear();
                fw.write(']');
              } catch (IOException e) {
                // we tried!
              }
            });
    thread.setName("Trace Drainer"); //$NON-NLS-1$
    thread.start();
  }

  /**
   * Enable or disable snapshotter
   * @param isEnabled true is enabled, false is disabled
   */
  public void setEnabled(Boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  /**
   * Is the snapshotter enabled?
   * @return true if enabled
   */
  public boolean isEnabled() {
    return isEnabled;
  }
}
