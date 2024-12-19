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

package jetbrains.buildServer.premerge;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.oauth.AgentTokenStorage;
import jetbrains.buildServer.buildTriggers.vcs.git.MirrorManager;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.GitAgentSSHService;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.GitMetaFactory;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.PluginConfigFactory;
import jetbrains.buildServer.ssh.SshKnownHostsManager;
import org.jetbrains.annotations.NotNull;

public class PremergeBuildRunner implements AgentBuildRunner, AgentBuildRunnerInfo {
  @NotNull private final GitMetaFactory myGitMetaFactory;
  @NotNull private final GitAgentSSHService mySshService;
  @NotNull private final PluginConfigFactory myConfigFactory;
  @NotNull private final MirrorManager myMirrorManager;
  @NotNull private final AgentTokenStorage myTokenStorage;
  @NotNull private final SshKnownHostsManager mySshKnownHostsManager;

  public PremergeBuildRunner(@NotNull GitMetaFactory gitMetaFactory,
                             @NotNull GitAgentSSHService sshService,
                             @NotNull PluginConfigFactory configFactory,
                             @NotNull MirrorManager mirrorManager,
                             @NotNull AgentTokenStorage tokenStorage,
                             @NotNull SshKnownHostsManager sshKnownHostsManager) {
    myGitMetaFactory = gitMetaFactory;
    mySshService = sshService;
    myConfigFactory = configFactory;
    myMirrorManager = mirrorManager;
    myTokenStorage = tokenStorage;
    mySshKnownHostsManager = sshKnownHostsManager;
  }

  @NotNull
  @Override
  public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    return new PremergeBuildProcess(myConfigFactory, mySshService, myGitMetaFactory, myMirrorManager, runningBuild, context, myTokenStorage, mySshKnownHostsManager);
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return this;
  }

  @NotNull
  @Override
  public String getType() {
    return PremergeConstants.TYPE;
  }

  @Override
  public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
    return true;
  }
}
