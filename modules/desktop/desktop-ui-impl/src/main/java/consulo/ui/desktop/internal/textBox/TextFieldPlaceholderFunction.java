/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal.textBox;

import com.intellij.ui.components.JBTextField;
import com.intellij.util.BooleanFunction;

/**
 * @author VISTALL
 * @since 2019-11-07
 */
public class TextFieldPlaceholderFunction implements BooleanFunction<JBTextField> {
  public static void install(JBTextField textField) {
    textField.putClientProperty("StatusVisibleFunction", INSTANCE);
  }

  private static final TextFieldPlaceholderFunction INSTANCE = new TextFieldPlaceholderFunction();

  @Override
  public boolean fun(JBTextField textField) {
    return textField.getText().length() == 0;
  }
}
