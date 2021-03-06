/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.ui.components.JBLabel;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.desktop.util.awt.MorphColor;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.image.Image;
import consulo.ui.shared.ColorValue;
import consulo.ui.shared.HorizontalAlignment;
import consulo.ui.style.ComponentColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
class DesktopLabelImpl extends SwingComponentDelegate<DesktopLabelImpl.MyJLabel> implements Label {
  public class MyJLabel extends JBLabel implements FromSwingComponentWrapper {
    private LocalizeValue myTextValue;

    private HorizontalAlignment myHorizontalAlignment2 = HorizontalAlignment.LEFT;

    MyJLabel(@Nonnull LocalizeValue text) {
      super(text.get());

      setHorizontalAlignment2(HorizontalAlignment.LEFT);

      myTextValue = text;
    }

    @Override
    public void updateUI() {
      super.updateUI();

      updateText();
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopLabelImpl.this;
    }

    public void setForegroundColor(ColorValue foregroundColor) {
      setForeground(MorphColor.ofWithoutCache(() -> TargetAWT.to(foregroundColor)));
    }

    public void setHorizontalAlignment2(@Nonnull HorizontalAlignment horizontalAlignment) {
      myHorizontalAlignment2 = horizontalAlignment;
      switch (horizontalAlignment) {
        case LEFT:
          setHorizontalAlignment(SwingConstants.LEFT);
          break;
        case CENTER:
          setHorizontalAlignment(SwingConstants.CENTER);
          break;
        case RIGHT:
          setHorizontalAlignment(SwingConstants.RIGHT);
          break;
      }
    }

    public HorizontalAlignment getHorizontalAlignment2() {
      return myHorizontalAlignment2;
    }

    @Nonnull
    public LocalizeValue getTextValue() {
      return myTextValue;
    }

    public void setTextValue(@Nonnull LocalizeValue textValue) {
      myTextValue = textValue;
    }

    private void updateText() {
      if(myTextValue == null) {
        return;
      }

      setText(myTextValue.getValue());
    }
  }

  public DesktopLabelImpl(LocalizeValue text) {
    initialize(new MyJLabel(text));
  }

  @Nonnull
  @Override
  public Label setImage(@Nullable Image icon) {
    toAWTComponent().setIcon(TargetAWT.to(icon));
    return this;
  }

  @Nullable
  @Override
  public Image getImage() {
    return TargetAWT.from(toAWTComponent().getIcon());
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Label setText(@Nonnull LocalizeValue text) {
    toAWTComponent().setTextValue(text);
    return this;
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return toAWTComponent().getTextValue();
  }

  @Nullable
  @Override
  public String getTooltipText() {
    return toAWTComponent().getToolTipText();
  }

  @Nonnull
  @Override
  public Label setToolTipText(@Nullable String text) {
    toAWTComponent().setToolTipText(text);
    return this;
  }

  @Nonnull
  @Override
  public Label setHorizontalAlignment(@Nonnull HorizontalAlignment horizontalAlignment) {
   toAWTComponent().setHorizontalAlignment2(horizontalAlignment);
    return this;
  }

  @Nonnull
  @Override
  public HorizontalAlignment getHorizontalAlignment() {
    return toAWTComponent().getHorizontalAlignment2();
  }

  @Nonnull
  @Override
  public Label setForeground(@Nonnull ColorValue colorValue) {
    toAWTComponent().setForegroundColor(colorValue);
    return this;
  }
}
