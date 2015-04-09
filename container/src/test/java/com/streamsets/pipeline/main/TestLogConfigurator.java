/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.main;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.helpers.FileWatchdog;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.UUID;

public class TestLogConfigurator {

  @Before
  public void setUp() throws Exception {
    cleanUp();
  }

  @After
  public void cleanUp() throws Exception {
    System.getProperties().remove("log4j.configuration");
    System.getProperties().remove("log4j.defaultInitOverride");
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      if (thread instanceof FileWatchdog) {
        Field interrupted = ((Class)thread.getClass().getGenericSuperclass()).getDeclaredField("interrupted");
        interrupted.setAccessible(true);
        interrupted.set(thread, true);
        thread.interrupt();
      }
    }
  }

  @Test
  public void testExternalLog4jConfig() {
    System.setProperty("log4j.configuration", "foo");
    RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
    new LogConfigurator(runtimeInfo).configure();
    Mockito.verify(runtimeInfo, Mockito.times(0)).getConfigDir();
    Mockito.verify(runtimeInfo, Mockito.times(0)).setAttribute(Mockito.eq(RuntimeInfo.LOG4J_CONFIGURATION_URL_ATTR),
                                                               Mockito.any());
  }

  @Test
  public void testClasspathLog4jConfig() {
    RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
    File configDir = new File("target", UUID.randomUUID().toString());
    Assert.assertTrue(configDir.mkdirs());
    Mockito.when(runtimeInfo.getConfigDir()).thenReturn(configDir.getAbsolutePath());
    new LogConfigurator(runtimeInfo).configure();
    Mockito.verify(runtimeInfo, Mockito.times(1)).getConfigDir();
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
     Assert.assertFalse(thread instanceof FileWatchdog);
    }
    Mockito.verify(runtimeInfo, Mockito.times(1)).setAttribute(Mockito.eq(RuntimeInfo.LOG4J_CONFIGURATION_URL_ATTR),
                                                               Mockito.any());
  }

  @Test
  public void testConfigDirLog4jConfig() throws IOException {
    RuntimeInfo runtimeInfo = Mockito.mock(RuntimeInfo.class);
    File configDir = new File("target", UUID.randomUUID().toString());
    Assert.assertTrue(configDir.mkdirs());
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("log4j.properties");
    OutputStream os = new FileOutputStream(new File(configDir, "log4j.properties"));
    IOUtils.copy(is, os);
    is.close();
    os.close();
    Mockito.when(runtimeInfo.getConfigDir()).thenReturn(configDir.getAbsolutePath());
    new LogConfigurator(runtimeInfo).configure();
    Mockito.verify(runtimeInfo, Mockito.times(1)).getConfigDir();
    boolean foundFileWatcher = false;
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      foundFileWatcher |= (thread instanceof FileWatchdog);
    }
    Assert.assertTrue(foundFileWatcher);
    Mockito.verify(runtimeInfo, Mockito.times(1)).setAttribute(Mockito.eq(RuntimeInfo.LOG4J_CONFIGURATION_URL_ATTR),
                                                               Mockito.any());
  }
}