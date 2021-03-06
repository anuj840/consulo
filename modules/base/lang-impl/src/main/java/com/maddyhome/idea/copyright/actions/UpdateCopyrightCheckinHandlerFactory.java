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

/*
* User: anna
* Date: 08-Dec-2008
*/
package com.maddyhome.idea.copyright.actions;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.PairConsumer;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class UpdateCopyrightCheckinHandlerFactory extends CheckinHandlerFactory {
  @Nonnull
  public CheckinHandler createHandler(final CheckinProjectPanel panel, CommitContext commitContext) {
    return new CheckinHandler() {
      @Override
      public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox updateCopyrightCb = new JCheckBox("Update copyright");
        return new RefreshableOnComponent() {
          public JComponent getComponent() {
            final JPanel panel = new JPanel(new BorderLayout());
            panel.add(updateCopyrightCb, BorderLayout.WEST);
            return panel;
          }

          public void refresh() {
          }

          public void saveState() {
            UpdateCopyrightCheckinHandlerState.getInstance(panel.getProject()).UPDATE_COPYRIGHT = updateCopyrightCb.isSelected();
          }

          public void restoreState() {
            updateCopyrightCb.setSelected(UpdateCopyrightCheckinHandlerState.getInstance(panel.getProject()).UPDATE_COPYRIGHT);
          }
        };
      }

      @Override
      public ReturnResult beforeCheckin(@Nullable CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
        if (UpdateCopyrightCheckinHandlerState.getInstance(panel.getProject()).UPDATE_COPYRIGHT) {
          new UpdateCopyrightProcessor(panel.getProject(), null, getPsiFiles()).run();
          FileDocumentManager.getInstance().saveAllDocuments();
        }
        return super.beforeCheckin();
      }

      private PsiFile[] getPsiFiles() {
        final Collection<VirtualFile> files = panel.getVirtualFiles();
        final List<PsiFile> psiFiles = new ArrayList<PsiFile>();
        final PsiManager manager = PsiManager.getInstance(panel.getProject());
        for (final VirtualFile file : files) {
          final PsiFile psiFile = manager.findFile(file);
          if (psiFile != null) {
            psiFiles.add(psiFile);
          }
        }
        return PsiUtilCore.toPsiFileArray(psiFiles);
      }
    };
  }
}