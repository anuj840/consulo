/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Bas Leijdekkers
 */
@Singleton
public class WindowDressing implements ProjectComponent {

  private Project myProject;

  @Inject
  public WindowDressing(@Nonnull Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    getWindowActionGroup().addProject(myProject);
  }

  @Override
  public void projectClosed() {
    getWindowActionGroup().removeProject(myProject);
  }

  public static ProjectWindowActionGroup getWindowActionGroup() {
    return (ProjectWindowActionGroup)ActionManager.getInstance().getAction("OpenProjectWindows");
  }
}
