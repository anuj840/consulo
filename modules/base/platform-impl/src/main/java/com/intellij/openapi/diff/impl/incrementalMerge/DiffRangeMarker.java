/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.diff.impl.incrementalMerge;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.Map;

class DiffRangeMarker implements RangeMarker {
  private final RangeMarker myRangeMarker;

  DiffRangeMarker(@Nonnull Document document, @Nonnull TextRange range, RangeInvalidListener listener) {
    myRangeMarker = document.createRangeMarker(range.getStartOffset(), range.getEndOffset());
    if (listener != null) {
      InvalidRangeDispatcher.addClient(document, this, listener);
    }
  }

  public void removeListener(@Nonnull RangeInvalidListener listener) {
    InvalidRangeDispatcher.removeClient(getDocument(), this, listener);
  }

  interface RangeInvalidListener {
    void onRangeInvalidated();
  }

  private static class InvalidRangeDispatcher implements DocumentListener {
    private static final Key<InvalidRangeDispatcher> KEY = Key.create("deferedNotifier");
    private final Map<DiffRangeMarker, RangeInvalidListener> myDiffRangeMarkers = ContainerUtil.newConcurrentMap();

    @Override
    public void documentChanged(DocumentEvent e) {
      for (Iterator<Map.Entry<DiffRangeMarker, RangeInvalidListener>> iterator = myDiffRangeMarkers.entrySet().iterator();
           iterator.hasNext(); ) {
        Map.Entry<DiffRangeMarker, RangeInvalidListener> entry = iterator.next();
        DiffRangeMarker diffRangeMarker = entry.getKey();
        RangeInvalidListener listener = entry.getValue();
        if (!diffRangeMarker.isValid() && listener != null) {
          listener.onRangeInvalidated();
          iterator.remove();
        }
      }
    }

    private static void addClient(@Nonnull Document document,
                                  @Nonnull DiffRangeMarker marker,
                                  @Nonnull RangeInvalidListener listener) {
      InvalidRangeDispatcher notifier = document.getUserData(KEY);
      if (notifier == null) {
        notifier = new InvalidRangeDispatcher();
        document.putUserData(KEY, notifier);
        document.addDocumentListener(notifier);
      }
      assert !notifier.myDiffRangeMarkers.containsKey(marker);
      notifier.myDiffRangeMarkers.put(marker, listener);
    }

    private static void removeClient(@Nonnull Document document,
                                     @Nonnull DiffRangeMarker marker,
                                     @Nonnull RangeInvalidListener listener) {
      InvalidRangeDispatcher notifier = document.getUserData(KEY);
      assert notifier != null;
      notifier.onClientRemoved(document, marker, listener);
    }

    private void onClientRemoved(@Nonnull Document document, @Nonnull DiffRangeMarker marker, @Nonnull RangeInvalidListener listener) {
      if (myDiffRangeMarkers.remove(marker) == listener && myDiffRangeMarkers.isEmpty()) {
        document.putUserData(KEY, null);
        document.removeDocumentListener(this);
      }
    }
  }

  /// delegates

  @Override
  @Nonnull
  public Document getDocument() {
    return myRangeMarker.getDocument();
  }

  @Override
  public int getStartOffset() {
    return myRangeMarker.getStartOffset();
  }

  @Override
  public int getEndOffset() {
    return myRangeMarker.getEndOffset();
  }

  @Override
  public boolean isValid() {
    return myRangeMarker.isValid();
  }

  @Override
  public void setGreedyToLeft(boolean greedy) {
    myRangeMarker.setGreedyToLeft(greedy);
  }

  @Override
  public void setGreedyToRight(boolean greedy) {
    myRangeMarker.setGreedyToRight(greedy);
  }

  @Override
  public boolean isGreedyToRight() {
    return myRangeMarker.isGreedyToRight();
  }

  @Override
  public boolean isGreedyToLeft() {
    return myRangeMarker.isGreedyToLeft();
  }

  @Override
  public void dispose() {
    myRangeMarker.dispose();
  }

  @Override
  @Nullable
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myRangeMarker.getUserData(key);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myRangeMarker.putUserData(key, value);
  }
}
