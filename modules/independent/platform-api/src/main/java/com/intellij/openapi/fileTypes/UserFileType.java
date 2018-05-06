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
package com.intellij.openapi.fileTypes;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.options.SettingsEditor;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

public abstract class UserFileType <T extends UserFileType> implements FileType, Cloneable {
  @Nonnull
  private String myName = "";
  private String myDescription = "";
  private Image myIcon = AllIcons.FileTypes.Custom;

  public abstract SettingsEditor<T> getEditor();

  @Override
  public UserFileType clone() {
    try {
      return (UserFileType)super.clone();
    }
    catch (CloneNotSupportedException e) {
      return null; //Can't be
    }
  }

  @Nonnull
  @Override
  public String getId() {
    return myName;
  }
  @Override
  @Nonnull
  public String getDescription() {
    return myDescription;
  }

  public void setName(@Nonnull String name) {
    myName = name;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  @Override
  @Nonnull
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public Image getIcon() {
    return myIcon;
  }

  public void copyFrom(UserFileType newType) {
    myName = newType.getId();
    myDescription = newType.getDescription();
  }

  public void setIcon(Image icon) {
    myIcon = icon;
  }

  @Override
  public String toString() {
    return myName;
  }
}
