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
package com.streamsets.datacollector.websockets;

import com.streamsets.datacollector.execution.PipelineState;
import com.streamsets.datacollector.execution.StateEventListener;
import com.streamsets.dc.execution.manager.standalone.ThreadUsage;

import java.util.Queue;


public class StatusWebSocket extends BaseWebSocket implements StateEventListener{
  public static final String TYPE = "status";

  public StatusWebSocket(ListenerManager<StateEventListener> listenerManager, Queue<WebSocketMessage> queue) {
    super(TYPE, listenerManager, queue);
  }

  @Override
  public void onStateChange(PipelineState fromState, PipelineState toState, String toStateJson,
                            ThreadUsage threadUsage) {
    notification(toStateJson);
  }
}
