/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.DifferenceFilter;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import consulo.logging.Logger;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CodeStyleSettingsManager implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(CodeStyleSettingsManager.class);

  public volatile CodeStyleSettings PER_PROJECT_SETTINGS = null;
  public volatile boolean USE_PER_PROJECT_SETTINGS = false;
  public volatile String PREFERRED_PROJECT_CODE_STYLE = null;
  private volatile CodeStyleSettings myTemporarySettings;
  private volatile boolean myIsLoaded = false;

  public static CodeStyleSettingsManager getInstance(@Nullable Project project) {
    if (project == null || project.isDefault()) {
      return getInstance();
    }
    return ProjectCodeStyleSettingsManager.getInstance(project);
  }

  public static CodeStyleSettingsManager getInstance() {
    return ServiceManager.getService(AppCodeStyleSettingsManager.class);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public CodeStyleSettingsManager(Project project) {
  }

  public CodeStyleSettingsManager() {
  }

  @Nonnull
  public static CodeStyleSettings getSettings(@Nullable final Project project) {
    return getInstance(project).getCurrentSettings();
  }

  @Nonnull
  public CodeStyleSettings getCurrentSettings() {
    CodeStyleSettings temporarySettings = myTemporarySettings;
    if (temporarySettings != null) return temporarySettings;
    CodeStyleSettings projectSettings = PER_PROJECT_SETTINGS;
    if (USE_PER_PROJECT_SETTINGS && projectSettings != null) return projectSettings;
    return CodeStyleSchemes.getInstance().findPreferredScheme(PREFERRED_PROJECT_CODE_STYLE).getCodeStyleSettings();
  }

  private void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  private void writeExternal(Element element) throws WriteExternalException {
    DefaultJDOMExternalizer.writeExternal(this, element, new DifferenceFilter<CodeStyleSettingsManager>(this, new CodeStyleSettingsManager()));
  }

  @Override
  public Element getState() {
    Element result = new Element("state");
    try {
      writeExternal(result);
    }
    catch (WriteExternalException e) {
      LOG.error(e);
    }
    return result;
  }

  @Override
  public void loadState(Element state) {
    try {
      readExternal(state);
      myIsLoaded = true;
    }
    catch (InvalidDataException e) {
      LOG.error(e);
    }
  }

  public CodeStyleSettings getTemporarySettings() {
    return myTemporarySettings;
  }

  public void setTemporarySettings(@Nonnull CodeStyleSettings settings) {
    myTemporarySettings = settings;
  }

  public void dropTemporarySettings() {
    myTemporarySettings = null;
  }

  public boolean isLoaded() {
    return myIsLoaded;
  }

  /**
   * Updates document's indent options from indent options providers.
   * <p><b>Note:</b> Calling this method directly when there is an editor associated with the document may cause the editor work
   * incorrectly. To keep consistency with the editor call <code>EditorEx.reinitSettings()</code> instead.
   *
   * @param project  The project of the document.
   * @param document The document to update indent options for.
   */
  public static void updateDocumentIndentOptions(@Nonnull Project project, @Nonnull Document document) {
    if (!project.isDisposed()) {
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
      if (documentManager != null) {
        PsiFile file = documentManager.getPsiFile(document);
        if (file != null) {
          CommonCodeStyleSettings.IndentOptions indentOptions = getSettings(project).getIndentOptionsByFile(file, null, true, null);
          indentOptions.associateWithDocument(document);
        }
      }
    }
  }
}
