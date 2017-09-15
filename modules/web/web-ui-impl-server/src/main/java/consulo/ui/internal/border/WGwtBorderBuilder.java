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
package consulo.ui.internal.border;

import consulo.ui.RGBColor;
import consulo.ui.impl.UIDataObject;
import consulo.ui.internal.VaadinWrapper;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.web.gwt.shared.ui.state.RGBColorShared;
import consulo.web.gwt.shared.ui.state.border.BorderListState;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WGwtBorderBuilder {
  public static void fill(VaadinWrapper wrapper, BorderListState borderListState) {
    borderListState.myBorders.clear();

    Style currentStyle = StyleManager.get().getCurrentStyle();

    for (UIDataObject.BorderInfo info : wrapper.dataObject().getBorders()) {
      BorderListState.BorderState borderState = new BorderListState.BorderState();
      borderState.myPosition = (byte)info.myBorderPosition.ordinal();
      borderState.myStyle = (byte)info.myBorderStyle.ordinal();

      RGBColor color = currentStyle.getColor(info.myColorKey);
      if (color == null) {
        System.err.println("No color for key " + info.myColorKey);
        continue;
      }

      borderState.myColor = new RGBColorShared(color.getRed(), color.getGreed(), color.getBlue());
      borderState.myWidth = info.myWidth;

      borderListState.myBorders.add(borderState);
    }
  }
}
