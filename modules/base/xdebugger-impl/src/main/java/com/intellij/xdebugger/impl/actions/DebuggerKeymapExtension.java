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
package com.intellij.xdebugger.impl.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.KeyMapBundle;
import com.intellij.openapi.keymap.KeymapExtension;
import com.intellij.openapi.keymap.KeymapGroup;
import com.intellij.openapi.keymap.impl.ui.Group;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author yole
 */
public class DebuggerKeymapExtension implements KeymapExtension {
  public KeymapGroup createGroup(final Condition<AnAction> filtered, final Project project) {
    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup debuggerGroup = (DefaultActionGroup)actionManager.getActionOrStub(IdeActions.GROUP_DEBUGGER);
    AnAction[] debuggerActions = debuggerGroup.getChildActionsOrStubs();

    ArrayList<String> ids = new ArrayList<String>();
    for (AnAction debuggerAction : debuggerActions) {
      String actionId = debuggerAction instanceof ActionStub ? ((ActionStub)debuggerAction).getId() : actionManager.getId(debuggerAction);
      if (filtered == null || filtered.value(debuggerAction)) {
        ids.add(actionId);
      }
    }

    Collections.sort(ids);
    Group group = new Group(KeyMapBundle.message("debugger.actions.group.title"), IdeActions.GROUP_DEBUGGER, null);
    for (String id : ids) {
      group.addActionId(id);
    }

    return group;
  }
}
