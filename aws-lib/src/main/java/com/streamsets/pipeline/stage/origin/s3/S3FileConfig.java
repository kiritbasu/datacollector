/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.origin.s3;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.Stage;

import java.util.List;

public class S3FileConfig {

  private static final int MIN_OVERRUN_LIMIT = 64 * 1000;
  private static final int MAX_OVERRUN_LIMIT = 1000 * 1000;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.STRING,
    label = "File Name Pattern",
    description = "A glob or regular expression that defines the pattern of the file names in the directory",
    displayPosition = 100,
    group = "#0"
  )
  public String filePattern;

  @ConfigDef(
    required = true,
    type = ConfigDef.Type.NUMBER,
    label = "Buffer Limit (KB)",
    defaultValue = "64000",
    description = "Low level reader buffer limit to avoid out of Memory errors",
    displayPosition = 120,
    group = "#0",
    min = 1,
    max = Integer.MAX_VALUE
  )
  public int overrunLimit;

  public void init(Stage.Context context, List<Stage.ConfigIssue> issues) {
    validate(context, issues);
  }

  private void validate(Stage.Context context, List<Stage.ConfigIssue> issues) {
    if (overrunLimit < MIN_OVERRUN_LIMIT || overrunLimit >= MAX_OVERRUN_LIMIT) {
      issues.add(context.createConfigIssue(Groups.S3.name(), "overrunLimit", Errors.S3_SPOOLDIR_04));
    }
    if(filePattern == null || filePattern.isEmpty()) {
      issues.add(context.createConfigIssue(Groups.S3.name(), "filePattern", Errors.S3_SPOOLDIR_06));
    }
  }
}
