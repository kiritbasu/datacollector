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
package com.streamsets.pipeline.lib.parser.log;

import com.streamsets.pipeline.api.OnRecordError;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.config.LogMode;
import com.streamsets.pipeline.lib.data.DataFactory;
import com.streamsets.pipeline.lib.parser.DataParser;
import com.streamsets.pipeline.lib.parser.DataParserException;
import com.streamsets.pipeline.lib.parser.DataParserFactory;
import com.streamsets.pipeline.lib.parser.DataParserFactoryBuilder;
import com.streamsets.pipeline.lib.parser.DataParserFormat;
import com.streamsets.pipeline.sdk.ContextInfoCreator;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

public class TestApacheCustomLogFormatParser {

  private static final String LOG_LINE = "127.0.0.1 ss h [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" " +
    "200 2326 Hello";
  private static final String FORMAT = "%h %l %u [%t] \"%r\" %>s %b";
  private Stage.Context getContext() {
    return ContextInfoCreator.createSourceContext("i", false, OnRecordError.TO_ERROR,
      Collections.<String>emptyList());
  }

  @Test
  public void testParse() throws Exception {
    DataParser parser = getDataParser(LOG_LINE, 1000, 0);
    Assert.assertEquals(0, Long.parseLong(parser.getOffset()));
    Record record = parser.parse();
    Assert.assertNotNull(record);

    Assert.assertEquals("id::0", record.getHeader().getSourceId());

    Assert.assertEquals(LOG_LINE, record.get().getValueAsMap().get("originalLine").getValueAsString());

    Assert.assertFalse(record.has("/truncated"));

    Assert.assertEquals(88, Long.parseLong(parser.getOffset()));

    Assert.assertTrue(record.has("/remoteHost"));
    Assert.assertEquals("127.0.0.1", record.get("/remoteHost").getValueAsString());

    Assert.assertTrue(record.has("/logName"));
    Assert.assertEquals("ss", record.get("/logName").getValueAsString());

    Assert.assertTrue(record.has("/remoteUser"));
    Assert.assertEquals("h", record.get("/remoteUser").getValueAsString());

    Assert.assertTrue(record.has("/requestTime"));
    Assert.assertEquals("10/Oct/2000:13:55:36 -0700", record.get("/requestTime").getValueAsString());

    Assert.assertTrue(record.has("/request"));
    Assert.assertEquals("GET /apache_pb.gif HTTP/1.0", record.get("/request").getValueAsString());

    Assert.assertTrue(record.has("/status"));
    Assert.assertEquals("200", record.get("/status").getValueAsString());

    Assert.assertTrue(record.has("/bytesSent"));
    Assert.assertEquals("2326", record.get("/bytesSent").getValueAsString());

    parser.close();
  }

  @Test
  public void testParseWithOffset() throws Exception {
    DataParser parser = getDataParser("Hello\n" + LOG_LINE, 1000, 6);
    Assert.assertEquals(6, Long.parseLong(parser.getOffset()));
    Record record = parser.parse();
    Assert.assertNotNull(record);

    Assert.assertEquals("id::6", record.getHeader().getSourceId());

    Assert.assertEquals(LOG_LINE, record.get().getValueAsMap().get("originalLine").getValueAsString());

    Assert.assertFalse(record.has("/truncated"));

    Assert.assertEquals(94, Long.parseLong(parser.getOffset()));

    Assert.assertTrue(record.has("/remoteHost"));
    Assert.assertEquals("127.0.0.1", record.get("/remoteHost").getValueAsString());

    Assert.assertTrue(record.has("/logName"));
    Assert.assertEquals("ss", record.get("/logName").getValueAsString());

    Assert.assertTrue(record.has("/remoteUser"));
    Assert.assertEquals("h", record.get("/remoteUser").getValueAsString());

    Assert.assertTrue(record.has("/requestTime"));
    Assert.assertEquals("10/Oct/2000:13:55:36 -0700", record.get("/requestTime").getValueAsString());

    Assert.assertTrue(record.has("/request"));
    Assert.assertEquals("GET /apache_pb.gif HTTP/1.0", record.get("/request").getValueAsString());

    Assert.assertTrue(record.has("/status"));
    Assert.assertEquals("200", record.get("/status").getValueAsString());

    Assert.assertTrue(record.has("/bytesSent"));
    Assert.assertEquals("2326", record.get("/bytesSent").getValueAsString());

    record = parser.parse();
    Assert.assertNull(record);

    Assert.assertEquals(-1, Long.parseLong(parser.getOffset()));
    parser.close();
  }

  @Test(expected = IOException.class)
  public void testClose() throws Exception {
    DataParser parser = getDataParser("Hello\nByte", 1000, 0);
    parser.close();
    parser.parse();
  }

  @Test(expected = DataParserException.class)
  public void testTruncate() throws Exception {
    DataParser parser = getDataParser(LOG_LINE, 25, 0);
    Assert.assertEquals(0, Long.parseLong(parser.getOffset()));
    try {
      parser.parse();
    } finally {
      parser.close();
    }
  }

  @Test(expected = DataParserException.class)
  public void testParseNonLogLine() throws Exception {

    DataParser parser = getDataParser(
      "127.0.0.1 ss h [10/Oct/2000:13:55:36 -0700] This is a log line that does not confirm to common log format",
      1000, 0);
    Assert.assertEquals(0, Long.parseLong(parser.getOffset()));
    try {
      parser.parse();
    } finally {
      parser.close();
    }
  }

  private DataParser getDataParser(String logLine, int maxObjectLength, int readerOffset) throws DataParserException {
    InputStream is = new ByteArrayInputStream(logLine.getBytes());
    DataParserFactoryBuilder dataParserFactoryBuilder = new DataParserFactoryBuilder(getContext(), DataParserFormat.LOG);
    DataParserFactory factory = dataParserFactoryBuilder
      .setMaxDataLen(maxObjectLength)
      .setMode(LogMode.APACHE_CUSTOM_LOG_FORMAT)
      .setOverRunLimit(1000)
      .setConfig(LogDataParserFactory.RETAIN_ORIGINAL_TEXT_KEY, true)
      .setConfig(LogDataParserFactory.APACHE_CUSTOMLOG_FORMAT_KEY, FORMAT)
      .build();
    return factory.getParser("id", is, readerOffset);
  }
}
