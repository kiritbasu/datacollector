/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.runner.production;

import com.streamsets.pipeline.alerts.TestUtil;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.config.DataRuleDefinition;
import com.streamsets.pipeline.config.ThresholdType;
import com.streamsets.pipeline.util.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

public class TestProductionObserver {

  private static final String LANE = "lane";
  private static final String ID = "myId";
  private static final int NUMBER_OF_RECORDS_PER_BATCH = 3;
  private static final int NUMBER_OF_BATCHES = 1000;

  private static ProductionObserver productionObserver;

  @Before
  public void setUp() {
    productionObserver = new ProductionObserver(new ArrayBlockingQueue<>(10), new Configuration());
  }

  @Test
  public void testGetSampledRecords() {
    List<DataRuleDefinition> dataRuleDefinitions = new ArrayList<>();
    dataRuleDefinitions.add(new DataRuleDefinition(ID+1, "myRule", LANE + "::s", 100 /*Sampling %*/, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));
    dataRuleDefinitions.add(new DataRuleDefinition(ID+2, "myRule", LANE + "::s", 50 /*Sampling %*/, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));
    dataRuleDefinitions.add(new DataRuleDefinition(ID+3, "myRule", LANE + "::s", 25 /*Sampling %*/, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));
    dataRuleDefinitions.add(new DataRuleDefinition(ID+4, "myRule", LANE + "::s", 10 /*Sampling %*/, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));

    //Generates 100 records
    List<Record> allRecords = TestUtil.createRecords(500);

    Map<String, List<Record>> sampleRecords = productionObserver.getSampleRecords(dataRuleDefinitions, allRecords,
      LANE);

    Assert.assertEquals(500, sampleRecords.get(ID + 1).size());
    Assert.assertEquals(250, sampleRecords.get(ID + 2).size());
    Assert.assertEquals(125, sampleRecords.get(ID + 3).size());
    Assert.assertEquals(50, sampleRecords.get(ID + 4).size());
  }

  @Test
  public void testGetSampledRecordsLowThroughput() {

    //To get an idea of how sampling behaves vary the NUMBER_OF_BATCHES and NUMBER_OF_RECORDS_PER_BATCH variables
    //and print out the result by un commenting the following.

    List<DataRuleDefinition> dataRuleDefinitions = new ArrayList<>();
    dataRuleDefinitions.add(new DataRuleDefinition(ID+1, "myRule", LANE + "::s", 100 /*Sampling %*/, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));
    dataRuleDefinitions.add(new DataRuleDefinition(ID+2, "myRule", LANE + "::s", 50 /*Sampling % */, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));
    dataRuleDefinitions.add(new DataRuleDefinition(ID+3, "myRule", LANE + "::s", 25 /* Sampling % */, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));
    dataRuleDefinitions.add(new DataRuleDefinition(ID+4, "myRule", LANE + "::s", 10 /* Sampling % */, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));
    dataRuleDefinitions.add(new DataRuleDefinition(ID+5, "myRule", LANE + "::s", 5 /* Sampling % */, 5,
      "${record:value(\"/name\")==null}", true, "alertText", ThresholdType.COUNT, "2", 5, true, false, true));

    //Generates 100 records
    List<Record> allRecords = TestUtil.createRecords(NUMBER_OF_RECORDS_PER_BATCH);

    Map<String, Integer> ruleIdToSampledRecordsSize = new HashMap<>();
    ruleIdToSampledRecordsSize.put(ID + 1, 0);
    ruleIdToSampledRecordsSize.put(ID + 2, 0);
    ruleIdToSampledRecordsSize.put(ID + 3, 0);
    ruleIdToSampledRecordsSize.put(ID + 4, 0);
    ruleIdToSampledRecordsSize.put(ID + 5, 0);

    for(int i = 0; i < NUMBER_OF_BATCHES; i++) {
      Map<String, List<Record>> sampleRecords = productionObserver.getSampleRecords(dataRuleDefinitions, allRecords,
        LANE);
      for(int j = 1; j < 6; j++) {
        if(sampleRecords.containsKey(ID + j)) {
          ruleIdToSampledRecordsSize.put(ID + j,
            ruleIdToSampledRecordsSize.get(ID + j) + sampleRecords.get(ID + j).size());
        }
      }
    }

    Assert.assertEquals(3000, ruleIdToSampledRecordsSize.get(ID + 1).intValue());
    Assert.assertEquals(1500, ruleIdToSampledRecordsSize.get(ID + 2).intValue());
    Assert.assertEquals(750, ruleIdToSampledRecordsSize.get(ID + 3).intValue());
    Assert.assertEquals(300, ruleIdToSampledRecordsSize.get(ID + 4).intValue());
    Assert.assertEquals(150, ruleIdToSampledRecordsSize.get(ID + 5).intValue());

    /*System.out.println("Records for rule myID1 : " + ruleIdToSampledRecordsSize.get(ID + 1));
    System.out.println("Records for rule myID2 : " + ruleIdToSampledRecordsSize.get(ID + 2));
    System.out.println("Records for rule myID3 : " + ruleIdToSampledRecordsSize.get(ID + 3));
    System.out.println("Records for rule myID4 : " + ruleIdToSampledRecordsSize.get(ID + 4));
    System.out.println("Records for rule myID5 : " + ruleIdToSampledRecordsSize.get(ID + 5));*/
  }
}