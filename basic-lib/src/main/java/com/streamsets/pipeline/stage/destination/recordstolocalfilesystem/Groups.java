/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.stage.destination.recordstolocalfilesystem;

import com.streamsets.pipeline.api.Label;

public enum Groups implements Label {
  FILES;

  @Override
  public String getLabel() {
    return "Files";
  }
}