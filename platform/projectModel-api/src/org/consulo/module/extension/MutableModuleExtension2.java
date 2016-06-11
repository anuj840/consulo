/*
 * Copyright 2013-2016 must-be.org
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
package org.consulo.module.extension;

import consulo.ui.Component;
import consulo.ui.RequiredUIThread;
import consulo.ui.UIAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public interface MutableModuleExtension2<T extends ModuleExtension<T>> extends MutableModuleExtension<T> {
  @Nullable
  @RequiredUIThread
  Component createConfigurablePanel2(@NotNull UIAccess uiAccess, @NotNull Runnable updateOnCheck);
}
