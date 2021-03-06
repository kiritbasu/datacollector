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
package com.streamsets.datacollector.definition;

import com.google.common.collect.ImmutableList;
import com.streamsets.datacollector.config.StageDefinition;
import com.streamsets.datacollector.config.StageLibraryDefinition;
import com.streamsets.datacollector.config.StageType;
import com.streamsets.datacollector.creation.StageConfigBean;
import com.streamsets.pipeline.api.Batch;
import com.streamsets.pipeline.api.BatchMaker;
import com.streamsets.pipeline.api.Config;
import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ErrorStage;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.HideConfigs;
import com.streamsets.pipeline.api.Label;
import com.streamsets.pipeline.api.RawSource;
import com.streamsets.pipeline.api.RawSourcePreviewer;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.StageUpgrader;
import com.streamsets.pipeline.api.base.BaseSource;
import com.streamsets.pipeline.api.base.BaseTarget;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class TestStageDefinitionExtractor {


  public enum Group1 implements Label {
    G1;

    @Override
    public String getLabel() {
      return "g1";
    }
  }

  public static class Previewer  implements RawSourcePreviewer {
    @Override
    public InputStream preview(int maxLength) {
      return null;
    }

    @Override
    public String getMimeType() {
      return null;
    }

    @Override
    public void setMimeType(String mimeType) {

    }
  }

  @StageDef(version = 1, label = "L", description = "D", icon = "TargetIcon.svg")
  public static class Source1 extends BaseSource {

    @ConfigDef(
        label = "L",
        type = ConfigDef.Type.STRING,
        defaultValue = "X",
        required = true
    )
    public String config1;

    @ConfigDef(
        label = "L",
        type = ConfigDef.Type.STRING,
        defaultValue = "X",
        required = true
    )
    public String config2;

    @Override
    public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
      return null;
    }
  }

  public static class Source2Upgrader implements StageUpgrader {
    @Override
    public List<Config> upgrade(String library, String stageName, String stageInstance, int fromVersion, int toVersion,
        List<Config> configs) throws StageException {
      return configs;
    }
  }

  public enum TwoOutputStreams implements Label {
    OS1, OS2;

    @Override
    public String getLabel() {
      return name();
    }
  }

  @StageDef(version = 2, label = "LL", description = "DD", icon = "TargetIcon.svg",
      execution = ExecutionMode.STANDALONE, outputStreams = TwoOutputStreams.class, recordsByRef = true,
      privateClassLoader = true, upgrader = Source2Upgrader.class)
  @ConfigGroups(Group1.class)
  @RawSource(rawSourcePreviewer = Previewer.class)
  @HideConfigs(value = "config2", preconditions = true, onErrorRecord = true)
  public static class Source2 extends Source1 {

    @ConfigDef(
        label = "L",
        type = ConfigDef.Type.STRING,
        defaultValue = "X",
        required = true
    )
    public String config3;

    @ConfigDef(
        label = "L",
        type = ConfigDef.Type.STRING,
        defaultValue = "X",
        required = true
    )
    public String config4;

    @Override
    public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
      return null;
    }
  }

  @StageDef(version = 1, label = "L", outputStreams = StageDef.VariableOutputStreams.class,
      outputStreamsDrivenByConfig = "config1")
  public static class Source3 extends Source1 {

    @Override
    public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
      return null;
    }
  }

  @StageDef(version = 1, label = "L")
  @HideConfigs(preconditions = true)
  public static class Target1 extends BaseTarget {
    @Override
    public void write(Batch batch) throws StageException {

    }
  }

  @StageDef(version = 1, label = "L")
  public static class Target2 extends BaseTarget {
    @Override
    public void write(Batch batch) throws StageException {

    }
  }

  @StageDef(version = 1, label = "L")
  @ErrorStage
  public static class ToErrorTarget1 extends Target1 {
    @Override
    public void write(Batch batch) throws StageException {

    }
  }

  @StageDef(version = 1, label = "", icon="missing.svg")
  @ErrorStage
  public static class MissingIcon extends BaseTarget {
    @Override
    public void write(Batch batch) throws StageException {

    }
  }

  private static final StageLibraryDefinition MOCK_LIB_DEF =
      new StageLibraryDefinition(TestStageDefinitionExtractor.class.getClassLoader(), "mock", "MOCK", new Properties(),
                                 null, null, null);

  @Test
  public void testExtractSource1() {
    StageDefinition def = StageDefinitionExtractor.get().extract(MOCK_LIB_DEF, Source1.class, "x");
    Assert.assertFalse(def.isPrivateClassLoader());
    Assert.assertEquals(Source1.class.getName(), def.getClassName());
    Assert.assertEquals(StageDefinitionExtractor.getStageName(Source1.class), def.getName());
    Assert.assertEquals(1, def.getVersion());
    Assert.assertEquals("L", def.getLabel());
    Assert.assertEquals("D", def.getDescription());
    Assert.assertEquals(null, def.getRawSourceDefinition());
    Assert.assertEquals(0, def.getConfigGroupDefinition().getGroupNames().size());
    Assert.assertEquals(3, def.getConfigDefinitions().size());
    Assert.assertEquals(1, def.getOutputStreams());
    Assert.assertEquals(2, def.getExecutionModes().size());
    Assert.assertEquals("TargetIcon.svg", def.getIcon());
    Assert.assertEquals(StageDef.DefaultOutputStreams.class.getName(), def.getOutputStreamLabelProviderClass());
    Assert.assertEquals(null, def.getOutputStreamLabels());
    Assert.assertEquals(StageType.SOURCE, def.getType());
    Assert.assertFalse(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_REQUIRED_FIELDS_CONFIG));
    Assert.assertFalse(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_PRECONDITIONS_CONFIG));
    Assert.assertTrue(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_ON_RECORD_ERROR_CONFIG));
    Assert.assertFalse(def.getRecordsByRef());
    Assert.assertTrue(def.getUpgrader() instanceof StageUpgrader.Default);
  }

  @Test
  public void testExtractSource2() {
    StageDefinition def = StageDefinitionExtractor.get().extract(MOCK_LIB_DEF, Source2.class, "x");
    Assert.assertTrue(def.isPrivateClassLoader());
    Assert.assertEquals(Source2.class.getName(), def.getClassName());
    Assert.assertEquals(StageDefinitionExtractor.getStageName(Source2.class), def.getName());
    Assert.assertEquals(2, def.getVersion());
    Assert.assertEquals("LL", def.getLabel());
    Assert.assertEquals("DD", def.getDescription());
    Assert.assertNotNull(def.getRawSourceDefinition());
    Assert.assertEquals(1, def.getConfigGroupDefinition().getGroupNames().size());
    Assert.assertEquals(3, def.getConfigDefinitions().size());
    Assert.assertEquals(2, def.getOutputStreams());
    Assert.assertEquals(1, def.getExecutionModes().size());
    Assert.assertEquals("TargetIcon.svg", def.getIcon());
    Assert.assertEquals(TwoOutputStreams.class.getName(), def.getOutputStreamLabelProviderClass());
    Assert.assertEquals(null, def.getOutputStreamLabels());
    Assert.assertEquals(StageType.SOURCE, def.getType());
    Assert.assertFalse(def.isVariableOutputStreams());
    Assert.assertFalse(def.hasOnRecordError());
    Assert.assertFalse(def.hasPreconditions());
    Assert.assertTrue(def.getUpgrader() instanceof Source2Upgrader);
  }

  @Test
  public void testExtractSource3() {
    StageDefinition def = StageDefinitionExtractor.get().extract(MOCK_LIB_DEF, Source3.class, "x");
    Assert.assertEquals(0, def.getOutputStreams());
    Assert.assertEquals(StageDef.VariableOutputStreams.class.getName(), def.getOutputStreamLabelProviderClass());
    Assert.assertTrue(def.isVariableOutputStreams());
  }

  @Test
  public void testExtractTarget1() {
    StageDefinition def = StageDefinitionExtractor.get().extract(MOCK_LIB_DEF, Target1.class, "x");
    Assert.assertEquals(StageType.TARGET, def.getType());
    Assert.assertEquals(0, def.getOutputStreams());
    Assert.assertEquals(null, def.getOutputStreamLabelProviderClass());
    Assert.assertTrue(def.hasOnRecordError());
    Assert.assertFalse(def.hasPreconditions());
    Assert.assertFalse(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_REQUIRED_FIELDS_CONFIG));
    Assert.assertFalse(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_PRECONDITIONS_CONFIG));
    Assert.assertTrue(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_ON_RECORD_ERROR_CONFIG));
  }

  @Test
  public void testExtractTarget2() {
    StageDefinition def = StageDefinitionExtractor.get().extract(MOCK_LIB_DEF, Target2.class, "x");
    Assert.assertTrue(def.hasOnRecordError());
    Assert.assertTrue(def.hasPreconditions());
    Assert.assertTrue(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_REQUIRED_FIELDS_CONFIG));
    Assert.assertTrue(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_PRECONDITIONS_CONFIG));
    Assert.assertTrue(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_ON_RECORD_ERROR_CONFIG));
  }

  @Test
  public void testExtractToErrorTarget1() {
    StageDefinition def = StageDefinitionExtractor.get().extract(MOCK_LIB_DEF, ToErrorTarget1.class, "x");
    Assert.assertEquals(StageType.TARGET, def.getType());
    Assert.assertEquals(0, def.getOutputStreams());
    Assert.assertEquals(null, def.getOutputStreamLabelProviderClass());
    Assert.assertFalse(def.hasOnRecordError());
    Assert.assertFalse(def.hasPreconditions());
    Assert.assertFalse(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_REQUIRED_FIELDS_CONFIG));
    Assert.assertFalse(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_PRECONDITIONS_CONFIG));
    Assert.assertFalse(def.getConfigDefinitionsMap().containsKey(StageConfigBean.STAGE_ON_RECORD_ERROR_CONFIG));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExtractMissingIcon() {
    StageDefinitionExtractor.get().extract(MOCK_LIB_DEF, MissingIcon.class, "x");
  }

  @Test
  public void testLibraryExecutionOverride() {
    Properties props = new Properties();
    props.put(StageLibraryDefinition.EXECUTION_MODE_PREFIX + Source1.class.getName(), "CLUSTER");
    StageLibraryDefinition libDef = new StageLibraryDefinition(TestStageDefinitionExtractor.class.getClassLoader(),
                                                               "mock", "MOCK", props, null, null, null);

    StageDefinition def = StageDefinitionExtractor.get().extract(libDef, Source1.class, "x");
    Assert.assertEquals(ImmutableList.of(ExecutionMode.CLUSTER),def.getExecutionModes());
  }

}
