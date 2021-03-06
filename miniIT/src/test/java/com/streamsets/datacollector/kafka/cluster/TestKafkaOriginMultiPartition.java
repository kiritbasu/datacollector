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
package com.streamsets.datacollector.kafka.cluster;

import com.google.common.io.Resources;
import com.streamsets.datacollector.base.TestPipelineOperationsCluster;
import com.streamsets.pipeline.lib.DataType;
import com.streamsets.pipeline.lib.KafkaTestUtil;
import com.streamsets.pipeline.lib.ProducerRunnable;
import kafka.javaapi.producer.Producer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestKafkaOriginMultiPartition extends TestPipelineOperationsCluster {
  private static final Logger LOG = LoggerFactory.getLogger(TestKafkaOriginMultiPartition.class);
  private static final String TOPIC = "TestKafkaOriginMultiPartitionCluster";
  private static CountDownLatch startLatch;
  private static ExecutorService executorService;


  @BeforeClass
  public static void beforeClass() throws Exception {
    KafkaTestUtil.startZookeeper();
    KafkaTestUtil.startKafkaBrokers(5);
    KafkaTestUtil.createTopic(TOPIC, 3, 2);
    LOG.info("Kafka Broker URIs: " + KafkaTestUtil.getMetadataBrokerURI());
    startLatch = new CountDownLatch(1);
    Producer<String, String> producer = KafkaTestUtil.createProducer(KafkaTestUtil.getMetadataBrokerURI(), true);
    executorService = Executors.newSingleThreadExecutor();
    executorService.submit(new ProducerRunnable(TOPIC, 3, producer, startLatch, DataType.TEXT, null, -1,
      null));
    TestPipelineOperationsCluster.beforeClass(getPipelineJson(), "TestKafkaOriginMultiPartitionCluster");
    startLatch.countDown();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    executorService.shutdownNow();
    KafkaTestUtil.shutdown();
    TestPipelineOperationsCluster.afterClass("TestKafkaOriginMultiPartitionCluster");
  }

  private static String getPipelineJson() throws Exception {
    URI uri = Resources.getResource("kafka_origin_pipeline.json").toURI();
    String pipelineJson =  new String(Files.readAllBytes(Paths.get(uri)), StandardCharsets.UTF_8);
    pipelineJson = pipelineJson.replace("topicName", TOPIC);
    pipelineJson = pipelineJson.replaceAll("localhost:9092", KafkaTestUtil.getMetadataBrokerURI());
    pipelineJson = pipelineJson.replaceAll("localhost:2181", KafkaTestUtil.getZkConnect());
    pipelineJson = pipelineJson.replaceAll("STANDALONE", "CLUSTER");
    return pipelineJson;
  }

  @Override
  protected String getPipelineName() {
    return "kafka_origin_pipeline";
  }

  @Override
  protected String getPipelineRev() {
    return "0";
  }

}
