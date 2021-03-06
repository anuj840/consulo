// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.config.IntentionSettingsConfigurable;
import com.intellij.codeInsight.intention.impl.config.IntentionsConfigurable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import javax.annotation.Nonnull;

import javax.swing.*;

public class EditIntentionSettingsAction extends AbstractEditIntentionSettingsAction implements HighPriorityAction {
  public EditIntentionSettingsAction(IntentionAction action) {
    super(action);
  }

  @Nonnull
  @Override
  public String getText() {
    return "Edit intention settings";
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final IntentionsConfigurable configurable = new IntentionSettingsConfigurable();
    ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> SwingUtilities.invokeLater(() -> configurable.selectIntention(myFamilyName)));
  }
}
