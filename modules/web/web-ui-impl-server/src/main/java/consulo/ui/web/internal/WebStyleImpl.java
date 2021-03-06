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
package consulo.ui.web.internal;

import consulo.ui.shared.ColorValue;
import consulo.ui.shared.RGBColor;
import consulo.ui.style.ColorKey;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.ui.style.Style;
import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebStyleImpl implements Style {
  private Map<ColorKey, ColorValue> myColors = new HashMap<>();
  private final String myName;

  public WebStyleImpl(String name) {
    myName = name;

    for (StandardColors color : StandardColors.values()) {
      myColors.put(color, color.getStaticValue());
    }

    myColors.put(ComponentColors.BORDER, new RGBColor(192, 192, 192));
    myColors.put(ComponentColors.LAYOUT, StandardColors.WHITE.getStaticValue());
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public ColorValue getColor(@Nonnull ColorKey colorKey) {
    ColorValue colorValue = myColors.get(colorKey);
    if(colorValue == null) {
      throw new UnsupportedOperationException(colorKey + " is not supported");
    }
    return colorValue;
  }

  @Override
  public boolean isDark() {
    return false;
  }
}
