// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

import java.awt.*;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class allows to disable (temporarily or permanently) showing certain popups on mouse hover (currently, error/warning descriptions
 * and quick documentation on mouse hover are impacted). If several requests to disable popups have been performed, corresponding number of
 * enabling requests must be performed to turn on hover popups again.
 */
@Singleton
public class EditorMouseHoverPopupControl {
  private static final Logger LOG = Logger.getInstance(EditorMouseHoverPopupControl.class);
  private static final Key<Integer> MOUSE_TRACKING_DISABLED_COUNT = Key.create("MOUSE_TRACKING_DISABLED_COUNT");
  private final Collection<Runnable> ourListeners = new CopyOnWriteArrayList<>();

  public static void disablePopups(@Nonnull Editor editor) {
    setTrackingDisabled(editor, true);
  }

  public static void enablePopups(@Nonnull Editor editor) {
    setTrackingDisabled(editor, false);
  }

  public static void disablePopups(@Nonnull Document document) {
    setTrackingDisabled(document, true);
  }

  public static void enablePopups(@Nonnull Document document) {
    setTrackingDisabled(document, false);
  }

  private static void setTrackingDisabled(@Nonnull UserDataHolder holder, boolean value) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    Integer userData = holder.getUserData(MOUSE_TRACKING_DISABLED_COUNT);
    int count = (userData == null ? 0 : userData) + (value ? 1 : -1);
    if (count < 0) {
      LOG.warn(new IllegalStateException("Editor mouse hover popups weren't disabled previously"));
      count = 0;
    }
    holder.putUserData(MOUSE_TRACKING_DISABLED_COUNT, count == 0 ? null : count);
    if ((userData == null) != (count == 0)) {
      EditorMouseHoverPopupControl instance = getInstance();
      if (instance != null) {
        instance.ourListeners.forEach(Runnable::run);
      }
    }
  }

  public static boolean arePopupsDisabled(@Nonnull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    return editor.getUserData(MOUSE_TRACKING_DISABLED_COUNT) != null ||
           editor.getDocument().getUserData(MOUSE_TRACKING_DISABLED_COUNT) != null ||
           editor.getComponent().getClientProperty(DesktopEditorImpl.IGNORE_MOUSE_TRACKING) != null; /* remove this clause in 2020.1 */
  }

  public static EditorMouseHoverPopupControl getInstance() {
    return ApplicationManager.getApplication().getComponent(EditorMouseHoverPopupControl.class);
  }

  public void addListener(@Nonnull Runnable listener) {
    ourListeners.add(listener);
  }

  public static void disablePopupsWhileShowing(@Nonnull Editor editor, @Nonnull Component popupComponent) {
    new UiNotifyConnector.Once(popupComponent, new Activatable.Adapter() {
      @Override
      public void showNotify() {
        EditorMouseHoverPopupControl.disablePopups(editor);
        new UiNotifyConnector.Once(popupComponent, new Adapter() {
          @Override
          public void hideNotify() {
            EditorMouseHoverPopupControl.enablePopups(editor);
          }
        });
      }
    });
  }
}
