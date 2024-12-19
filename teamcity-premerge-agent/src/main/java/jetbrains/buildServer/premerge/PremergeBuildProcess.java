/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.oauth.AgentTokenStorage;
import jetbrains.buildServer.buildTriggers.vcs.git.GitUtils;
import jetbrains.buildServer.buildTriggers.vcs.git.MirrorManager;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.*;
import jetbrains.buildServer.ssh.SshKnownHostsManager;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.vcs.VcsRootEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PremergeBuildProcess extends BuildProcessAdapter {
  @NotNull private final PluginConfigFactory myConfigFactory;
  @NotNull private final GitAgentSSHService mySshService;
  @NotNull private final GitMetaFactory myGitMetaFactory;
  @NotNull private final MirrorManager myMirrorManager;
  @NotNull private final AgentRunningBuild myBuild;
  @NotNull private final BuildRunnerContext myRunner;
  @NotNull private final AgentTokenStorage myTokenStorage;
  @NotNull private final SshKnownHostsManager mySshKnownHostsManager;
  private String targetBranch;
  private final Map<String, String> targetSHAs = new HashMap<>();
  private ResultStatus status = ResultStatus.SKIPPED;
  private int unsuccessfulFetchesCount = 0;

  public enum ResultStatus {SUCCESS, SKIPPED, FAILED}

  public PremergeBuildProcess(@NotNull PluginConfigFactory configFactory,
                              @NotNull GitAgentSSHService sshService,
                              @NotNull GitMetaFactory gitMetaFactory,
                              @NotNull MirrorManager mirrorManager,
                              @NotNull AgentRunningBuild build,
                              @NotNull BuildRunnerContext runner,
                              @NotNull AgentTokenStorage tokenStorage,
                              @NotNull SshKnownHostsManager sshKnownHostsManager) {
    myConfigFactory = configFactory;
    mySshService = sshService;
    myGitMetaFactory = gitMetaFactory;
    myMirrorManager = mirrorManager;
    myBuild = build;
    myRunner = runner;
    myTokenStorage = tokenStorage;
    mySshKnownHostsManager = sshKnownHostsManager;
  }

  @Override
  public void start() {
    myBuild.getBuildLogger().message("Preliminary merge build step:");
    if (myBuild.getEffectiveCheckoutMode() != AgentCheckoutMode.ON_AGENT) {
      getBuild().getBuildLogger().error("Wrong checkout mode. This build step works only with agent-side checkout");
      setUnsuccess();
      return;
    }

    try {
      preliminaryMerge();
    }
    catch (VcsException vcsException) {
      myBuild.getBuildLogger().error("Error while build step execution");
    }
  }

  protected void preliminaryMerge() throws VcsException {
    targetBranch = PremergeBranchSupport.cutRefsHeads(myRunner.getRunnerParameters().get(PremergeConstants.TARGET_BRANCH));
    List<VcsRootEntry> vcsRootEntries = myBuild.getVcsRootEntries();
    for (VcsRootEntry entry : vcsRootEntries) {
      makeVcsRootPreliminaryMerge(entry.getVcsRoot(), entry.getCheckoutRules().map("."));
    }

    if (unsuccessfulFetchesCount == vcsRootEntries.size()) {
      getBuild().getBuildLogger().error("Fetching all target branches error");
      setUnsuccess();
      throw new VcsException("Fetching all target branches error");
    }
  }

  protected void makeVcsRootPreliminaryMerge(VcsRoot root, String repoRelativePath) throws VcsException {
    PremergeBranchSupport branchSupport = createPremergeBranchSupport(root, repoRelativePath);

    String premergeBranch = branchSupport.constructBranchName();
    getBuild().getBuildLogger().message("> " + root.getName());

    String rootBranch = getBuild().getSharedConfigParameters().get(GitUtils.getGitRootBranchParamName(root));
    if (PremergeBranchSupport.cutRefsHeads(rootBranch).equals(targetBranch)) {
      getBuild().getBuildLogger().warning("Current branch is the same as the target branch. Skipping VcsRoot.");
    }
    else {
      try {
        branchSupport.fetch(targetBranch);
      } catch (VcsException e) {
        unsuccessfulFetchesCount++;
        return;
      }

      try {
        branchSupport.createBranch(premergeBranch);
        branchSupport.checkout(premergeBranch);
        branchSupport.merge(targetBranch);
        targetSHAs.put(root.getExternalId(), branchSupport.getParameter(targetBranch));
      } catch (VcsException ex) {
        setUnsuccess();
        throw ex;
      }
    }
  }

  protected PremergeBranchSupport createPremergeBranchSupport(VcsRoot root, String repoRelativePath) throws VcsException {
    return new PremergeBranchSupportImpl(this, root, repoRelativePath, myTokenStorage, myBuild, mySshKnownHostsManager);
  }

  @NotNull
  @Override
  public BuildFinishedStatus waitFor() {
    if (getStatus() == ResultStatus.SUCCESS) {
      assert targetBranch != null;
      myBuild.addSharedConfigParameter(PremergeConstants.TARGET_BRANCH_SHARED_PARAM, targetBranch);
      targetSHAs.forEach((name, sha) -> myBuild.addSharedConfigParameter(PremergeConstants.TARGET_SHA_SHARED_PARAM + "." + name, sha));
      return BuildFinishedStatus.FINISHED_SUCCESS;
    }
    else {
      return BuildFinishedStatus.FINISHED_FAILED;
    }
  }

  @NotNull
  public PluginConfigFactory getConfigFactory() {
    return myConfigFactory;
  }

  @NotNull
  public GitAgentSSHService getSshService() {
    return mySshService;
  }

  @NotNull
  public GitMetaFactory getGitMetaFactory() {
    return myGitMetaFactory;
  }

  @NotNull
  public MirrorManager getMirrorManager() {
    return myMirrorManager;
  }

  @NotNull
  public AgentRunningBuild getBuild() {
    return myBuild;
  }

  @NotNull
  public BuildRunnerContext getRunner() {
    return myRunner;
  }

  public ResultStatus getStatus() {
    return status;
  }

  void setSuccess() {
    if (status != ResultStatus.FAILED) {
      status = ResultStatus.SUCCESS;
    }
  }

  void setUnsuccess() {
    status = ResultStatus.FAILED;
  }
}
