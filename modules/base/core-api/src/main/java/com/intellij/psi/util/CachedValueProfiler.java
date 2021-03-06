// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.containers.ConcurrentMultiMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.concurrent.ConcurrentMap;

public class CachedValueProfiler {
  private static final CachedValueProfiler ourInstance = new CachedValueProfiler();

  private volatile ConcurrentMultiMap<StackTraceElement, ProfilingInfo> myStorage = null;

  private final Object myLock = new Object();
  private final ConcurrentMap<CachedValueProvider.Result, ProfilingInfo> myTemporaryResults = ContainerUtil.newConcurrentMap();

  public static boolean canProfile() {
    return ApplicationManager.getApplication().isInternal();
  }

  public boolean isEnabled() {
    return myStorage != null;
  }

  public void setEnabled(boolean value) {
    synchronized (myLock) {
      if (value) {
        ConcurrentMultiMap<StackTraceElement, ProfilingInfo> storage = myStorage;
        if (storage == null) {
          myStorage = new ConcurrentMultiMap<>();
        }
      }
      else {
        myStorage = null;
      }
    }
  }

  @Nonnull
  public static CachedValueProfiler getInstance() {
    return ourInstance;
  }

  public void createInfo(@Nonnull CachedValueProvider.Result<?> result) {
    ConcurrentMultiMap<StackTraceElement, ProfilingInfo> storage = myStorage;
    if (storage == null) return;

    StackTraceElement origin = findOrigin();
    if (origin == null) return;

    ProfilingInfo info = new ProfilingInfo(origin);
    storage.putValue(origin, info);

    myTemporaryResults.put(result, info);
  }

  @Nullable
  public <T> ProfilingInfo getTemporaryInfo(@Nonnull CachedValueProvider.Result<T> result) {
    return myTemporaryResults.remove(result);
  }

  public MultiMap<StackTraceElement, ProfilingInfo> getStorageSnapshot() {
    return myStorage.copy();
  }

  @Nullable
  private static StackTraceElement findOrigin() {
    StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    return findFirstStackTraceElementExcluding(stackTrace, CachedValueProfiler.class.getName(), CachedValueProvider.class.getName());
  }

  @Nullable
  private static StackTraceElement findFirstStackTraceElementExcluding(@Nonnull StackTraceElement[] stackTraceElements, @Nonnull String... excludedClasses) {
    for (StackTraceElement element : stackTraceElements) {
      if (!matches(element, excludedClasses)) {
        return element;
      }
    }

    return null;
  }

  private static boolean matches(@Nonnull StackTraceElement element, @Nonnull String[] excludedClasses) {
    for (String aClass : excludedClasses) {
      if (element.getClassName().startsWith(aClass)) return true;
    }
    return false;
  }

}
