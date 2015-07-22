/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.dc.execution.runner.common;

import com.codahale.metrics.MetricRegistry;
import com.streamsets.dc.execution.Manager;
import com.streamsets.dc.execution.PipelineStateStore;
import com.streamsets.dc.execution.PipelineStatus;
import com.streamsets.dc.execution.Runner;
import com.streamsets.dc.execution.manager.standalone.StandaloneAndClusterPipelineManager;
import com.streamsets.dc.execution.runner.standalone.StandaloneRunner;
import com.streamsets.dc.execution.snapshot.common.SnapshotInfoImpl;
import com.streamsets.dc.execution.snapshot.file.FileSnapshotStore;
import com.streamsets.pipeline.api.BatchMaker;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseSource;
import com.streamsets.pipeline.config.DeliveryGuarantee;
import com.streamsets.pipeline.config.MemoryLimitConfiguration;
import com.streamsets.pipeline.main.RuntimeInfo;
import com.streamsets.pipeline.main.RuntimeModule;
import com.streamsets.pipeline.runner.MockStages;
import com.streamsets.pipeline.runner.PipelineRuntimeException;
import com.streamsets.pipeline.runner.SourceOffsetTracker;
import com.streamsets.pipeline.store.PipelineStoreTask;
import com.streamsets.pipeline.util.Configuration;
import com.streamsets.pipeline.util.PipelineException;
import com.streamsets.pipeline.util.TestUtil;

import dagger.ObjectGraph;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

public class TestProdPipelineRunnable {

  private static final String SNAPSHOT_NAME = "snapshot";
  private Runner runner;
  private PipelineStoreTask pipelineStoreTask;
  private RuntimeInfo info;
  private PipelineStateStore pipelineStateStore;
  private Manager manager;

  @BeforeClass
  public static void beforeClass() throws IOException {
    System.setProperty(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR, "./target/var");
    File f = new File(System.getProperty(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR));
    FileUtils.deleteDirectory(f);
  }

  @AfterClass
  public static void afterClass() throws IOException {
    System.getProperties().remove(RuntimeModule.SDC_PROPERTY_PREFIX + RuntimeInfo.DATA_DIR);
  }

  @Before()
  public void setUp() throws Exception {
    MockStages.resetStageCaptures();
    info = new RuntimeInfo(RuntimeModule.SDC_PROPERTY_PREFIX, new MetricRegistry(),
      Arrays.asList(getClass().getClassLoader()));
    ObjectGraph objectGraph = ObjectGraph.create(new TestUtil.TestPipelineManagerModule());
    pipelineStateStore = objectGraph.get(PipelineStateStore.class);
    manager = new StandaloneAndClusterPipelineManager(objectGraph);
    manager.init();
    runner = manager.getRunner("admin", TestUtil.MY_PIPELINE, "0");
  }

  @After
  public void tearDown() {

  }

  @Test
  public void testRun() throws Exception {
    TestUtil.captureMockStages();
    ProductionPipeline pipeline = createProductionPipeline(DeliveryGuarantee.AT_MOST_ONCE, true);
    ProductionPipelineRunnable runnable =
      new ProductionPipelineRunnable(null, (StandaloneRunner) ((AsyncRunner)runner).getRunner(), pipeline, TestUtil.MY_PIPELINE, "0",
        Collections.<Future<?>> emptyList());
    pipelineStateStore.saveState("admin", TestUtil.MY_PIPELINE, "0", PipelineStatus.RUNNING, null, null, null);
    runnable.run();
    // The source returns null offset because all the data from source was read
    Assert.assertNull(pipeline.getCommittedOffset());
  }

  @Test
  public void testStop() throws Exception {
    TestUtil.captureMockStages();
    ProductionPipeline pipeline = createProductionPipeline(DeliveryGuarantee.AT_MOST_ONCE, false);
    ProductionPipelineRunnable runnable = new ProductionPipelineRunnable
      (null, (StandaloneRunner) ((AsyncRunner)runner).getRunner(), pipeline, TestUtil.MY_PIPELINE, "0",
      Collections.<Future<?>>emptyList());
    pipelineStateStore.saveState("admin", TestUtil.MY_PIPELINE, "0", PipelineStatus.RUNNING, null, null, null);
    //Stops after the first batch
    runnable.run();
    runnable.stop(false);
    Assert.assertTrue(pipeline.wasStopped());
  }

  private volatile CountDownLatch latch;
  private volatile boolean stopInterrupted;

  @Test
  public void testStopInterrupt() throws Exception {
    latch = new CountDownLatch(1);
    stopInterrupted = false;
    MockStages.setSourceCapture(new BaseSource() {
      @Override
      public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
        try {
          latch.countDown();
          Thread.currentThread().sleep(1000000);
        } catch (InterruptedException ex) {
          stopInterrupted = true;
        }
        return null;
      }
    });

    ProductionPipeline pipeline = createProductionPipeline(DeliveryGuarantee.AT_MOST_ONCE, false);
    ProductionPipelineRunnable runnable =
      new ProductionPipelineRunnable(null, (StandaloneRunner) ((AsyncRunner) runner).getRunner(), pipeline,
        TestUtil.MY_PIPELINE, "0", Collections.<Future<?>> emptyList());

    Thread t = new Thread(runnable);
    t.start();
    latch.await();
    runnable.stop(false);
    t.join();
    Assert.assertTrue(stopInterrupted);
  }

  private ProductionPipeline createProductionPipeline(DeliveryGuarantee deliveryGuarantee, boolean captureNextBatch)
    throws StageException, PipelineException {
    RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
    Mockito.when(runtimeInfo.getId()).thenReturn("id");

    SourceOffsetTracker tracker = new TestUtil.SourceOffsetTrackerImpl("1");
    FileSnapshotStore snapshotStore = Mockito.mock(FileSnapshotStore.class);

    Mockito.when(snapshotStore.getInfo(TestUtil.MY_PIPELINE, "0", SNAPSHOT_NAME)).
      thenReturn(new SnapshotInfoImpl("user", SNAPSHOT_NAME, TestUtil.MY_PIPELINE, "0", System.currentTimeMillis(), false));
    BlockingQueue<Object> productionObserveRequests = new ArrayBlockingQueue<>(100, true /*FIFO*/);
    ProductionPipelineRunner runner =
      new ProductionPipelineRunner(TestUtil.MY_PIPELINE, "0", new Configuration(), runtimeInfo, new MetricRegistry(), snapshotStore,
        null, null);
    runner.setDeliveryGuarantee(deliveryGuarantee);
    runner.setMemoryLimitConfiguration(new MemoryLimitConfiguration());
    runner.setObserveRequests(productionObserveRequests);
    runner.setOffsetTracker(tracker);

    ProductionPipeline pipeline = new ProductionPipelineBuilder(TestUtil.MY_PIPELINE, "0", runtimeInfo, MockStages.createStageLibrary(),
      runner, null).build(MockStages.createPipelineConfigurationSourceProcessorTarget());

    pipelineStateStore.saveState("admin", TestUtil.MY_PIPELINE, "0", PipelineStatus.STOPPED, null, null, null);

    if(captureNextBatch) {
      runner.capture(SNAPSHOT_NAME, 1, 1);
    }

    return pipeline;
  }

}