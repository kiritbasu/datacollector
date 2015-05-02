/**
 * (c) 2015 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.util;

import com.google.common.base.Joiner;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.lib.util.ThreadUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class SystemProcess {
  private static final Logger LOG = LoggerFactory.getLogger(SystemProcess.class);
  private static final Method DESTROY_FORCIBLY;
  static {
    Method destroyForcibly;
    try {
      destroyForcibly = Process.class.getDeclaredMethod("destroyForcibly");
    } catch (NoSuchMethodException e) {
      destroyForcibly = null;
    }
    DESTROY_FORCIBLY = destroyForcibly;
  }
  private final ImmutableList<String> args;
  private final File tempDir;
  private final File input = new File("/dev/null");
  private final File output;
  private final File error;
  private SimpleFileTailer outputTailer;
  private SimpleFileTailer errorTailer;
  private Process delegate;

  public SystemProcess(String name, File tempDir, List<String> args) {
    String uuid = UUID.randomUUID().toString();
    output = new File(tempDir, Utils.format("{}-{}.out", name, uuid));
    error = new File(tempDir, Utils.format("{}-{}.err", name, uuid));
    this.tempDir = tempDir;
    this.args = ImmutableList.copyOf(args);
  }

  public void start() throws IOException {
    Utils.checkState(output.createNewFile(), Utils.formatL("Could not create output file: {}", output));
    Utils.checkState(error.createNewFile(), Utils.formatL("Could not create error file: {}", error));
    Utils.checkState(delegate == null, "start can only be called once");
    ProcessBuilder processBuilder = new ProcessBuilder()
      .redirectInput(input)
      .redirectOutput(output)
      .redirectError(error)
      .directory(tempDir).command(args);
    LOG.info("Starting: " + args);
    delegate = processBuilder.start();
    ThreadUtil.sleep(100); // let it start
    outputTailer = new SimpleFileTailer(output);
    errorTailer = new SimpleFileTailer(error);
  }

  public String getCommand() {
    return Joiner.on(" ").join(args);
  }

  public boolean isAlive() {
    return delegate != null && isAlive(delegate);
  }

  public void cleanup() {
    if (outputTailer != null) {
      outputTailer.close();
    }
    if (errorTailer != null) {
      errorTailer.close();
    }
    error.delete();
    output.delete();
    kill(5000);
  }
  public List<String> getAllOutput() {
    List<String> result = new ArrayList<>();
    if (outputTailer != null) {
      return outputTailer.getAllData();
    }
    return result;
  }

  public List<String> getAllError() {
    List<String> result = new ArrayList<>();
    if (errorTailer != null) {
      return errorTailer.getAllData();
    }
    return result;
  }

  public List<String> getOutput() {
    List<String> result = new ArrayList<>();
    if (outputTailer != null) {
      return outputTailer.getData();
    }
    return result;
  }

  public List<String> getError() {
    List<String> result = new ArrayList<>();
    if (errorTailer != null) {
      return errorTailer.getData();
    }
    return result;
  }

  public void kill(long timeoutBeforeForceKill) {
    if (outputTailer != null) {
      outputTailer.close();
    }
    if (errorTailer != null) {
      errorTailer.close();
    }
    if (delegate != null && isAlive(delegate)) {
      delegate.destroy();
      long start = System.currentTimeMillis();
      while (isAlive(delegate) && (System.currentTimeMillis() - start) > timeoutBeforeForceKill) {
        if (!ThreadUtil.sleep(100)) {
          break;
        }
      }
      if (isAlive(delegate)) {
        if (DESTROY_FORCIBLY != null) {
          try {
            DESTROY_FORCIBLY.invoke(delegate);
          } catch (Exception e) {
            LOG.error("Error trying to call destroyForcibly on {}: {}", delegate, e, e);
          }
        }
      }
    }
  }

  @Override
  public String toString() {
    return Utils.format("SystemProcess: {} ", Joiner.on(" ").join(args));
  }

  public int exitValue() {
    return delegate.exitValue();
  }

  public boolean waitFor(long timeout, TimeUnit unit)
    throws InterruptedException {
    return waitFor(delegate, timeout, unit);
  }

  /**
   * Java 1.7 does not have Process.isAlive
   */
  private static boolean isAlive(Process process) {
    try {
      process.exitValue();
      return false;
    } catch(IllegalThreadStateException e) {
      return true;
    }
  }

  /**
   * Java 1.7 does not have Process.waitFor(timeout)
   */
  private static boolean waitFor(Process process, long timeout, TimeUnit unit)
    throws InterruptedException
  {
    long startTime = System.nanoTime();
    long rem = unit.toNanos(timeout);

    do {
      try {
        process.exitValue();
        return true;
      } catch(IllegalThreadStateException ex) {
        if (rem > 0)
          ThreadUtil.sleep(
            Math.min(TimeUnit.NANOSECONDS.toMillis(rem) + 1, 100));
      }
      rem = unit.toNanos(timeout) - (System.nanoTime() - startTime);
    } while (rem > 0);
    return false;
  }

  private static class SimpleFileTailer {
    private final File file;
    private final List<String> lines;
    private final EvictingQueue<String> history;
    private final RandomAccessFile randomAccessFile;
    private final byte[] inbuf;
    private long position;

    public SimpleFileTailer(File file) {
      this.file = file;
      this.lines = new CopyOnWriteArrayList();
      this.history = EvictingQueue.create(2500);
      this.position = 0;
      this.inbuf = new byte[8192 * 8];
      try {
        this.randomAccessFile = new RandomAccessFile(file, "r");
      } catch (FileNotFoundException e) {
        throw new RuntimeException(Utils.format("Unexpected error reading output file {}: ", file, e), e);
      }
    }

    public void close() {
      IOUtils.closeQuietly(randomAccessFile);
    }

    public List<String> getData() {
      List<String> result = new ArrayList<>();
      try {
        this.position = readLines(randomAccessFile, result);
      } catch (IOException e) {
        throw new RuntimeException(Utils.format("Error reading from {}: {}", file, e, e));
      }
      history.addAll(result);
      return result;
    }

    public List<String> getAllData() {
      List<String> result = new ArrayList<>();
      result.addAll(history);
      return result;
    }

    /**
     * Read new lines.
     *
     * @param reader The file to read
     * @return The new position after the lines have been read
     * @throws java.io.IOException if an I/O error occurs.
     */
    private long readLines(final RandomAccessFile reader, List<String> result) throws IOException {
      ByteArrayOutputStream lineBuf = new ByteArrayOutputStream(64);
      long pos = reader.getFilePointer();
      long rePos = pos; // position to re-read
      int num;
      boolean seenCR = false;
      while (((num = reader.read(inbuf)) != -1)) {
        for (int i = 0; i < num; i++) {
          final byte ch = inbuf[i];
          switch (ch) {
            case '\n':
              seenCR = false; // swallow CR before LF
              result.add(new String(lineBuf.toByteArray(), StandardCharsets.UTF_8));
              lineBuf.reset();
              rePos = pos + i + 1;
              break;
            case '\r':
              if (seenCR) {
                lineBuf.write('\r');
              }
              seenCR = true;
              break;
            default:
              if (seenCR) {
                seenCR = false; // swallow final CR
                result.add(new String(lineBuf.toByteArray(), StandardCharsets.UTF_8));
                lineBuf.reset();
                rePos = pos + i + 1;
              }
              lineBuf.write(ch);
          }
        }
        pos = reader.getFilePointer();
      }
      IOUtils.closeQuietly(lineBuf); // not strictly necessary
      reader.seek(rePos); // Ensure we can re-read if necessary
      return rePos;
    }
  }
}