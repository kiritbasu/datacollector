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
package com.streamsets.datacollector.callback;

import com.streamsets.datacollector.main.RuntimeInfo;
import com.streamsets.datacollector.metrics.MetricsEventListener;
import com.streamsets.datacollector.restapi.bean.BeanHelper;
import com.streamsets.datacollector.util.AuthzRole;
import com.streamsets.pipeline.api.impl.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import java.util.Map;


public class CallbackServerMetricsEventListener implements MetricsEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(CallbackServerMetricsEventListener.class);
  private static final boolean IS_TRACE_ENABLED = LOG.isTraceEnabled();
  private final String name;
  private final String rev;
  private final String user;
  private final RuntimeInfo runtimeInfo;
  private final String callbackServerURL;
  private final String sdcClusterToken;
  private final String sdcSlaveToken;

  public CallbackServerMetricsEventListener(String user, String name, String rev,
    RuntimeInfo runtimeInfo, String callbackServerURL, String sdcClusterToken, String sdcSlaveToken) {
    this.name = name;
    this.rev = rev;
    this.user = user;
    this.runtimeInfo = runtimeInfo;
    this.callbackServerURL = callbackServerURL;
    Utils.checkNotNull(sdcClusterToken, "SDC Cluster Token");
    this.sdcClusterToken = sdcClusterToken;
    Utils.checkNotNull(sdcSlaveToken, "SDC Slave Token");
    this.sdcSlaveToken = sdcSlaveToken;
  }

  @Override
  public void notification(String metrics) {
    try {
      Map<String, String> authenticationToken = runtimeInfo.getAuthenticationTokens();
      CallbackInfo callbackInfo =
        new CallbackInfo(user, name, rev, sdcClusterToken, sdcSlaveToken, runtimeInfo.getBaseHttpUrl(),
          authenticationToken.get(AuthzRole.ADMIN), authenticationToken.get(AuthzRole.CREATOR),
          authenticationToken.get(AuthzRole.MANAGER), authenticationToken.get(AuthzRole.GUEST), metrics);
      if (IS_TRACE_ENABLED) {
        LOG.trace("Calling back on " + callbackServerURL + " with the sdc url of " + runtimeInfo.getBaseHttpUrl());
      }
      Response response = ClientBuilder
        .newClient()
        .target(callbackServerURL)
        .request()
        .post(Entity.json(BeanHelper.wrapCallbackInfo(callbackInfo)));

      if (response.getStatus() != 200) {
        throw new RuntimeException("Failed : HTTP error code : "
          + response.getStatus());
      }
    } catch (Exception ex) {
      LOG.warn("Error while calling callback to Callback Server , {}", ex.toString(), ex);
    }
  }
}
