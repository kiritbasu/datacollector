/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.stage.destination.hdfs;

import com.streamsets.pipeline.api.ChooserMode;
import com.streamsets.pipeline.api.ComplexField;
import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.Target;
import com.streamsets.pipeline.api.ValueChooser;
import com.streamsets.pipeline.config.CsvMode;
import com.streamsets.pipeline.config.CsvModeChooserValues;
import com.streamsets.pipeline.config.DataFormat;
import com.streamsets.pipeline.config.TimeZoneChooserValues;
import com.streamsets.pipeline.configurablestage.DTarget;

import java.util.List;
import java.util.Map;

@GenerateResourceBundle
@StageDef(
    version = "1.0.0",
    label = "Hadoop FS",
    description = "Writes to a Hadoop file system",
    icon = "hdfs.png"
)
@ConfigGroups(Groups.class)
public class HdfsDTarget extends DTarget {

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      label = "Hadoop FS URI",
      description = "",
      displayPosition = 10,
      group = "HADOOP_FS"
  )
  public String hdfsUri;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.BOOLEAN,
      label = "Kerberos Authentication",
      defaultValue = "false",
      description = "",
      displayPosition = 20,
      group = "HADOOP_FS"
  )
  public boolean hdfsKerberos;

  @ConfigDef(
      required = false,
      type = ConfigDef.Type.STRING,
      label = "Kerberos principal",
      description = "",
      displayPosition = 30,
      group = "HADOOP_FS",
      dependsOn = "hdfsKerberos",
      triggeredByValue = "true"
  )
  public String kerberosPrincipal;

  @ConfigDef(required = false,
      type = ConfigDef.Type.STRING,
      label = "Kerberos keytab",
      description = "Keytab file path",
      displayPosition = 40,
      group = "HADOOP_FS",
      dependsOn = "hdfsKerberos",
      triggeredByValue = "true"
  )
  public String kerberosKeytab;

  @ConfigDef(
      required = false,
      type = ConfigDef.Type.MAP,
      label = "Hadoop FS Configuration",
      description = "Additional Hadoop properties to pass to the underlying Hadoop FileSystem",
      displayPosition = 50,
      group = "HADOOP_FS"
  )
  public Map<String, String> hdfsConfigs;


  @ConfigDef(
      required = false,
      type = ConfigDef.Type.STRING,
      defaultValue = "",
      label = "Files Prefix",
      description = "File name prefix",
      displayPosition = 105,
      group = "OUTPUT_FILES"
  )
  public String uniquePrefix;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.EL_STRING,
      defaultValue = "/tmp/out/${YYYY}-${MM}-${DD}-${hh}-${mm}-${ss}",
      label = "Directory Template",
      description = "Template for the creation of output directories. Valid variables are ${YYYY}, ${MM}, ${DD}, " +
                    "${hh}, ${mm}, ${ss} and {record:value(“/field”)} for values in a field. Directories are " +
                    "created based on the smallest time unit variable used.",
      displayPosition = 110,
      group = "OUTPUT_FILES"
  )
  public String dirPathTemplate;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "UTC",
      label = "Data Time Zone",
      description = "Time zone to use to resolve directory paths",
      displayPosition = 120,
      group = "OUTPUT_FILES"
  )
  @ValueChooser(type = ChooserMode.PROVIDED, chooserValues = TimeZoneChooserValues.class)
  public String timeZoneID;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.EL_DATE,
      defaultValue = "${time:now()}",
      label = "Time Basis",
      description = "Time basis to use for a record. Enter an expression that evaluates to a datetime. To use the " +
                    "processing time, enter ${time:now()}. To use field values, use '${record:value(\"<filepath>\")}'.",
      displayPosition = 130,
      group = "OUTPUT_FILES"
  )
  public String timeDriver;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.INTEGER,
      defaultValue = "0",
      label = "Max records in a File",
      description = "Number of records that triggers the creation of a new file. Use 0 to opt out.",
      displayPosition = 140,
      group = "OUTPUT_FILES"
  )
  public long maxRecordsPerFile;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.INTEGER,
      defaultValue = "0",
      label = "Max file size (MB)",
      description = "Exceeding this size triggers the creation of a new file. Use 0 to opt out.",
      displayPosition = 150,
      group = "OUTPUT_FILES"
  )
  public long maxFileSize;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "NONE",
      label = "Compression Codec",
      description = "",
      displayPosition = 160,
      group = "OUTPUT_FILES"
  )
  @ValueChooser(type = ChooserMode.SUGGESTED, chooserValues = CompressionChooserValues.class)
  public String compression;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "TEXT",
      label = "File Type",
      description = "",
      displayPosition = 100,
      group = "OUTPUT_FILES"
  )
  @ValueChooser(type = ChooserMode.PROVIDED, chooserValues = FileTypeChooserValues.class)
  public HdfsFileType fileType;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      defaultValue = "${uuid()}",
      label = "Sequence File Key",
      description = "Record key for creating Hadoop sequence files. Valid options are " +
                    "'${record:value(\"<field-path>\")}' or '${uuid()}'",
      displayPosition = 180,
      group = "OUTPUT_FILES",
      dependsOn = "fileType",
      triggeredByValue = "SEQUENCE_FILE"
  )
  public String keyEl;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "BLOCK",
      label = "Compression Type",
      description = "Compression type if using a CompressionCodec",
      displayPosition = 190,
      group = "OUTPUT_FILES",
      dependsOn = "fileType",
      triggeredByValue = "SEQUENCE_FILE"
  )
  @ValueChooser(type = ChooserMode.PROVIDED, chooserValues = HdfsSequenceFileCompressionTypeChooserValues.class)
  public HdfsSequenceFileCompressionType seqFileCompressionType;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.EL_NUMBER,
      defaultValue = "${1 * HOURS}",
      label = "Late Record Time Limit (secs)",
      description = "Time limit (in seconds) for a record to be written to the corresponding HDFS directory, if the " +
                    "limit is exceeded the record will be written to the current late records file. 0 means no limit. " +
                    "If a number is used it is considered seconds, it can be multiplied by 'MINUTES' or 'HOURS', ie: " +
                    "'${30 * MINUTES}'",
      displayPosition = 200,
      group = "LATE_RECORDS"
  )
  public String lateRecordsLimit;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "SEND_TO_ERROR",
      label = "Late Record Handling",
      description = "Action for records considered late.",
      displayPosition = 210,
      group = "LATE_RECORDS"
  )
  @ValueChooser(type = ChooserMode.PROVIDED, chooserValues = LateRecordsActionChooserValues.class)
  public LateRecordsAction lateRecordsAction;

  @ConfigDef(
      required = false,
      type = ConfigDef.Type.EL_STRING,
      defaultValue = "/tmp/late/${YYYY}-${MM}-${DD}",
      label = "Late Record Directory Template",
      description = "Template for the creation of late record directories. Valid variables are ${YYYY}, ${MM}, " +
                    "${DD}, ${hh}, ${mm}, ${ss}.",
      displayPosition = 220,
      group = "LATE_RECORDS",
      dependsOn = "lateRecordsAction",
      triggeredByValue = "SEND_TO_LATE_RECORDS_FILE"
  )
  public String lateRecordsDirPathTemplate;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "SDC_JSON",
      label = "Data Format",
      description = "Data Format",
      displayPosition = 100,
      group = "OUTPUT_FILES",
      dependsOn = "fileType",
      triggeredByValue = { "TEXT", "SEQUENCE_FILE"}
  )
  @ValueChooser(type = ChooserMode.PROVIDED, chooserValues = DataFormatChooserValues.class)
  public DataFormat dataFormat;


  /********  For DELIMITED Content  ***********/

  @ConfigDef(
      required = false,
      type = ConfigDef.Type.MODEL,
      defaultValue = "CSV",
      label = "CSV Format",
      description = "",
      displayPosition = 310,
      group = "DELIMITED",
      dependsOn = "dataFormat",
      triggeredByValue = "DELIMITED"
  )
  @ValueChooser(type = ChooserMode.PROVIDED, chooserValues = CsvModeChooserValues.class)
  public CsvMode csvFileFormat;

  @ConfigDef(
      required = false,
      type = ConfigDef.Type.BOOLEAN,
      defaultValue = "true",
      label = "Remove New Line Characters",
      description = "Replaces new lines characters with white spaces",
      displayPosition = 315,
      group = "DELIMITED",
      dependsOn = "dataFormat",
      triggeredByValue = "DELIMITED"
  )
  public boolean replaceNewLines;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      label = "Field Mapping",
      description = "",
      displayPosition = 320,
      group = "DELIMITED",
      dependsOn = "dataFormat",
      triggeredByValue = "DELIMITED"
  )
  @ComplexField
  public List<FieldPathToNameMappingConfig> cvsFieldPathToNameMappingConfigList;

  @Override
  protected Target createTarget() {
    return new HdfsTarget(
      hdfsUri,
      hdfsKerberos,
      kerberosPrincipal,
      kerberosKeytab,
      hdfsConfigs,
      uniquePrefix,
      dirPathTemplate,
      timeZoneID,
      timeDriver,
      maxRecordsPerFile,
      maxFileSize,
      compression,
      fileType,
      keyEl,
      seqFileCompressionType,
      lateRecordsLimit,
      lateRecordsAction,
      lateRecordsDirPathTemplate,
      dataFormat,
      csvFileFormat,
      replaceNewLines,
      cvsFieldPathToNameMappingConfigList
    );
  }

}