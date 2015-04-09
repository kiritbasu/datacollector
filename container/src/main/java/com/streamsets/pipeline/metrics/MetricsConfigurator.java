/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Timer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

public class MetricsConfigurator {
  public static final String JMX_PREFIX = "pipeline_";

  private static final String METER_SUFFIX = ".meter";
  private static final String COUNTER_SUFFIX = ".counter";
  private static final String HISTOGRAM_M5_SUFFIX = ".histogramM5";
  private static final String TIMER_SUFFIX = ".timer";
  private static final String GAUGE_SUFFIX = ".gauge";

  private static String metricName(String name, String type) {
    if (name.endsWith(type)) {
      return name;
    }
    return name + type;
  }


  //we need to use the AccessController.doPrivilege for sdcMetric calls because there are calls to JMX MBs
  //and user-libs stages fail otherwise due to lesser privileges
  public static Timer createTimer(MetricRegistry metrics, String name) {
    final String timerName = metricName(name, TIMER_SUFFIX);
    final Timer timer = new Timer(new SlidingTimeWindowReservoir(60, TimeUnit.SECONDS));
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.register(JMX_PREFIX + timerName, timer);
          return null;
        }
      });
    }
    return metrics.register(timerName, timer);
  }

  public static Meter createMeter(MetricRegistry metrics, String name) {
    final String meterName = metricName(name, METER_SUFFIX);
    final ExtendedMeter meter = new ExtendedMeter();
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.register(JMX_PREFIX + meterName, meter);
          return null;
        }
      });
    }
    return metrics.register(meterName, meter);
  }

  public static Counter createCounter(MetricRegistry metrics, String name) {
    final String counterName = metricName(name, COUNTER_SUFFIX);
    final Counter counter = new Counter();
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.register(JMX_PREFIX + counterName, counter);
          return null;
        }
      });
    }
    return metrics.register(counterName, counter);
  }

  public static Histogram createHistogram5Min(MetricRegistry metrics, String name) {
    final String histogramName = metricName(name, HISTOGRAM_M5_SUFFIX);
    final Histogram histogram = new Histogram(new ExponentiallyDecayingReservoir());
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.register(JMX_PREFIX + histogramName, histogram);
          return null;
        }
      });
    }
    return metrics.register(histogramName, histogram);
  }

  public static Gauge<Object> createGauge(MetricRegistry metrics, String name, final Gauge<Object> guage) {
    final String gaugeName = metricName(name, GAUGE_SUFFIX);
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.register(JMX_PREFIX + gaugeName, guage);
          return null;
        }
      });
    }
    return metrics.register(gaugeName, guage);
  }

  public static Counter getCounter(MetricRegistry metrics, String name) {
    return metrics.getCounters().get(metricName(name, COUNTER_SUFFIX));
  }

  public static ExtendedMeter getMeter(MetricRegistry metrics, String name) {
    return (ExtendedMeter) metrics.getMeters().get(metricName(name, METER_SUFFIX));
  }

  public static Histogram getHistogram(MetricRegistry metrics, String name) {
    return metrics.getHistograms().get(metricName(name, HISTOGRAM_M5_SUFFIX));
  }

  public static Timer getTimer(MetricRegistry metrics, String name) {
    return metrics.getTimers().get(metricName(name, TIMER_SUFFIX));
  }

  public static Gauge<Object> getGauge(MetricRegistry metrics, String name) {
    return metrics.getGauges().get(metricName(name, GAUGE_SUFFIX));
  }

  public static boolean removeGauge(MetricRegistry metrics, String name) {
    final String gaugeName = metricName(name, GAUGE_SUFFIX);
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.remove(JMX_PREFIX + gaugeName);
          return null;
        }
      });
    }
    return metrics.remove(gaugeName);
  }

  public static boolean removeMeter(MetricRegistry metrics, String name) {
    final String meterName = metricName(name, METER_SUFFIX);
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.remove(JMX_PREFIX + meterName);
          return null;
        }
      });
    }
    return metrics.remove(meterName);
  }

  public static boolean removeCounter(MetricRegistry metrics, String name) {
    final String counterName = metricName(name, COUNTER_SUFFIX);
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          sdcMetrics.remove(JMX_PREFIX + counterName);
          return null;
        }
      });
    }
    return metrics.remove(counterName);
  }

  private static MetricRegistry sdcMetrics = null;

  public static void registerJmxMetrics(MetricRegistry metrics) {
    cleanUpJmxMetrics();
    sdcMetrics = metrics;
  }

  public static void cleanUpJmxMetrics() {
    if (sdcMetrics != null) {
      AccessController.doPrivileged(new PrivilegedAction<Void>() {
        @Override
        public Void run() {
          for (String name : sdcMetrics.getNames()) {
            if (name.startsWith(JMX_PREFIX)) {
              sdcMetrics.remove(name);
            }
          }
          return null;
        }
      });
    }
    sdcMetrics = null;
  }

}