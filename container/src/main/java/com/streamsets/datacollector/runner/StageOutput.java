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
package com.streamsets.datacollector.runner;

import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.impl.ErrorMessage;
import com.streamsets.pipeline.api.impl.Utils;

import java.util.List;
import java.util.Map;

public class StageOutput {
  private final String instanceName;
  private final Map<String, List<Record>> output;
  private final List<Record> errorRecords;
  private final List<ErrorMessage> stageErrors;

  @SuppressWarnings("unchecked")
  public StageOutput(String instanceName, Map<String, List<Record>> output, ErrorSink errorSink) {
    this(instanceName, (Map) output, (List) errorSink.getErrorRecords(instanceName),
         errorSink.getStageErrors().get(instanceName));
  }

  public StageOutput(String instanceName, Map<String, List<Record>> output, List<Record> errorRecords,
        List<ErrorMessage> stageErrors) {
    this.instanceName = instanceName;
    this.output = (Map) output;
    this.errorRecords = (List) errorRecords;
    this.stageErrors = stageErrors;
  }

  public String getInstanceName() {
    return instanceName;
  }

  public Map<String, List<Record>> getOutput() {
    return output;
  }

  public List<Record> getErrorRecords() {
    return errorRecords;
  }

  public List<ErrorMessage> getStageErrors() {
    return stageErrors;
  }

  @Override
  public String toString() {
    return Utils.format("StageOutput[instance='{}' lanes='{}']", instanceName, output.keySet());
  }

}
