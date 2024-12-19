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

public class PremergeConstants {
  private PremergeConstants() {

  }

  public static final String TYPE = "premergeRunner";
  public static final String DISPLAY_NAME = "Preliminary Merge";
  public static final String DESCRIPTION = "Makes preliminary merge commits with " +
                                           "\"Target branch\" when a source branch is updated (source branch must be set in 'Build condition')";
  public static final String TARGET_BRANCH = "tar.br";
  public static final String PRELIMINARY_MERGE_BRANCH_PREFIX = "premerge";
  public static final String TARGET_BRANCH_SHARED_PARAM = "teamcity.build.premerge.target";
  public static final String TARGET_SHA_SHARED_PARAM = "teamcity.build.premerge.targetSHA";
}
