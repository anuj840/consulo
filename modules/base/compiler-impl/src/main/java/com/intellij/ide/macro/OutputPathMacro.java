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
package com.intellij.ide.macro;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.compiler.roots.CompilerPathsImpl;
import consulo.roots.impl.ProductionContentFolderTypeProvider;
import consulo.roots.impl.TestContentFolderTypeProvider;

import java.io.File;

public final class OutputPathMacro extends Macro {
  @Override
  public String getName() {
    return "OutputPath";
  }

  @Override
  public String getDescription() {
    return IdeBundle.message("macro.output.path");
  }

  @Override
  @RequiredReadAction
  public String expand(DataContext dataContext) {
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return null;
    }

    VirtualFile file = dataContext.getData(PlatformDataKeys.VIRTUAL_FILE);
    if (file != null) {
      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
      Module module = projectFileIndex.getModuleForFile(file);
      if (module != null) {
        boolean isTest = projectFileIndex.isInTestSourceContent(file);
        String outputPathUrl = ModuleCompilerPathsManager.getInstance(module).getCompilerOutputUrl(isTest ? TestContentFolderTypeProvider.getInstance() : ProductionContentFolderTypeProvider.getInstance());
        if (outputPathUrl == null) {
          return null;
        }
        return VirtualFileManager.extractPath(outputPathUrl).replace('/', File.separatorChar);
      }
    }

    Module[] allModules = ModuleManager.getInstance(project).getSortedModules();
    if (allModules.length == 0) {
      return null;
    }
    String[] paths = CompilerPathsImpl.getOutputPaths(allModules);
    final StringBuilder outputPath = new StringBuilder();
    for (int idx = 0; idx < paths.length; idx++) {
      String path = paths[idx];
      if (idx > 0) {
        outputPath.append(File.pathSeparator);
      }
      outputPath.append(path);
    }
    return outputPath.toString();
  }
}
