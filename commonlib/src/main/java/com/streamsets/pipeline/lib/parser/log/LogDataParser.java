/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.lib.parser.log;

import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.lib.io.OverrunReader;
import com.streamsets.pipeline.lib.parser.DataParser;
import com.streamsets.pipeline.lib.parser.DataParserException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class LogDataParser implements DataParser {

  static final String TEXT_FIELD_NAME = "originalLine";
  static final String TRUNCATED_FIELD_NAME = "truncated";

  private final Stage.Context context;
  private final String readerId;
  private final OverrunReader reader;
  private final int maxObjectLen;
  private final StringBuilder sb;
  private final StringBuilder previousLine;
  private final boolean retainOriginalText;
  private final Map<String, Field> fieldsFromPrevLine;
  private final int maxStackTraceLines;
  private int previousRead;
  private long currentOffset;

  public LogDataParser(Stage.Context context, String readerId, OverrunReader reader, long readerOffset,
                        int maxObjectLen, boolean retainOriginalText, int maxStackTraceLines) throws IOException {
    this.context = context;
    this.readerId = readerId;
    this.reader = reader;
    this.maxObjectLen = maxObjectLen;
    this.retainOriginalText = retainOriginalText;
    reader.setEnabled(false);
    IOUtils.skipFully(reader, readerOffset);
    reader.setEnabled(true);
    sb = new StringBuilder(maxObjectLen > 0 ? maxObjectLen : 1024);
    previousLine = new StringBuilder(maxObjectLen > 0 ? maxObjectLen : 1024);
    fieldsFromPrevLine = new LinkedHashMap<>();
    currentOffset = readerOffset;
    this.maxStackTraceLines = maxStackTraceLines;
  }

  private boolean isOverMaxObjectLen(int len) {
    return maxObjectLen > -1 && len > maxObjectLen;
  }

  @Override
  public Record parse() throws IOException, DataParserException {
    //In order to detect stack trace / multi line error messages, the parser reads the next line and attempts
    //a pattern match. If it fails then the line is treated a a stack trace and associated with the previous line.
    //If the pattern matches then its a valid log line and is saved for the next round.

    Record record = null;

    //Check if EOF encountered in the previous round
    if(previousLine.length() == 0 && previousRead == -1) {
      //EOF encountered previous round, return null
      currentOffset = -1;
      return record;
    }

    //Check if a line was read and saved from the previous round
    if(previousLine.length() > 0) {
      record = createRecordFromPreviousLine();
      //update the current offset. This is what gets returned by the produce API.
      currentOffset = reader.getPos();
      //check if the EOF was reached in the previous read and update the offset accordingly
      if(previousRead == -1) {
        currentOffset = -1;
      }
    }

    //read the next line
    sb.setLength(0);
    Map<String, Field> fieldsFromLogLine = new LinkedHashMap<>();
    StringBuilder stackTrace = new StringBuilder();
    int read = readAhead(fieldsFromLogLine, stackTrace);

    //Use the data from the read line if there is no saved data from the previous round.
    if(record == null && !fieldsFromLogLine.isEmpty()) {
      record = context.createRecord(readerId + "::" + currentOffset);
      //create field for the record
      Map<String, Field> map = new HashMap<>();
      if (retainOriginalText) {
        map.put(TEXT_FIELD_NAME, Field.create(sb.toString()));
        if (isOverMaxObjectLen(read)) {
          map.put(TRUNCATED_FIELD_NAME, Field.create(true));
        }
      }
      map.putAll(fieldsFromLogLine);
      record.set(Field.create(map));
      //Since there was no previously saved line, the current offset must be updated to the current reader position
      currentOffset = reader.getPos();
      if(read == -1) {
        currentOffset = -1;
      }

      //store already read line for the next iteration
      fieldsFromPrevLine.clear();
      previousLine.setLength(0);
      fieldsFromPrevLine.putAll(fieldsFromLogLine);
      previousLine.append(sb.toString());
      previousRead = read;

      //read ahead since there was no line from the previous round
      sb.setLength(0);
      fieldsFromLogLine.clear();
      stackTrace.setLength(0);
      read = readAhead(fieldsFromLogLine, stackTrace);
    }

    //check if a stack trace was found during read ahead
    if(stackTrace.length() > 0) {
      //associate it with the last field in the previously read line
      Field messageField = record.get("/" + Constants.MESSAGE);
      if(messageField != null) {
        messageField.set(Field.Type.STRING, messageField.getValueAsString() + "\n" + stackTrace.toString());
      }
      //update the originalLine if required
      if(record.has("/" + TEXT_FIELD_NAME)) {
        Field originalLine = record.get("/" + TEXT_FIELD_NAME);
        originalLine.set(Field.Type.STRING, originalLine.getValueAsString() + "\n" + stackTrace.toString());
      }
      //if EOF was reached while reading the stack trace, update the current offset
      if(read == -1) {
        currentOffset = -1;
      }
    }

    //store already read line for the next iteration
    fieldsFromPrevLine.clear();
    previousLine.setLength(0);
    fieldsFromPrevLine.putAll(fieldsFromLogLine);
    previousLine.append(sb.toString());
    previousRead = read;
    return record;
  }

  private Record createRecordFromPreviousLine() {
    //We already have a log line from the previous round. Use it in the record that will be produced this round.
    Record record = context.createRecord(readerId + "::" + currentOffset);
    Map<String, Field> map = new HashMap<>();
    if (retainOriginalText) {
      map.put(TEXT_FIELD_NAME, Field.create(sb.toString()));
      if (isOverMaxObjectLen(previousRead)) {
        map.put(TRUNCATED_FIELD_NAME, Field.create(true));
      }
    }
    map.putAll(fieldsFromPrevLine);
    record.set(Field.create(map));
    return record;
  }


  /*
   Captures valid lines in "sb" and corresponding fields in "fieldsFromLogLine".
   Captures stack traces if any in the argument "stackTrace"
   */
  private int readAhead(Map<String, Field> fieldsFromLogLine, StringBuilder stackTrace)
    throws DataParserException, IOException {
    StringBuilder multilineLog = new StringBuilder();
    int read = readLine(multilineLog);
    int numberOfLinesRead = 0;
    while (read > -1) {
      try {
        Map<String, Field> stringFieldMap = parseLogLine(multilineLog);
        fieldsFromLogLine.putAll(stringFieldMap);
        sb.append(multilineLog);
        //If the line can be parsed successfully, do not read further
        //This line will be used in the current record if this is the first line being read
        //or stored for the next round if there is a line from the previous round.
        break;
      } catch (DataParserException e) {
        //is this the first line being read? Yes -> throw exception
        if (previousLine.length() == 0 || maxStackTraceLines == -1) {
          throw new DataParserException(Errors.LOG_PARSER_01, multilineLog.toString(), e.getMessage(), e);
        }
        //otherwise read until we get a line that matches pattern
        if(numberOfLinesRead < maxStackTraceLines) {
          if(numberOfLinesRead != 0) {
            stackTrace.append("\n");
          }
          stackTrace.append(multilineLog.toString());
        }
        numberOfLinesRead++;
        multilineLog.setLength(0);
        read = readLine(multilineLog);
      }
    }
    return read;
  }


  protected abstract Map<String, Field> parseLogLine(StringBuilder sb) throws DataParserException;

  @Override
  public long getOffset() {
    return currentOffset;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  // returns the reader line length, the StringBuilder has up to maxObjectLen chars
  int readLine(StringBuilder sb) throws IOException {
    int c = reader.read();
    int count = (c == -1) ? -1 : 0;
    while (c > -1 && !isOverMaxObjectLen(count) && !checkEolAndAdjust(c)) {
      count++;
      sb.append((char) c);
      c = reader.read();
    }
    if (isOverMaxObjectLen(count)) {
      sb.setLength(sb.length() - 1);
      while (c > -1 && c != '\n' && c != '\r') {
        count++;
        c = reader.read();
      }
      checkEolAndAdjust(c);
    }
    return count;
  }

  boolean checkEolAndAdjust(int c) throws IOException {
    boolean eol = false;
    if (c == '\n') {
      eol = true;
    } else if (c == '\r') {
      eol = true;
      reader.mark(1);
      c = reader.read();
      if (c != '\n') {
        reader.reset();
      }
    }
    return eol;
  }


}