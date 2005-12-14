/*
 * Copyright 2000-2005 JetBrains s.r.o.
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
package com.intellij.debugger.engine;

import com.intellij.debugger.DebuggerBundle;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.j2ee.deployment.JspDeploymentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: lex
 * Date: Apr 5, 2004
 * Time: 2:18:27 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class JSR45PositionManager implements PositionManager {
  private final DebugProcess      myDebugProcess;
  private final Module[] myScope;
  private final JspDeploymentManager myHelper;
  private final String            JSP_PATTERN;
  private Matcher myJspPatternMatcher;
  protected static final @NonNls String JSP_STRATUM = "JSP";

  /**
   * @deprecated
   * Use JSR45PositionManager(DebugProcess debugProcess, Module[] scopeModules) and explicitly specify WebModules to be used when searching for sources
   */
  public JSR45PositionManager(DebugProcess debugProcess) {
    this(debugProcess, ModuleManager.getInstance(debugProcess.getProject()).getModules());
  }

  public JSR45PositionManager(DebugProcess debugProcess, Module[] scopeModules) {
    myDebugProcess = debugProcess;
    myScope = scopeModules;
    myHelper =  ApplicationManager.getApplication().getComponent(JspDeploymentManager.class);
    String jsp_pattern = getJSPClassesPackage();
    if(jsp_pattern.equals("")) {
      jsp_pattern = getJSPClassesNamePattern();
    }
    else {
      jsp_pattern = jsp_pattern + "." + getJSPClassesNamePattern();
    }

    JSP_PATTERN = jsp_pattern;
    myJspPatternMatcher = Pattern.compile(jsp_pattern.replaceAll("\\*", ".*")).matcher("");
  }

  public SourcePosition getSourcePosition(final Location location) throws NoDataException {
    SourcePosition sourcePosition = null;

    try {
      String sourcePath = getRelativePath(location.sourcePath(JSP_STRATUM));
      PsiFile file = myHelper.getDeployedJspSource(sourcePath, myDebugProcess.getProject(), myScope);
      if(file == null) throw new NoDataException();
      int lineNumber = getLineNumber(location);
      sourcePosition = SourcePosition.createFromLine(file, lineNumber - 1);
    }
    catch (AbsentInformationException e) {
    }

    if(sourcePosition == null) throw new NoDataException();

    return sourcePosition;
  }

  protected int getLineNumber(final Location location) {
    return location.lineNumber(JSP_STRATUM);
  }

  public List<ReferenceType> getAllClasses(SourcePosition classPosition) throws NoDataException {
    final FileType fileType = classPosition.getFile().getFileType();
    if(fileType != StdFileTypes.JSP && fileType != StdFileTypes.JSPX) {
      throw new NoDataException();
    }

    final List<ReferenceType> referenceTypes = myDebugProcess.getVirtualMachineProxy().allClasses();

    final List<ReferenceType> result = new ArrayList<ReferenceType>();

    for (final ReferenceType referenceType : referenceTypes) {
      myJspPatternMatcher.reset(referenceType.name());
      if (myJspPatternMatcher.matches()) {
        final List<Location> locations = locationsOfClassAt(referenceType, classPosition);
        if (locations != null) {
          result.add(referenceType);
        }
      }
    }

    return result;
  }

  public List<Location> locationsOfLine(final ReferenceType type, final SourcePosition position) throws NoDataException {
    List<Location> locations = locationsOfClassAt(type, position);
    return locations != null ? locations : Collections.<Location>emptyList();

  }

  private List<Location> locationsOfClassAt(final ReferenceType type, final SourcePosition position) throws NoDataException {
    final FileType fileType = position.getFile().getFileType();
    if(fileType != StdFileTypes.JSP && fileType != StdFileTypes.JSPX) {
      throw new NoDataException();
    }

    return ApplicationManager.getApplication().runReadAction(new Computable<List<Location>>() {
      public List<Location> compute() {
        try {
          final List<String> paths = type.sourcePaths(JSP_STRATUM);
          for (String path : paths) {
            final String relativePath = getRelativePath(path);
            final PsiFile file = myHelper.getDeployedJspSource(relativePath, myDebugProcess.getProject(), myScope);
            if(file != null && file.equals(position.getFile())) {
              return getLocationsOfLine(type, getJspSourceName(file.getName(), type), relativePath, position.getLine() + 1);
            }
          }
        }
        catch (ObjectCollectedException e) {
        }
        catch (AbsentInformationException e) {
        }
        catch (InternalError e) {
          myDebugProcess.getExecutionResult().getProcessHandler().notifyTextAvailable(
            DebuggerBundle.message("internal.error.locations.of.line", type.name()), ProcessOutputTypes.SYSTEM);
        }
        return null;
      }

      // Finds exact server file name (from available in type)
      // This is needed because some servers (e.g. WebSphere) put not exact file name such as 'A.jsp  '
      private String getJspSourceName(final String name, final ReferenceType type) throws AbsentInformationException {
        for(String sourceNameFromType:type.sourceNames(JSP_STRATUM)) {
          if (sourceNameFromType.indexOf(name) >= 0) {
            return sourceNameFromType;
          }
        }
        return name;
      }
    });
  }

  protected List<Location> getLocationsOfLine(final ReferenceType type, final String fileName,
                                              final String relativePath, final int lineNumber) throws AbsentInformationException {
    return type.locationsOfLine(JSP_STRATUM, fileName, lineNumber);
  }

  public ClassPrepareRequest createPrepareRequest(final ClassPrepareRequestor requestor, final SourcePosition position)
    throws NoDataException {
    final FileType fileType = position.getFile().getFileType();
    if(fileType != StdFileTypes.JSP && fileType != StdFileTypes.JSPX) {
      throw new NoDataException();
    }

    return myDebugProcess.getRequestsManager().createClassPrepareRequest(new ClassPrepareRequestor() {
      public void processClassPrepare(DebugProcess debuggerProcess, ReferenceType referenceType) {
        try {
          if(locationsOfClassAt(referenceType, position) != null) {
            requestor.processClassPrepare(debuggerProcess, referenceType);
          }
        }
        catch (NoDataException e) {
        }
      }
    }, JSP_PATTERN);
  }

  protected String getRelativePath(String jspPath) {

    if (jspPath != null) {
      jspPath = jspPath.trim();
      String jspClassesPackage = getJSPClassesPackage();
      final String prefix = jspClassesPackage.replace('.', File.separatorChar);

      if (jspPath.startsWith(prefix)) {
        jspPath = jspPath.substring(prefix.length() + 1);
      }
    }

    return jspPath;
  }

  @NonNls protected abstract String getJSPClassesPackage();

  protected String getJSPClassesNamePattern() {
    return "*";
  }
}
