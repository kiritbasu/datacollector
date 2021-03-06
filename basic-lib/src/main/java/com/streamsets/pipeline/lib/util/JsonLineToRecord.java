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
package com.streamsets.pipeline.lib.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Source;

public class JsonLineToRecord implements ToRecord {
  private ObjectMapper objectMapper;

  public JsonLineToRecord() {
    objectMapper = new ObjectMapper();

  }

  public Field parse(String line) throws ToRecordException {
    try {
      Object json = objectMapper.readValue(line, Object.class);
      return JsonUtil.jsonToField(json);
    } catch (Exception ex) {
      throw new ToRecordException(Errors.RECORD_00, line, ex.toString(), ex);
    }
  }

  @Override
  public Record createRecord(Source.Context context, String sourceFile, long offset, String line, boolean truncated)
      throws ToRecordException {
    try {
      Record record = context.createRecord(sourceFile + "::" + offset);
      record.set(parse(line));
      return record;
    } catch (Exception ex) {
      throw new ToRecordException(Errors.RECORD_00, line, ex.toString(), ex);
    }
  }

}
