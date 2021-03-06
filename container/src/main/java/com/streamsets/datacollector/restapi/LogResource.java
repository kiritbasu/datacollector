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
package com.streamsets.datacollector.restapi;

import com.streamsets.datacollector.log.LogStreamer;
import com.streamsets.datacollector.log.LogUtils;
import com.streamsets.datacollector.main.RuntimeInfo;
import com.streamsets.datacollector.util.AuthzRole;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/v1/system")
@Api(value = "system")
@DenyAll
public class LogResource {
  public static final String X_SDC_LOG_PREVIOUS_OFFSET_HEADER = "X-SDC-LOG-PREVIOUS-OFFSET";
  private final String logFile;

  @Inject
  public LogResource(RuntimeInfo runtimeInfo) throws RuntimeException {
    try {
      logFile = LogUtils.getLogFile(runtimeInfo);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @GET
  @Path("/logs")
  @ApiOperation(value= "Return latest log file contents")
  @Produces(MediaType.TEXT_PLAIN)
  @RolesAllowed({AuthzRole.ADMIN, AuthzRole.CREATOR, AuthzRole.MANAGER})
  public Response currentLog(@QueryParam("endingOffset") @DefaultValue("-1") long offset) throws IOException {
    final LogStreamer streamer = new LogStreamer(logFile, offset, 50 * 1024);
    StreamingOutput stream = new StreamingOutput() {
      @Override
      public void write(OutputStream output) throws IOException, WebApplicationException {
        try {
          streamer.stream(output);
        } finally {
          streamer.close();
        }
      }
    };
    return Response.ok(stream).header(X_SDC_LOG_PREVIOUS_OFFSET_HEADER, streamer.getNewEndingOffset()).build();
  }

  private File[] getLogFiles() throws IOException {
    File log = new File(logFile);
    File logDir = log.getParentFile();
    final String logName = log.getName();
    return logDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith(logName);
      }
    });
  }

  @GET
  @Path("/logs/files")
  @ApiOperation(value = "Returns all available SDC Log files", response = Map.class, responseContainer = "List",
    authorizations = @Authorization(value = "basic"))
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed({AuthzRole.ADMIN, AuthzRole.CREATOR, AuthzRole.MANAGER})
  @SuppressWarnings("unchecked")
  public Response listLogFiles() throws IOException {
    File[] logFiles = getLogFiles();
    List<Map> list = new ArrayList<>();
    for (File logFile : logFiles) {
      Map map = new HashMap();
      map.put("file", logFile.getName());
      map.put("lastModified", logFile.lastModified());
      list.add(map);
    }
    return Response.ok(list).build();
  }

  @GET
  @Path("/logs/files/{logName}")
  @ApiOperation(value = "Returns SDC Log File Content", response = String.class,
    authorizations = @Authorization(value = "basic"))
  @Produces(MediaType.TEXT_PLAIN)
  @RolesAllowed({AuthzRole.ADMIN, AuthzRole.CREATOR, AuthzRole.MANAGER})
  public Response getLogFile(@PathParam("logName") String logName,
                             @QueryParam("attachment") @DefaultValue("false") Boolean attachment) throws IOException {
    Response response;
    File logFile = null;
    for (File file : getLogFiles()) {
      if (file.getName().equals(logName)) {
        logFile = file;
        break;
      }
    }
    if (logFile != null) {
      if(attachment) {
        return Response.ok().
          header("Content-Disposition", "attachment; filename=" + logName).entity(new FileInputStream(logFile)).build();
      } else {
        response = Response.ok(new FileInputStream(logFile)).build();
      }

    } else {
      response = Response.status(Response.Status.NOT_FOUND).build();
    }
    return response;
  }

}
