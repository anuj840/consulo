/*
 * Copyright 2013-2017 consulo.io
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
package consulo.localize;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Nov-17
 */
class SingleLocalizeValue implements LocalizeValue {
  static final SingleLocalizeValue ourEmpty = new SingleLocalizeValue("");

  private final String myValue;

  SingleLocalizeValue(String value) {
    myValue = value;
  }

  @Nonnull
  @Override
  public String getValue() {
    return myValue;
  }

  @Override
  public long getModificationCount() {
    return 0;
  }

  @Override
  public String toString() {
    return getValue();
  }
}
