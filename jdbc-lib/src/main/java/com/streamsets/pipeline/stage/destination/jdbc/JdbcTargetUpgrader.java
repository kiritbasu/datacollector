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
package com.streamsets.pipeline.stage.destination.jdbc;

import com.google.common.collect.Lists;
import com.streamsets.pipeline.api.Config;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.StageUpgrader;
import com.streamsets.pipeline.api.impl.Utils;

import java.util.HashMap;
import java.util.List;

/** {@inheritDoc} */
public class JdbcTargetUpgrader implements StageUpgrader {
  @Override
  public List<Config> upgrade(String library, String stageName, String stageInstance, int fromVersion, int toVersion, List<Config> configs) throws StageException {
    switch(fromVersion) {
      case 1:
        upgradeV1ToV2(configs);
        break;
      default:
        throw new IllegalStateException(Utils.format("Unexpected fromVersion {}", fromVersion));
    }
    return configs;
  }

  private void upgradeV1ToV2(List<Config> configs) {
    configs.add(new Config("changeLogFormat", "NONE"));

    Config tableNameConfig = null;
    for (Config config : configs) {
      if (config.getName().equals("qualifiedTableName")) {
        tableNameConfig = config;
      }
    }

    if (null != tableNameConfig) {
      configs.add(new Config("tableName", tableNameConfig.getValue()));
      configs.remove(tableNameConfig);
    }

    Config columnNamesConfig = null;
    for (Config config : configs) {
      if (config.getName().equals("columnNames")) {
        columnNamesConfig = config;
      }
    }

    for (HashMap<String, String> columnName : (List<HashMap<String, String>>)columnNamesConfig.getValue()) {
      columnName.put("paramValue", "?");
    }
  }
}
