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
package com.intellij.util.containers;

import com.intellij.util.DeprecatedMethodException;
import gnu.trove.TObjectHashingStrategy;
import javax.annotation.Nonnull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * @deprecated use {@link ContainerUtil#createWeakValueMap()} instead
 */
@Deprecated
public final class WeakValueHashMap<K, V> extends RefValueHashMap<K, V> {
  private static class MyWeakReference<K, T> extends WeakReference<T> implements MyReference<K, T> {
    private final K key;

    private MyWeakReference(@Nonnull K key, T referent, ReferenceQueue<? super T> q) {
      super(referent, q);
      this.key = key;
    }

    @Nonnull
    @Override
    public K getKey() {
      return key;
    }
  }

  /**
   * @deprecated use {@link ContainerUtil#createWeakValueMap()} instead
   */
  @Deprecated
  public WeakValueHashMap() {
    DeprecatedMethodException.report("Use ContainerUtil.createWeakValueMap() instead");
  }

  WeakValueHashMap(@Nonnull TObjectHashingStrategy<K> strategy) {
    super(strategy);
  }

  @Override
  protected MyReference<K, V> createReference(@Nonnull K key, V value, @Nonnull ReferenceQueue<? super V> queue) {
    return new MyWeakReference<>(key, value, queue);
  }
}
