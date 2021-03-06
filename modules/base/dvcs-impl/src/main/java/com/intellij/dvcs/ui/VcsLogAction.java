/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.dvcs.ui;

import com.intellij.dvcs.repo.AbstractRepositoryManager;
import com.intellij.dvcs.repo.Repository;
import com.intellij.dvcs.repo.RepositoryManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLog;
import com.intellij.vcs.log.VcsLogDataKeys;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public abstract class VcsLogAction<Repo extends Repository> extends DumbAwareAction {

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    VcsLog log = e.getRequiredData(VcsLogDataKeys.VCS_LOG);
    List<VcsFullCommitDetails> details = log.getSelectedDetails();
    MultiMap<Repo, VcsFullCommitDetails> grouped = groupByRootWithCheck(project, details);
    assert grouped != null;
    actionPerformed(project, grouped);
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    Project project = e.getProject();
    VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
    if (project == null || log == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    List<VcsFullCommitDetails> details = log.getSelectedDetails();
    MultiMap<Repo, VcsFullCommitDetails> grouped = groupByRootWithCheck(project, details);
    if (grouped == null) {
      e.getPresentation().setEnabledAndVisible(false);
    }
    else {
      e.getPresentation().setVisible(isVisible(project, grouped));
      e.getPresentation().setEnabled(!grouped.isEmpty() && isEnabled(grouped));
    }
  }

  protected abstract void actionPerformed(@Nonnull Project project, @Nonnull MultiMap<Repo, VcsFullCommitDetails> grouped);

  protected abstract boolean isEnabled(@Nonnull MultiMap<Repo, VcsFullCommitDetails> grouped);

  protected boolean isVisible(@Nonnull final Project project, @Nonnull MultiMap<Repo, VcsFullCommitDetails> grouped) {
    return ContainerUtil.and(grouped.keySet(), new Condition<Repo>() {
      @Override
      public boolean value(Repo repo) {
        RepositoryManager<Repo> manager = getRepositoryManager(project);
        return !manager.isExternal(repo);
      }
    });
  }

  @Nonnull
  protected abstract AbstractRepositoryManager<Repo> getRepositoryManager(@Nonnull Project project);

  @Nullable
  protected abstract Repo getRepositoryForRoot(@Nonnull Project project, @Nonnull VirtualFile root);

  @javax.annotation.Nullable
  private MultiMap<Repo, VcsFullCommitDetails> groupByRootWithCheck(@Nonnull Project project, @Nonnull List<VcsFullCommitDetails> commits) {
    MultiMap<Repo, VcsFullCommitDetails> map = MultiMap.create();
    for (VcsFullCommitDetails commit : commits) {
      Repo root = getRepositoryForRoot(project, commit.getRoot());
      if (root == null) { // commit from some other VCS
        return null;
      }
      map.putValue(root, commit);
    }
    return map;
  }

}
