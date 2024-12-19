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

import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PremergeBranchSupport {
  @NotNull
  public static String cutRefsHeads(@NotNull String branchName) {
    if (branchName.startsWith("refs/heads/")) {
      return branchName.substring("refs/heads/".length());
    }
    return branchName;
  }

  public void fetch(String branch) throws VcsException;
  public void checkout(String branch) throws VcsException;
  public void createBranch(String branch) throws VcsException;
  public void merge(String branch) throws VcsException;
  @NotNull String constructBranchName();
  @Nullable public String getParameter(String parameter) throws VcsException;
}
