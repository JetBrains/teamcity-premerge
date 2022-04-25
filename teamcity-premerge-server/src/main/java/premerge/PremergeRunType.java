/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package premerge;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PremergeRunType extends RunType {
  @NotNull
  private final PluginDescriptor myPluginDescriptor;

  public PremergeRunType(@NotNull RunTypeRegistry runTypeRegistry, @NotNull PluginDescriptor pluginDescriptor) {
    runTypeRegistry.registerRunType(this);
    myPluginDescriptor = pluginDescriptor;
  }

  @NotNull
  @Override
  public String getType() {
    return premerge.PremergeConstants.TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return PremergeConstants.DISPLAY_NAME_START;
  }

  @NotNull
  @Override
  public String getDescription() {
    return PremergeConstants.DESCRIPTION;
  }

  @Nullable
  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new PropertiesProcessor() {
      @Override
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        final HashSet<InvalidProperty> invalid = new HashSet<>();
        String gitHubToken = properties.get(PremergeConstants.GITHUB_ACCESS_TOKEN);
        if (StringUtil.isEmpty(gitHubToken)) {
          invalid.add(new InvalidProperty(PremergeConstants.GITHUB_ACCESS_TOKEN, "GitHub Access Token must be specified"));
        }

        return invalid;
      }
    };
  }

  @Nullable
  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myPluginDescriptor.getPluginResourcesPath("premergeParams.jsp");
  }

  @Nullable
  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myPluginDescriptor.getPluginResourcesPath("viewPremergeParams.jsp");
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    return new HashMap<>();
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull Map<String, String> parameters) {
    StringBuilder builder = new StringBuilder();
    builder.append("GitHub Access Token: ").append(parameters.get(PremergeConstants.GITHUB_ACCESS_TOKEN) == null ? "not specified" : "***");
    return builder.toString();
  }
}


