/*
 * (c) 2015 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */

package com.streamsets.pipeline.stage.origin.http;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.Source;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.ValueChooser;
import com.streamsets.pipeline.config.DataFormat;
import com.streamsets.pipeline.config.JsonMode;
import com.streamsets.pipeline.config.JsonModeChooserValues;
import com.streamsets.pipeline.configurablestage.DSource;
import org.apache.commons.lang3.StringEscapeUtils;

@StageDef(
    version = "1.0.0",
    label = "HTTP Client",
    description = "Uses an HTTP client to read records from an URL.",
    icon="httpclient.png"
)

@ConfigGroups(Groups.class)
@GenerateResourceBundle
public class HttpClientDSource extends DSource {

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      label = "Data Format",
      defaultValue = "JSON",
      description = "Format of data in the files",
      displayPosition = 0,
      group = "HTTP"
  )
  @ValueChooser(DataFormatChooserValues.class)
  public DataFormat dataFormat;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Resource URL",
      defaultValue = "https://stream.twitter.com/1.1/statuses/sample.json",
      description = "Specify the streaming HTTP resource URL",
      displayPosition = 10,
      group = "HTTP"
  )
  public String resourceUrl;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.NUMBER,
      label = "Request Timeout",
      defaultValue = "1000",
      description = "HTTP request timeout in milliseconds.",
      displayPosition = 20,
      group = "HTTP"
  )
  public long requestTimeoutMillis;


  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      label = "Mode",
      defaultValue = "STREAMING",
      displayPosition = 25,
      group = "HTTP"
  )
  @ValueChooser(HttpClientModeChooserValues.class)
  public HttpClientMode httpMode;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.NUMBER,
      label = "Polling interval (ms)",
      defaultValue = "5000",
      displayPosition = 26,
      group = "HTTP",
      dependsOn = "httpMode",
      triggeredByValue = "POLLING"
  )
  public long pollingInterval;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.BOOLEAN,
      label = "Use OAuth",
      defaultValue = "false",
      description = "Enables OAuth tab for connections requiring authentication.",
      displayPosition = 30,
      group = "HTTP"
  )
  public boolean isOAuthEnabled;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.NUMBER,
      label = "Batch Size",
      defaultValue = "100",
      description = "Maximum number of response entities to queue (e.g. JSON objects).",
      displayPosition = 40,
      group = "HTTP"
  )
  public int batchSize;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.NUMBER,
      label = "Batch Wait Time (ms)",
      defaultValue = "5000",
      description = "Maximum amount of time to wait to fill a batch before sending it",
      displayPosition = 40,
      group = "HTTP"
  )
  public long maxBatchWaitTime;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Consumer Key",
      description = "OAuth Consumer Key",
      displayPosition = 10,
      group = "OAUTH",
      dependsOn = "isOAuthEnabled",
      triggeredByValue = { "true" }
  )
  public String consumerKey;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Consumer Secret",
      description = "OAuth Consumer Secret",
      displayPosition = 20,
      group = "OAUTH",
      dependsOn = "isOAuthEnabled",
      triggeredByValue = { "true" }
  )
  public String consumerSecret;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Token",
      description = "OAuth Consumer Token",
      displayPosition = 30,
      group = "OAUTH",
      dependsOn = "isOAuthEnabled",
      triggeredByValue = { "true" }
  )
  public String token;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Token Secret",
      description = "OAuth Token Secret",
      displayPosition = 40,
      group = "OAUTH",
      dependsOn = "isOAuthEnabled",
      triggeredByValue = "true"
  )
  public String tokenSecret;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "MULTIPLE_OBJECTS",
      label = "JSON Content",
      description = "",
      displayPosition = 10,
      group = "JSON",
      dependsOn = "dataFormat",
      triggeredByValue = "JSON"
  )
  @ValueChooser(JsonModeChooserValues.class)
  public JsonMode jsonMode;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Entity Delimiter",
      defaultValue = "\\r\\n",
      description = "Records may be delimited by a user-defined string. Common values are \\r\\n and \\n",
      displayPosition = 20,
      group = "JSON",
      dependsOn = "jsonMode",
      triggeredByValue = "MULTIPLE_OBJECTS"
  )
  public String entityDelimiter;

  @Override
  protected Source createSource() {
      final String delimiter = StringEscapeUtils.unescapeJava(entityDelimiter);
      if (isOAuthEnabled) {
        return new HttpClientSource(
            httpMode,
            resourceUrl,
            requestTimeoutMillis,
            delimiter,
            batchSize,
            maxBatchWaitTime,
            pollingInterval,
            consumerKey,
            consumerSecret,
            token,
            tokenSecret
        );
      } else {
        return new HttpClientSource(
            httpMode,
            resourceUrl,
            requestTimeoutMillis,
            delimiter,
            batchSize,
            maxBatchWaitTime,
            pollingInterval
        );
      }
  }
}