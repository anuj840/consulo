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
package com.intellij.openapi.command.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import consulo.platform.Platform;

import java.awt.*;

/**
 * @author max
 */
public class FocusBasedCurrentEditorProvider implements CurrentEditorProvider {
  public FileEditor getCurrentEditor() {
    if(Platform.current().isWebService()) {
      return null;
    }
    // [kirillk] this is a hack, since much of editor-related code was written long before
    // own focus managenent in the platform, so this method should be strictly synchronous
    final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return DataManager.getInstance().getDataContext(owner).getData(PlatformDataKeys.FILE_EDITOR);
  }
}