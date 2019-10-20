// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.newvfs.events;

import com.intellij.openapi.util.io.FileAttributes;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An internal class for data transfer from refresh worker to persistent FS impl, do not use.
 */
//@ApiStatus.Internal
public interface ChildInfo {
  ChildInfo[] EMPTY_ARRAY = new ChildInfo[0];

  int getId();

  @Nonnull
  CharSequence getName();

  int getNameId();

  String getSymLinkTarget();

  /**
   * @return null means children are unknown
   */
  @Nullable
  ChildInfo[] getChildren();

  FileAttributes getFileAttributes();
}