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

import java.util.HashSet;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.buildTriggers.vcs.git.MirrorManager;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.GitAgentSSHService;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.GitMetaFactory;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.PluginConfigFactory;
import jetbrains.buildServer.http.HttpApi;
import org.jetbrains.annotations.NotNull;
import trains.PullRequestsFetcherProvider;

public class PremergeBuildRunner implements AgentBuildRunner, AgentBuildRunnerInfo {
  @NotNull private final GitMetaFactory myGitMetaFactory;
  @NotNull private final GitAgentSSHService mySshService;
  @NotNull private final PluginConfigFactory myConfigFactory;
  @NotNull private final MirrorManager myMirrorManager;
  @NotNull private final HttpApi myHttpApi;
  HashSet<PullRequestsFetcherProvider> myPullRequestFetcherProviders = new HashSet<>();

  public PremergeBuildRunner(@NotNull GitMetaFactory gitMetaFactory,
                             @NotNull GitAgentSSHService sshService,
                             @NotNull PluginConfigFactory configFactory,
                             @NotNull MirrorManager mirrorManager,
                             @NotNull HttpApi httpApi) {
    myGitMetaFactory = gitMetaFactory;
    mySshService = sshService;
    myConfigFactory = configFactory;
    myMirrorManager = mirrorManager;
    myHttpApi = httpApi;
  }

  public void registerPullRequestFetcherProvider(PullRequestsFetcherProvider pullRequestFetcherProvider) {
    myPullRequestFetcherProviders.add(pullRequestFetcherProvider);
  }

  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    String currentRunnerType = context.getRunnerParameters().get("providerType");
    for (PullRequestsFetcherProvider provider : myPullRequestFetcherProviders) {
      if (provider.getType().equals(currentRunnerType)) {
        return new PremergeBuildProcess(myConfigFactory,
                                        mySshService,
                                        myGitMetaFactory,
                                        myMirrorManager,
                                        myHttpApi,
                                        runningBuild,
                                        context,
                                        provider);
      }
    }
    throw new RuntimeException(currentRunnerType + " is unsupported");
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return this;
  }

  @NotNull
  @Override
  public String getType() {
    return premerge.PremergeConstants.TYPE;
  }

  @Override
  public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
    return true;
  }
}
