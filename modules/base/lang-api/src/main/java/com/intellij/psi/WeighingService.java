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
package com.intellij.psi;

import com.intellij.openapi.util.Computable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.KeyedExtensionCollector;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author peter
 */
public class WeighingService {
  private static final KeyedExtensionCollector<Weigher,Key> COLLECTOR = new KeyedExtensionCollector<Weigher, Key>("com.intellij.weigher") {
    @Override
    protected String keyToString(final Key key) {
      return key.toString();
    }
  };

  private WeighingService() {
  }

  @Nonnull
  public static <T,Loc> WeighingComparable<T,Loc> weigh(final Key<? extends Weigher<T,Loc>> key, final T element, @javax.annotation.Nullable final Loc location) {
    return weigh(key, new Computable.PredefinedValueComputable<T>(element), location);
  }

  @Nonnull
  public static <T,Loc> WeighingComparable<T,Loc> weigh(final Key<? extends Weigher<T,Loc>> key, final Computable<T> element, @javax.annotation.Nullable final Loc location) {
    final List<Weigher> weighers = getWeighers(key);
    return new WeighingComparable<T,Loc>(element, location, ContainerUtil.toArray(weighers, new Weigher[weighers.size()]));
  }

  public static <T,Loc> List<Weigher> getWeighers(Key<? extends Weigher<T, Loc>> key) {
    return COLLECTOR.forKey(key);
  }
}
