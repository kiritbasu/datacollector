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
package com.streamsets.pipeline.stage.processor.geolocation;

import com.streamsets.pipeline.api.ErrorCode;
import com.streamsets.pipeline.api.GenerateResourceBundle;

@GenerateResourceBundle
public enum Errors implements ErrorCode {
  GEOIP_00("Database file '{}' does not exist"),
  GEOIP_01("Error reading database file '{}': '{}'"),
  GEOIP_02("Address '{}' not found: '{}'"),
  GEOIP_03("Unknown geolocation occurred: '{}'"),
  GEOIP_04("At least one field is required"),
  GEOIP_05("Supplied database does not support: '{}'"),
  GEOIP_06("String IP addresses must be dot delimited [0-9].[0-9].[0-9].[0-9] or an integer: '{}'"),
  GEOIP_07("Unknown error occurred during initialization: '{}'"),
  GEOIP_08("Input field name is empty"),
  GEOIP_09("Output field name is empty"),
  GEOIP_10("Database file '{}' must be relative to SDC resources directory in cluster mode"),
  ;


  private final String msg;
  Errors(String msg) {
    this.msg = msg;
  }

  @Override
  public String getCode() {
    return name();
  }

  @Override
  public String getMessage() {
    return msg;
  }

}
