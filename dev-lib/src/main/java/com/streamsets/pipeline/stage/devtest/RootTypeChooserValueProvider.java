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
package com.streamsets.pipeline.stage.devtest;

import com.google.common.collect.ImmutableList;
import com.streamsets.pipeline.api.ChooserValues;

import java.util.ArrayList;
import java.util.List;

public class RootTypeChooserValueProvider implements ChooserValues {

  @Override
  public String getResourceBundle() {
    return null;
  }

  @Override
  public List<String> getValues() {
    List<String> values = new ArrayList<>();
    for (RandomDataGeneratorSource.RootType type : RandomDataGeneratorSource.RootType.values()) {
      values.add(type.toString());
    }
    return values;
  }

  @Override
  public List<String> getLabels() {
    List<String> labels = new ArrayList<>();
    for (RandomDataGeneratorSource.RootType type : RandomDataGeneratorSource.RootType.values()) {
      labels.add(type.toString());
    }
    return labels;
  }
}
