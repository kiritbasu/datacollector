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
package com.streamsets.datacollector.execution.runner.provider;

import com.streamsets.datacollector.execution.Runner;
import com.streamsets.datacollector.execution.manager.RunnerProvider;
import com.streamsets.datacollector.execution.runner.slave.dagger.SlaveRunnerModule;
import com.streamsets.datacollector.execution.runner.standalone.dagger.StandaloneRunnerInjectorModule;
import com.streamsets.pipeline.api.ExecutionMode;

import dagger.ObjectGraph;

import javax.inject.Inject;

public class SlaveRunnerProviderImpl implements RunnerProvider {

  @Inject
  public SlaveRunnerProviderImpl() {
  }

  @Override
  public Runner createRunner( String user, String name, String rev, ObjectGraph objectGraph,
      ExecutionMode executionMode) {
    objectGraph = objectGraph.plus(StandaloneRunnerInjectorModule.class);
    ObjectGraph plus =  objectGraph.plus(new SlaveRunnerModule(user, name, rev, objectGraph));
    return plus.get(Runner.class);
  }
}
