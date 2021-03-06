/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.vcs.log.data;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.BiDirectionalEnumerator;
import com.intellij.vcs.log.CommitId;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsRef;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.intellij.util.containers.ContainerUtil.canonicalStrategy;

public class InMemoryStorage implements VcsLogStorage {
  private final BiDirectionalEnumerator<CommitId> myCommitIdEnumerator = new BiDirectionalEnumerator<>(1, canonicalStrategy());
  private final BiDirectionalEnumerator<VcsRef> myRefsEnumerator = new BiDirectionalEnumerator<>(1, canonicalStrategy());

  @Override
  public int getCommitIndex(@Nonnull Hash hash, @Nonnull VirtualFile root) {
    return getOrPut(hash, root);
  }

  private int getOrPut(@Nonnull Hash hash, @Nonnull VirtualFile root) {
    return myCommitIdEnumerator.enumerate(new CommitId(hash, root));
  }

  @Nonnull
  @Override
  public CommitId getCommitId(int commitIndex) {
    return myCommitIdEnumerator.getValue(commitIndex);
  }

  @Nullable
  @Override
  public CommitId findCommitId(@Nonnull final Condition<CommitId> condition) {
    final CommitId[] result = new CommitId[]{null};
    myCommitIdEnumerator.forEachValue(commitId -> {
      if (condition.value(commitId)) {
        result[0] = commitId;
        return false;
      }
      return true;
    });
    return result[0];
  }

  @Override
  public int getRefIndex(@Nonnull VcsRef ref) {
    return myRefsEnumerator.enumerate(ref);
  }

  @Nullable
  @Override
  public VcsRef getVcsRef(int refIndex) {
    return myRefsEnumerator.getValue(refIndex);
  }

  @Override
  public void flush() {
    // nothing to flush
  }
}
