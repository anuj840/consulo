/*
 * Copyright 2013-2018 consulo.io
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
package consulo.command.impl;

import com.intellij.history.core.LocalHistoryFacade;
import com.intellij.history.core.changes.Change;
import com.intellij.history.core.changes.ContentChange;
import com.intellij.history.core.changes.StructuralChange;
import com.intellij.history.integration.LocalHistoryImpl;
import com.intellij.openapi.command.impl.FileUndoProvider;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * @author VISTALL
 * @since 2018-08-24
 */
public class ProjectFileUndoProvider extends FileUndoProvider {
  @Inject
  ProjectFileUndoProvider(@Nonnull Project project) {
    super(project);
  }
}
