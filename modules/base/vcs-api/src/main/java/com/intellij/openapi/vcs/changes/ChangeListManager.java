/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.openapi.vcs.changes;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.List;

/**
 * @author max
 */
public abstract class ChangeListManager implements ChangeListModification {
  @Nonnull
  public static ChangeListManager getInstance(Project project) {
    return project.getComponent(ChangeListManager.class);
  }

  public abstract void scheduleUpdate();

  public abstract void scheduleUpdate(boolean updateUnversionedFiles);

  public abstract void invokeAfterUpdate(final Runnable afterUpdate, final InvokeAfterUpdateMode mode, final String title, final ModalityState state);

  public abstract void invokeAfterUpdate(final Runnable afterUpdate,
                                         final InvokeAfterUpdateMode mode,
                                         final String title,
                                         final Consumer<VcsDirtyScopeManager> dirtyScopeManager,
                                         final ModalityState state);

  @TestOnly
  public abstract boolean ensureUpToDate(boolean canBeCanceled);

  public abstract int getChangeListsNumber();

  public abstract List<LocalChangeList> getChangeListsCopy();

  @Nonnull
  public abstract List<LocalChangeList> getChangeLists();

  public abstract List<File> getAffectedPaths();

  @Nonnull
  public abstract List<VirtualFile> getAffectedFiles();

  public abstract boolean isFileAffected(final VirtualFile file);

  /**
   * @return all changes in all changelists.
   */
  @Nonnull
  public abstract Collection<Change> getAllChanges();

  @javax.annotation.Nullable
  public abstract LocalChangeList findChangeList(final String name);

  @javax.annotation.Nullable
  public abstract LocalChangeList getChangeList(String id);
//  public abstract LocalChangeList addChangeList(@NotNull String name, final String comment);
//  public abstract void setDefaultChangeList(@NotNull LocalChangeList list);

  /**
   * Returns currently active changelist
   *
   * @return active changelist
   */
  public abstract LocalChangeList getDefaultChangeList();

  public abstract boolean isDefaultChangeList(ChangeList list);

  @Nullable
  public abstract LocalChangeList getChangeList(@Nonnull Change change);

  @Nullable
  public abstract String getChangeListNameIfOnlyOne(Change[] changes);

  @Nonnull
  public abstract Runnable prepareForChangeDeletion(final Collection<Change> changes);

  @javax.annotation.Nullable
  public abstract Change getChange(@Nonnull VirtualFile file);

  @javax.annotation.Nullable
  public abstract LocalChangeList getChangeList(@Nonnull VirtualFile file);

  @javax.annotation.Nullable
  public abstract Change getChange(FilePath file);

  public abstract boolean isUnversioned(VirtualFile file);

  @Nonnull
  public abstract FileStatus getStatus(VirtualFile file);

  @Nonnull
  public abstract Collection<Change> getChangesIn(VirtualFile dir);

  @Nonnull
  public abstract Collection<Change> getChangesIn(FilePath path);

  @javax.annotation.Nullable
  public abstract AbstractVcs getVcsFor(@Nonnull Change change);

//  public abstract void removeChangeList(final LocalChangeList list);

//  public abstract void moveChangesTo(final LocalChangeList list, final Change[] changes);

  public abstract void addChangeListListener(ChangeListListener listener);

  public abstract void removeChangeListListener(ChangeListListener listener);

  public abstract void registerCommitExecutor(CommitExecutor executor);

  public abstract void commitChanges(LocalChangeList changeList, List<Change> changes);

  public abstract void commitChangesSynchronously(LocalChangeList changeList, List<Change> changes);

  /**
   * @return if commit successful
   */
  public abstract boolean commitChangesSynchronouslyWithResult(LocalChangeList changeList, List<Change> changes);

  public abstract void reopenFiles(List<FilePath> paths);

  public abstract List<CommitExecutor> getRegisteredExecutors();

  public abstract void addFilesToIgnore(final IgnoredFileBean... ignoredFiles);

  public abstract void addDirectoryToIgnoreImplicitly(@Nonnull String path);

  public abstract void setFilesToIgnore(final IgnoredFileBean... ignoredFiles);

  public abstract IgnoredFileBean[] getFilesToIgnore();

  public abstract boolean isIgnoredFile(@Nonnull VirtualFile file);

  @javax.annotation.Nullable
  public abstract String getSwitchedBranch(VirtualFile file);

  public abstract String getDefaultListName();

  @Deprecated
  public abstract void letGo();

  public abstract String isFreezed();

  public abstract boolean isFreezedWithNotification(@javax.annotation.Nullable String modalTitle);


  public abstract List<VirtualFile> getModifiedWithoutEditing();

  @Nonnull
  public abstract ThreeState haveChangesUnder(@Nonnull VirtualFile vf);
}
