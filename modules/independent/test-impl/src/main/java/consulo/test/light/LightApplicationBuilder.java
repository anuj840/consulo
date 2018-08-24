/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import consulo.test.light.impl.LightApplication;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplicationBuilder {
  @Nonnull
  public static LightApplicationBuilder create(@Nonnull Disposable rootDisposable) {
    return new LightApplicationBuilder(rootDisposable);
  }

  private final Disposable myRootDisposable;

  private LightApplicationBuilder(Disposable rootDisposable) {
    myRootDisposable = rootDisposable;
  }

  @Nonnull
  public Application build() {
    return new LightApplication(myRootDisposable);
  }
}
