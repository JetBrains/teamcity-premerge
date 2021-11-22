package premerge;

import java.io.File;
import jetbrains.buildServer.buildTriggers.vcs.git.AuthSettings;
import jetbrains.buildServer.buildTriggers.vcs.git.GitVersion;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.*;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.command.MergeCommand;
import jetbrains.buildServer.buildTriggers.vcs.git.agent.command.SetConfigCommand;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PremergeBranchSupportImpl implements PremergeBranchSupport {
  @NotNull private final AgentGitFacade myFacade;
  @NotNull private final VcsRoot myRoot;
  @NotNull private final AgentPluginConfig myConfig;
  @NotNull protected final PremergeBuildProcess myProcess;
  @NotNull private final AgentGitVcsRoot myVcsRoot;

  public PremergeBranchSupportImpl(@NotNull PremergeBuildProcess process,
                                   @NotNull VcsRoot root,
                                   @NotNull String repoRelativePath) throws VcsException {
    myRoot = root;
    myProcess = process;
    myConfig = createPluginConfig();
    myVcsRoot = createGitVcsRoot(root);
    myFacade = getFacade(repoRelativePath);
  }

  protected AgentPluginConfig createPluginConfig() throws VcsException {
    return myProcess.getConfigFactory().createConfig(myProcess.getBuild(), myRoot);
  }

  protected AgentGitVcsRoot createGitVcsRoot(VcsRoot root) throws VcsException {
    return new AgentGitVcsRoot(myProcess.getMirrorManager(), myProcess.getBuild().getCheckoutDirectory(), root);
  }

  protected AgentGitFacade getFacade() {
    return getFacade("");
  }

  protected AgentGitFacade getFacade(String repoRelativePath) {
    GitFactory gitFactory = myProcess.getGitMetaFactory().createFactory(myProcess.getSshService(),
                                                                        new BuildContext(myProcess.getBuild(), myConfig));
    return gitFactory.create(new File(myProcess.getBuild().getCheckoutDirectory().getAbsolutePath() + "/" + repoRelativePath));
  }

  protected AuthSettings retrieveAuthSettings() {
    return myVcsRoot.getAuthSettings();
  }

  @Override
  @NotNull
  public String constructBranchName() {
    return PremergeConstants.PRELIMINARY_MERGE_BRANCH_PREFIX + "/" + myProcess.getBuild().getBuildId();
  }

  @Override
  public void fetch(String branch) throws VcsException {
    try {
      myFacade.fetch()
              .setAuthSettings(retrieveAuthSettings())
              .setUseNativeSsh(myConfig.isUseNativeSSH())
              .setTimeout(getTimeout())
              .setRefspec("+" + branch + ":" + branch)
              .setFetchTags(myConfig.isFetchTags())
              .setQuite(true)
              .call();
    } catch (Exception e) {
      myProcess.getBuild().getBuildLogger().warning("Fetching '" + branch + "' error");
      throw new VcsException(e);
    }
    myProcess.getBuild().getBuildLogger().message("'" + branch + "' fetched");
  }

  @Override
  public void checkout(String branch) throws VcsException {
    try {
      myFacade.checkout()
              .setAuthSettings(retrieveAuthSettings())
              .setUseNativeSsh(myConfig.isUseNativeSSH())
              .setBranch(branch)
              .setTimeout(myConfig.getCheckoutIdleTimeoutSeconds())
              .call();
    } catch (Exception e) {
      myProcess.getBuild().getBuildLogger().error("Checkout to '" + branch + "' error");
      throw new VcsException(e);
    }
    myProcess.getBuild().getBuildLogger().message("Checkout to '" + branch + "'");
  }

  @Override
  public void createBranch(String branch) throws VcsException {
    try {
      myFacade.createBranch()
              .setName(branch)
              .call();
    } catch (Exception e) {
      myProcess.getBuild().getBuildLogger().error("Creating '" + branch + "' error");
      throw new VcsException(e);
    }
    myProcess.getBuild().getBuildLogger().message("Created '" + branch + "'");
  }

  @Override
  public void merge(String branch) throws VcsException {
    try {
      MergeCommand mergeCommand = myFacade.merge();
      mergeCommand.setEnv("author.name", "Premerge Robot");
      mergeCommand.setEnv("author.email", "premerge.plugin@jetbrains.com");
      mergeCommand.setBranches(branch)
                  .setQuiet(true)
                  .call();
      myProcess.setSuccess();
    } catch (VcsException vcsException) {
      String mergeCommits = getParameter("MERGE_HEAD");
      if (!StringUtil.isEmpty(mergeCommits)) {
        myProcess.getBuild().getBuildLogger().warning("Preliminary merge conflict with branch '" + branch + "'");
        mergeAbort();
      }
      throw vcsException;
    } catch (Exception e) {
      myProcess.getBuild().getBuildLogger().error("Merging '" + branch +"' error");
      throw new VcsException(e);
    }
    myProcess.getBuild().getBuildLogger().message("'" + branch + "' was merged");
  }

  public void mergeAbort() throws VcsException {
    try {
      myFacade.merge()
              .setAbort(true)
              .call();
    } catch (Exception e) {
      myProcess.getBuild().getBuildLogger().error("Merge abort error");
      throw new VcsException(e);
    }
  }

  @Nullable
  @Override
  public String getParameter(String parameter) throws VcsException {
    return myFacade.revParse()
            .verify(parameter)
            .call();
  }

  @Override
  public void setUser() throws VcsException {
    SetConfigCommand setConfig = myFacade.setConfig();
    setConfig.addConfig("user.name", "premergeRobot");
    setConfig.addConfig("user.email", "premerge@jetbrains.com");
    setConfig.call();
  }

  public int getTimeout() {
    int timeout = myConfig.getIdleTimeoutSeconds();
    GitVersion version = myConfig.getGitVersion();
    if(version.isLessThan(new GitVersion(1, 7, 1, 0))) {
      timeout = 24 * 60 * 60; //24 hours
    }
    return timeout;
  }
}
