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
package com.intellij.dvcs.push.ui;

import com.intellij.dvcs.push.*;
import com.intellij.dvcs.repo.Repository;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.OptionAction;
import com.intellij.openapi.ui.ValidationInfo;
import net.miginfocom.swing.MigLayout;
import javax.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VcsPushDialog extends DialogWrapper {

  private static final String ID = "Vcs.Push.Dialog";

  private final PushLog myListPanel;
  private final PushController myController;
  private final Map<PushSupport, VcsPushOptionsPanel> myAdditionalPanels;

  private Action myPushAction;
  @javax.annotation.Nullable
  private ForcePushAction myForcePushAction;

  public VcsPushDialog(@Nonnull Project project,
                       @Nonnull List<? extends Repository> selectedRepositories,
                       @javax.annotation.Nullable Repository currentRepo) {
    super(project);
    myController = new PushController(project, this, selectedRepositories, currentRepo);
    myAdditionalPanels = myController.createAdditionalPanels();
    myListPanel = myController.getPushPanelLog();

    init();
    updateOkActions();
    setOKButtonText("Push");
    setOKButtonMnemonic('P');
    setTitle("Push Commits");
  }

  @Override
  protected JComponent createCenterPanel() {
    JComponent rootPanel = new JPanel(new BorderLayout(0, 15));
    rootPanel.add(myListPanel, BorderLayout.CENTER);
    JPanel optionsPanel = new JPanel(new MigLayout("ins 0 0, flowx"));
    for (VcsPushOptionsPanel panel : myAdditionalPanels.values()) {
      optionsPanel.add(panel);
    }
    rootPanel.add(optionsPanel, BorderLayout.SOUTH);
    return rootPanel;
  }

  @Override
  protected String getDimensionServiceKey() {
    return ID;
  }

  @javax.annotation.Nullable
  @Override
  protected ValidationInfo doValidate() {
    updateOkActions();
    return null;
  }

  @Override
  protected boolean postponeValidation() {
    return false;
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    final List<Action> actions = new ArrayList<Action>();
    myForcePushAction = new ForcePushAction();
    myForcePushAction.setEnabled(canForcePush());
    myPushAction = new ComplexPushAction(myForcePushAction);
    myPushAction.putValue(DEFAULT_ACTION, Boolean.TRUE);
    actions.add(myPushAction);
    actions.add(getCancelAction());
    actions.add(getHelpAction());
    return actions.toArray(new Action[actions.size()]);
  }

  private boolean canPush() {
    return myController.isPushAllowed(false);
  }

  private boolean canForcePush() {
    return myController.isForcePushEnabled() && myController.getProhibitedTarget() == null && myController.isPushAllowed(true);
  }

  @javax.annotation.Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myListPanel.getPreferredFocusedComponent();
  }

  @Nonnull
  @Override
  protected Action getOKAction() {
    return myPushAction;
  }

  @Override
  protected String getHelpId() {
    return ID;
  }

  public void updateOkActions() {
    myPushAction.setEnabled(canPush());
    if (myForcePushAction != null) {
      boolean canForcePush = canForcePush();
      myForcePushAction.setEnabled(canForcePush);
      String tooltip = null;
      if (!canForcePush) {
        PushTarget target = myController.getProhibitedTarget();
        tooltip = myController.isForcePushEnabled() && target != null
                  ? "Force push to <b>" + target.getPresentation() + "</b> is prohibited"
                  : "<b>Force Push</b> can be enabled in the Settings";
      }
      myForcePushAction.putValue(Action.SHORT_DESCRIPTION, tooltip);
    }
  }

  public void disableOkActions() {
    myPushAction.setEnabled(false);
  }

  @javax.annotation.Nullable
  public VcsPushOptionValue getAdditionalOptionValue(@Nonnull PushSupport support) {
    VcsPushOptionsPanel panel = myAdditionalPanels.get(support);
    return panel == null ? null : panel.getValue();
  }

  private class ForcePushAction extends AbstractAction {
    ForcePushAction() {
      super("&Force Push");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (myController.ensureForcePushIsNeeded()) {
        myController.push(true);
        close(OK_EXIT_CODE);
      }
    }
  }

  private class ComplexPushAction extends AbstractAction implements OptionAction {
    private final Action[] myOptions;

    private ComplexPushAction(Action additionalAction) {
      super("&Push");
      myOptions = new Action[]{additionalAction};
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      myController.push(false);
      close(OK_EXIT_CODE);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
      super.setEnabled(isEnabled);
      for (Action optionAction : myOptions) {
        optionAction.setEnabled(isEnabled);
      }
    }

    @Nonnull
    @Override
    public Action[] getOptions() {
      return myOptions;
    }
  }
}
