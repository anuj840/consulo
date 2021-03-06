/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.navigationToolbar;

import com.intellij.ProjectTopics;
import com.intellij.ide.actions.CopyAction;
import com.intellij.ide.actions.CutAction;
import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.AsyncResult;
import consulo.disposer.Disposer;
import com.intellij.openapi.vcs.FileStatusListener;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.problems.ProblemListener;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import com.intellij.ui.ScrollingUtil;
import com.intellij.util.messages.MessageBusConnection;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarListener
  implements ActionListener, ProblemListener, FocusListener, FileStatusListener, AnActionListener, FileEditorManagerListener,
             PsiTreeChangeListener, ModuleRootListener, NavBarModelListener, PropertyChangeListener, KeyListener, WindowFocusListener {
  private static final String LISTENER = "NavBarListener";
  private static final String BUS = "NavBarMessageBus";
  private final NavBarPanel myPanel;
  private boolean shouldFocusEditor = false;

  static void subscribeTo(NavBarPanel panel) {
    if (panel.getClientProperty(LISTENER) != null) {
      unsubscribeFrom(panel);
    }

    final NavBarListener listener = new NavBarListener(panel);
    final Project project = panel.getProject();
    panel.putClientProperty(LISTENER, listener);
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(listener);
    FileStatusManager.getInstance(project).addFileStatusListener(listener);
    PsiManager.getInstance(project).addPsiTreeChangeListener(listener);

    final MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(AnActionListener.TOPIC, listener);
    connection.subscribe(ProblemListener.TOPIC, listener);
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, listener);
    connection.subscribe(NavBarModelListener.NAV_BAR, listener);
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
    panel.putClientProperty(BUS, connection);
    panel.addKeyListener(listener);

    if (panel.isInFloatingMode()) {
      final Window window = SwingUtilities.windowForComponent(panel);
      if (window != null) {
        window.addWindowFocusListener(listener);
      }
    }
  }

  static void unsubscribeFrom(NavBarPanel panel) {
    final NavBarListener listener = (NavBarListener)panel.getClientProperty(LISTENER);
    panel.putClientProperty(LISTENER, null);
    if (listener != null) {
      final Project project = panel.getProject();
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(listener);
      FileStatusManager.getInstance(project).removeFileStatusListener(listener);
      PsiManager.getInstance(project).removePsiTreeChangeListener(listener);
      final MessageBusConnection connection = (MessageBusConnection)panel.getClientProperty(BUS);
      panel.putClientProperty(BUS, null);
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  NavBarListener(NavBarPanel panel) {
    myPanel = panel;
    for (NavBarKeyboardCommand command : NavBarKeyboardCommand.values()) {
      registerKey(command);
    }
    myPanel.addFocusListener(this);
  }

  private void registerKey(NavBarKeyboardCommand cmd) {
    myPanel.registerKeyboardAction(this, cmd.name(), cmd.getKeyStroke(), JComponent.WHEN_FOCUSED);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final NavBarKeyboardCommand cmd = NavBarKeyboardCommand.fromString(e.getActionCommand());
    if (cmd != null) {
      switch (cmd) {
        case LEFT:     myPanel.moveLeft();  break;
        case RIGHT:    myPanel.moveRight(); break;
        case HOME:     myPanel.moveHome();  break;
        case END:      myPanel.moveEnd();   break;
        case DOWN:     myPanel.moveDown();  break;
        case UP:       myPanel.moveDown();  break;
        case ENTER:    myPanel.enter();     break;
        case ESCAPE:   myPanel.escape();    break;
        case NAVIGATE: myPanel.navigate();  break;
      }
    }
  }

  @Override
  public void focusGained(final FocusEvent e) {
    if (e.getOppositeComponent() == null && shouldFocusEditor) {
      shouldFocusEditor = false;
      ToolWindowManager.getInstance(myPanel.getProject()).activateEditorComponent();
      return;
    }
    myPanel.updateItems();
    final List<NavBarItem> items = myPanel.getItems();
    if (!myPanel.isInFloatingMode() && items.size() > 0) {
      myPanel.setContextComponent(items.get(items.size() - 1));
    } else {
      myPanel.setContextComponent(null);
    }
  }

  @Override
  public void focusLost(final FocusEvent e) {
    if (myPanel.getProject().isDisposed()) {
      myPanel.setContextComponent(null);
      myPanel.hideHint();
      return;
    }
    final DialogWrapper dialog = DialogWrapper.findInstance(e.getOppositeComponent());
    shouldFocusEditor =  dialog != null;
    if (dialog != null) {
      Disposer.register(dialog.getDisposable(), new Disposable() {
        @Override
        public void dispose() {
          if (dialog.getExitCode() == DialogWrapper.CANCEL_EXIT_CODE) {
            shouldFocusEditor = false;
          }
        }
      });
    }

    // required invokeLater since in current call sequence KeyboardFocusManager is not initialized yet
    // but future focused component
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        processFocusLost(e);
      }
    });
  }

  private void processFocusLost(FocusEvent e) {
    final Component opposite = e.getOppositeComponent();

    if (myPanel.isInFloatingMode() && opposite != null && DialogWrapper.findInstance(opposite) != null) {
      myPanel.hideHint();
      return;
    }

    final boolean nodePopupInactive = !myPanel.isNodePopupActive();
    boolean childPopupInactive = !JBPopupFactory.getInstance().isChildPopupFocused(myPanel);
    if (nodePopupInactive && childPopupInactive) {
      if (opposite != null && opposite != myPanel && !myPanel.isAncestorOf(opposite) && !e.isTemporary()) {
        myPanel.setContextComponent(null);
        myPanel.hideHint();
      }
    }

    myPanel.updateItems();
  }

  private void rebuildUI() {
    if (myPanel.isShowing()) {
      myPanel.getUpdateQueue().queueRebuildUi();
    }
  }

  private void updateModel() {
    if (myPanel.isShowing()) {
      myPanel.getModel().setChanged(true);
      myPanel.getUpdateQueue().queueModelUpdateFromFocus();
    }
  }

  @Override
  public void fileStatusesChanged() {
    rebuildUI();
  }

  @Override
  public void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
    rebuildUI();
  }

  @Override
  public void childAdded(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void childMoved(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void propertyChanged(@Nonnull final PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void rootsChanged(ModuleRootEvent event) {
    updateModel();
  }

  @Override
  public void problemsAppeared(@Nonnull VirtualFile file) {
    updateModel();
  }

  @Override
  public void problemsDisappeared(@Nonnull VirtualFile file) {
    updateModel();
  }

  @Override
  public void modelChanged() {
    rebuildUI();
  }

  @Override
  public void selectionChanged() {
    myPanel.updateItems();
    myPanel.scrollSelectionToVisible();
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (myPanel.isShowing()) {
      final String name = evt.getPropertyName();
      if ("focusOwner".equals(name) || "permanentFocusOwner".equals(name)) {
        myPanel.getUpdateQueue().restartRebuild();
      }
    }
  }
  @Override
  public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
    if (shouldSkipAction(action)) return;

    if (myPanel.isInFloatingMode()) {
      myPanel.hideHint();
    } else {
      myPanel.cancelPopup();
    }
  }

  private static boolean shouldSkipAction(AnAction action) {
    return action instanceof PopupAction
           || action instanceof CopyAction
           || action instanceof CutAction
           || action instanceof ScrollingUtil.ListScrollAction;
  }

  @Override
  public void keyPressed(final KeyEvent e) {
    if (!(e.isAltDown() || e.isMetaDown() || e.isControlDown() || myPanel.isNodePopupActive())) {
      if (!Character.isLetter(e.getKeyChar())) {
        return;
      }

      final IdeFocusManager focusManager = IdeFocusManager.getInstance(myPanel.getProject());
      final AsyncResult<Void> firstCharTyped = new AsyncResult<>();
      focusManager.typeAheadUntil(firstCharTyped);
      myPanel.moveDown();
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
            final Robot robot = new Robot();
            final boolean shiftOn = e.isShiftDown();
            final int code = e.getKeyCode();
            if (shiftOn) {
              robot.keyPress(KeyEvent.VK_SHIFT);
            }
            robot.keyPress(code);
            robot.keyRelease(code);

            //don't release Shift
            firstCharTyped.setDone();
          }
          catch (AWTException ignored) {
          }
        }
      });
    }
  }

  @Override
  public void fileOpened(@Nonnull final FileEditorManager manager, @Nonnull final VirtualFile file) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myPanel.hasFocus()) {
          manager.openFile(file, true);
        }
      }
    });
  }

  @Override
  public void windowLostFocus(WindowEvent e) {
    final Window window = e.getWindow();
    final Window oppositeWindow = e.getOppositeWindow();
  }

  //---- Ignored
  @Override
  public void windowGainedFocus(WindowEvent e) {
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  @Override
  public void keyReleased(KeyEvent e) {}

  @Override
  public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {}

  @Override
  public void beforeEditorTyping(char c, DataContext dataContext) {}

  @Override
  public void beforeRootsChange(ModuleRootEvent event) {}

  @Override
  public void beforeChildAddition(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildRemoval(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildReplacement(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildMovement(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildrenChange(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforePropertyChange(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void childRemoved(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {}

  @Override
  public void selectionChanged(@Nonnull FileEditorManagerEvent event) {}
}
