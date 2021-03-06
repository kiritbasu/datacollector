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

import com.streamsets.pipeline.api.Batch;
import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.SingleLaneProcessor;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

@GenerateResourceBundle
@StageDef(
  version = 2,
  label = "Dev Random Error",
  description = "Generates error records and silently discards records as specified.",
  icon= "dev.png",
  upgrader = RandomErrorProcessorUpgrader.class
)
public class RandomErrorProcessor extends SingleLaneProcessor {
  private Random random;
  private int batchCount;
  private double batchThreshold1;
  private double batchThreshold2;

  @ConfigDef(label = "Discard Some Records",
    required = false,
    type = ConfigDef.Type.BOOLEAN,
    defaultValue = "false",
    description = "Silently discard some records ")
  public boolean discardSomeRecords;

  @Override
  protected List<ConfigIssue> init() {
    List<ConfigIssue> issues = super.init();
    random = new Random();
    return issues;
  }

  @Override
  public void process(Batch batch, SingleLaneBatchMaker batchMaker) throws
      StageException {
    if (batchCount++ % 500 == 0) {
      batchThreshold1 = random.nextDouble();
      batchThreshold2 = batchThreshold1 * (1 + random.nextDouble());
    }
    Iterator<Record> it = batch.getRecords();
    while (it.hasNext()) {
      double action = random.nextDouble();
      if(discardSomeRecords) {
        if (action < batchThreshold1) {
          batchMaker.addRecord(it.next());
        } else if (action < batchThreshold2) {
          getContext().toError(it.next(), "Random error");
        } else {
          // we eat the record
          it.next();
        }
      } else {
        if (action < batchThreshold1) {
          batchMaker.addRecord(it.next());
        } else {
          getContext().toError(it.next(), "Random error");
        }
      }
    }

    //generate error message 50% of the time
    if(random.nextFloat() < 0.5) {
      getContext().reportError("Error reported by the RandomErrorProcessor");
    }
  }

}
