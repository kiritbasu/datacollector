package com.streamsets.pipeline.stage.origin;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by brock on 4/20/15.
 */
public class EmbeddedSDC {
  private static final AtomicInteger instanceIdCounter = new AtomicInteger(0);
  private final int instanceId;
  private SparkStreamingSource source;

  public EmbeddedSDC() {
    instanceId = instanceIdCounter.getAndIncrement();
  }

  public int getInstanceId() {
    return instanceId;
  }

  public SparkStreamingSource getSource() {
    return source;
  }

  public void setSource(SparkStreamingSource source) {
    this.source = source;
  }
}