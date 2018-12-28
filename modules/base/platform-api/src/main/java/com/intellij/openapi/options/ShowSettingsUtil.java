/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.options;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import consulo.ui.RequiredUIAccess;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;

public abstract class ShowSettingsUtil {
  public static ShowSettingsUtil getInstance() {
    return ServiceManager.getService(ShowSettingsUtil.class);
  }

  @RequiredUIAccess
  public abstract void showSettingsDialog(@Nullable Project project);

  @RequiredUIAccess
  public abstract void showSettingsDialog(@Nullable Project project, Class toSelect);

  @RequiredUIAccess
  public abstract void showSettingsDialog(@Nullable Project project, @Nonnull String nameToSelect);

  @RequiredUIAccess
  public abstract void showSettingsDialog(@Nonnull final Project project, final Configurable toSelect);

  @RequiredUIAccess
  public void editConfigurable(Project project, Configurable configurable) {
    editConfigurable(null, project, configurable);
  }

  @RequiredUIAccess
  public abstract void editConfigurable(@Nullable String title, Project project, Configurable configurable);

  @RequiredUIAccess
  public void editConfigurable(Project project, Configurable configurable, Runnable advancedInitialization) {
    editConfigurable(null, project, configurable, advancedInitialization);
  }

  @RequiredUIAccess
  public abstract void editConfigurable(@Nullable String title, Project project, Configurable configurable, Runnable advancedInitialization);

  @RequiredUIAccess
  public abstract void editConfigurable(Component parent, Configurable configurable);

  @RequiredUIAccess
  public abstract void editConfigurable(Component parent, Configurable configurable, Runnable advancedInitialization);

  @RequiredUIAccess
  public void editConfigurable(Project project, @NonNls String dimensionServiceKey, Configurable configurable) {
    editConfigurable(null, project, dimensionServiceKey, configurable);
  }

  @RequiredUIAccess
  public abstract void editConfigurable(@Nullable String title, Project project, @NonNls String dimensionServiceKey, Configurable configurable);

  @RequiredUIAccess
  public abstract void editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable);

  /**
   * @deprecated create a new instance of configurable instead
   */
  public abstract <T extends Configurable> T findProjectConfigurable(Project project, Class<T> confClass);

  /**
   * @deprecated create a new instance of configurable instead
   */
  public abstract <T extends Configurable> T findApplicationConfigurable(Class<T> confClass);

  @Nonnull
  public static String getSettingsMenuName() {
    return SystemInfo.isMac ? "Preferences" : "Settings";
  }
}