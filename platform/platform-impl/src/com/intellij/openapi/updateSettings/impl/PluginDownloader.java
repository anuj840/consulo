/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.*;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.ZipUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URLConnection;
import java.util.List;

/**
 * @author anna
 * @since 10-Aug-2007
 */
public class PluginDownloader {
  private static final Logger LOG = Logger.getInstance("#" + PluginDownloader.class.getName());

  @NonNls private static final String FILENAME = "filename=";

  private final String myPluginId;
  private String myPluginUrl;
  private String myPluginVersion;

  private String myFileName;
  private String myPluginName;

  private File myFile;
  private File myOldFile;
  private String myDescription;
  private PluginId[] myDepends;
  private PluginId[] myOptionalDepends;
  private IdeaPluginDescriptor myDescriptor;

  public PluginDownloader(String pluginId, String pluginUrl, String pluginVersion) {
    myPluginId = pluginId;
    myPluginUrl = pluginUrl;
    myPluginVersion = pluginVersion;
  }

  public PluginDownloader(String pluginId, String pluginUrl, String pluginVersion, String fileName, String pluginName) {
    myPluginId = pluginId;
    myPluginUrl = pluginUrl;
    myPluginVersion = pluginVersion;
    myFileName = fileName;
    myPluginName = pluginName;
  }

  public boolean prepareToInstall() throws IOException {
    return prepareToInstall(new ProgressIndicatorBase());
  }

  public boolean prepareToInstall(ProgressIndicator pi) throws IOException {
    IdeaPluginDescriptor descriptor = null;
    if (!Boolean.getBoolean(StartupActionScriptManager.STARTUP_WIZARD_MODE) && PluginManager.isPluginInstalled(PluginId.getId(myPluginId))) {
      //store old plugins file
      descriptor = PluginManager.getPlugin(PluginId.getId(myPluginId));
      LOG.assertTrue(descriptor != null);
      if (myPluginVersion != null && StringUtil.compareVersionNumbers(descriptor.getVersion(), myPluginVersion) >= 0) {
        LOG.info("Plugin " + myPluginId + ": current version (max) " + myPluginVersion);
        return false;
      }
      myOldFile = descriptor.getPath();
    }

    // download plugin
    String errorMessage = IdeBundle.message("unknown.error");
    try {
      myFile = downloadPlugin(pi);
    }
    catch (IOException ex) {
      myFile = null;
      errorMessage = ex.getMessage();
    }
    if (myFile == null) {
      final String text = IdeBundle.message("error.plugin.was.not.installed", getPluginName(), errorMessage);
      final String title = IdeBundle.message("title.failed.to.download");
      ApplicationManager.getApplication().invokeLater(new Runnable() {
        @Override
        public void run() {
          Messages.showErrorDialog(text, title);
        }
      });
      return false;
    }

    IdeaPluginDescriptorImpl actualDescriptor = loadDescriptionFromJar(myFile);
    if (actualDescriptor != null) {
      if (InstalledPluginsTableModel.wasUpdated(actualDescriptor.getPluginId())) {
        return false; //already updated
      }

      myPluginVersion = actualDescriptor.getVersion();
      if (descriptor != null && StringUtil.compareVersionNumbers(descriptor.getVersion(), actualDescriptor.getVersion()) >= 0) {
        LOG.info("Plugin " + myPluginId + ": current version (max) " + myPluginVersion);
        return false; //was not updated
      }

      if (PluginManagerCore.isIncompatible(actualDescriptor)) {
        LOG.info("Plugin " + myPluginId + " is incompatible with current installation (since: " + actualDescriptor.getSinceBuild() + ", until: " + actualDescriptor.getUntilBuild()+ ")");
        return false; //shouldn't happen
      }

      setDescriptor(actualDescriptor);
    }
    return true;
  }

  @Nullable
  public static IdeaPluginDescriptorImpl loadDescriptionFromJar(final File file) throws IOException {
    IdeaPluginDescriptorImpl descriptor = PluginManagerCore.loadDescriptorFromJar(file);
    if (descriptor == null) {
      if (file.getName().endsWith(".zip")) {
        final File outputDir = FileUtil.createTempDirectory("plugin", "");
        try {
          ZipUtil.extract(file, outputDir, new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
              return true;
            }
          });
          final File[] files = outputDir.listFiles();
          if (files != null && files.length == 1) {
            descriptor = PluginManagerCore.loadDescriptor(files[0], PluginManagerCore.PLUGIN_XML);
          }
        }
        finally {
          FileUtil.delete(outputDir);
        }
      }
    }
    return descriptor;
  }

  public void install(boolean deleteTempFile) throws IOException {
    LOG.assertTrue(myFile != null);
    if (myOldFile != null) {
      // add command to delete the 'action script' file
      StartupActionScriptManager.ActionCommand deleteOld = new StartupActionScriptManager.DeleteCommand(myOldFile);
      StartupActionScriptManager.addActionCommand(deleteOld);
    }
    install(myFile, getPluginName(), deleteTempFile);
  }

  public static void install(final File fromFile, final String pluginName) throws IOException {
    install(fromFile, pluginName, true);
  }

  public static void install(final File fromFile, final String pluginName, boolean deleteFromFile) throws IOException {
    //noinspection HardCodedStringLiteral
    if (fromFile.getName().endsWith(".jar")) {
      // add command to copy file to the IDEA/plugins path
      StartupActionScriptManager.ActionCommand copyPlugin =
        new StartupActionScriptManager.CopyCommand(fromFile, new File(PathManager.getPluginsPath() + File.separator + fromFile.getName()));
      StartupActionScriptManager.addActionCommand(copyPlugin);
    }
    else {
      // add command to unzip file to the IDEA/plugins path
      String unzipPath;
      if (ZipUtil.isZipContainsFolder(fromFile)) {
        unzipPath = PathManager.getPluginsPath();
      }
      else {
        unzipPath = PathManager.getPluginsPath() + File.separator + pluginName;
      }

      StartupActionScriptManager.ActionCommand unzip = new StartupActionScriptManager.UnzipCommand(fromFile, new File(unzipPath));
      StartupActionScriptManager.addActionCommand(unzip);
    }

    // add command to remove temp plugin file
    if (deleteFromFile) {
      StartupActionScriptManager.ActionCommand deleteTemp = new StartupActionScriptManager.DeleteCommand(fromFile);
      StartupActionScriptManager.addActionCommand(deleteTemp);
    }
  }

  private File downloadPlugin(final ProgressIndicator indicator) throws IOException {
    File pluginsTemp = new File(PathManager.getPluginTempPath());
    if (!pluginsTemp.exists() && !pluginsTemp.mkdirs()) {
      throw new IOException(IdeBundle.message("error.cannot.create.temp.dir", pluginsTemp));
    }
    final File file = FileUtil.createTempFile(pluginsTemp, "plugin_", "_download", true, false);

    indicator.checkCanceled();
    indicator.setText2(IdeBundle.message("progress.downloading.plugin", getPluginName()));

    return HttpRequests.request(myPluginUrl).gzip(false).connect(new HttpRequests.RequestProcessor<File>() {
      @Override
      public File process(@NotNull HttpRequests.Request request) throws IOException {
        request.saveToFile(file, indicator);

        String fileName = guessFileName(request.getConnection(), file);
        File newFile = new File(file.getParentFile(), fileName);
        FileUtil.rename(file, newFile);
        return newFile;
      }
    });
  }

  @NotNull
  private String guessFileName(final URLConnection connection, final File file) throws IOException {
    String fileName = null;

    final String contentDisposition = connection.getHeaderField("Content-Disposition");
    LOG.debug("header: " + contentDisposition);

    if (contentDisposition != null && contentDisposition.contains(FILENAME)) {
      final int startIdx = contentDisposition.indexOf(FILENAME);
      final int endIdx = contentDisposition.indexOf(';', startIdx);
      fileName = contentDisposition.substring(startIdx + FILENAME.length(), endIdx > 0 ? endIdx : contentDisposition.length());

      if (StringUtil.startsWithChar(fileName, '\"') && StringUtil.endsWithChar(fileName, '\"')) {
        fileName = fileName.substring(1, fileName.length() - 1);
      }
    }

    if (fileName == null) {
      // try to find a filename in an URL
      final String usedURL = connection.getURL().toString();
      fileName = usedURL.substring(usedURL.lastIndexOf("/") + 1);
      if (fileName.length() == 0 || fileName.contains("?")) {
        fileName = myPluginUrl.substring(myPluginUrl.lastIndexOf("/") + 1);
      }
    }

    if (!PathUtil.isValidFileName(fileName)) {
      FileUtil.delete(file);
      throw new IOException("Invalid filename returned by a server");
    }

    return fileName;
  }

  public String getPluginId() {
    return myPluginId;
  }

  public String getFileName() {
    if (myFileName == null) {
      myFileName = myPluginUrl.substring(myPluginUrl.lastIndexOf("/") + 1);
    }
    return myFileName;
  }

  public String getPluginName() {
    if (myPluginName == null) {
      myPluginName = FileUtil.getNameWithoutExtension(getFileName());
    }
    return myPluginName;
  }

  public String getPluginVersion() {
    return myPluginVersion;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  public String getDescription() {
    return myDescription;
  }

  public void setDepends(List<PluginId> depends, List<PluginId> optionalDependsPlugins) {
    myDepends = depends.isEmpty() ? PluginId.EMPTY_ARRAY : depends.toArray(new PluginId[depends.size()]);
    myDepends = optionalDependsPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : optionalDependsPlugins.toArray(new PluginId[optionalDependsPlugins.size()]);
  }

  @NotNull
  public PluginId[] getDepends() {
    return myDepends == null ? PluginId.EMPTY_ARRAY : myDepends;
  }

  @NotNull
  public PluginId[] getOptionalDepends() {
    return myOptionalDepends == null ? PluginId.EMPTY_ARRAY : myOptionalDepends;
  }

  public static PluginDownloader createDownloader(IdeaPluginDescriptor descriptor) {
    String url = RepositoryHelper.buildUrlForDownload(consulo.ide.updateSettings.UpdateSettings.getInstance().getChannel(), descriptor.getPluginId().toString());

    PluginDownloader downloader = new PluginDownloader(descriptor.getPluginId().getIdString(), url, null, null, descriptor.getName());
    downloader.setDescriptor(descriptor);
    return downloader;
  }

  public void setDescriptor(IdeaPluginDescriptor descriptor) {
    myDescriptor = descriptor;
  }

  public IdeaPluginDescriptor getDescriptor() {
    return myDescriptor;
  }
}
