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
package org.jetbrains.idea.maven.importing;

import com.intellij.compiler.impl.javaCompiler.javac.JavacCompilerConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.Stack;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.consulo.java.platform.module.extension.JavaMutableModuleExtension;
import org.consulo.maven.module.extension.MavenMutableModuleExtension;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.importing.configurers.MavenModuleConfigurer;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.*;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;
import org.jetbrains.idea.maven.utils.MavenProgressIndicator;
import org.jetbrains.idea.maven.utils.MavenUtil;
import org.jetbrains.jps.model.java.compiler.JpsJavaCompilerOptions;

import java.io.File;
import java.util.*;

public class MavenProjectImporter {
  private final Project myProject;
  private final MavenProjectsTree myProjectsTree;
  private final Map<VirtualFile, Module> myFileToModuleMapping;
  private volatile Map<MavenProject, MavenProjectChanges> myProjectsToImportWithChanges;
  private volatile Set<MavenProject> myAllProjects;
  private final boolean myImportModuleGroupsRequired;
  private final MavenModifiableModelsProvider myModelsProvider;
  private final MavenImportingSettings myImportingSettings;

  private final ModifiableModuleModel myModuleModel;

  private final List<Module> myCreatedModules = new ArrayList<Module>();

  private final Map<MavenProject, Module> myMavenProjectToModule = new THashMap<MavenProject, Module>();
  private final Map<MavenProject, String> myMavenProjectToModuleName = new THashMap<MavenProject, String>();
  private final Map<MavenProject, String> myMavenProjectToModulePath = new THashMap<MavenProject, String>();

  public MavenProjectImporter(Project p,
                              MavenProjectsTree projectsTree,
                              Map<VirtualFile, Module> fileToModuleMapping,
                              Map<MavenProject, MavenProjectChanges> projectsToImportWithChanges,
                              boolean importModuleGroupsRequired,
                              MavenModifiableModelsProvider modelsProvider,
                              MavenImportingSettings importingSettings) {
    myProject = p;
    myProjectsTree = projectsTree;
    myFileToModuleMapping = fileToModuleMapping;
    myProjectsToImportWithChanges = projectsToImportWithChanges;
    myImportModuleGroupsRequired = importModuleGroupsRequired;
    myModelsProvider = modelsProvider;
    myImportingSettings = importingSettings;

    myModuleModel = modelsProvider.getModuleModel();
  }

  @Nullable
  public List<MavenProjectsProcessorTask> importProject() {
    List<MavenProjectsProcessorTask> postTasks = new ArrayList<MavenProjectsProcessorTask>();

    boolean hasChanges;

    // in the case projects are changed during importing we must memorise them
    myAllProjects = new LinkedHashSet<MavenProject>(myProjectsTree.getProjects());
    myAllProjects.addAll(myProjectsToImportWithChanges.keySet()); // some projects may already have been removed from the tree

    hasChanges = deleteIncompatibleModules();
    myProjectsToImportWithChanges = collectProjectsToImport(myProjectsToImportWithChanges);

    mapMavenProjectsToModulesAndNames();

    if (myProject.isDisposed()) return null;

    final boolean projectsHaveChanges = projectsToImportHaveChanges();
    if (projectsHaveChanges) {
      hasChanges = true;
      importModules(postTasks);
      scheduleRefreshResolvedArtifacts(postTasks);
    }

    if (projectsHaveChanges || myImportModuleGroupsRequired) {
      hasChanges = true;
      configModuleGroups();
    }

    if (myProject.isDisposed()) return null;

    boolean modulesDeleted = deleteObsoleteModules();
    hasChanges |= modulesDeleted;
    if (hasChanges) {
      removeUnusedProjectLibraries();
    }

    final boolean finalHasChanges = hasChanges;

    MavenUtil.invokeAndWaitWriteAction(myProject, new Runnable() {
      public void run() {
        if (finalHasChanges) {
          myModelsProvider.commit();

          if (projectsHaveChanges) {
            removeOutdatedCompilerConfigSettings();

            for (MavenProject mavenProject : myAllProjects) {
              Module module = myMavenProjectToModule.get(mavenProject);
              if (module != null && module.isDisposed()) {
                module = null;
              }

              for (MavenModuleConfigurer configurer : MavenModuleConfigurer.getConfigurers()) {
                configurer.configure(mavenProject, myProject, module);
              }
            }
          }
        }
        else {
          myModelsProvider.dispose();
        }
      }
    });

    return postTasks;
  }

  private boolean projectsToImportHaveChanges() {
    for (MavenProjectChanges each : myProjectsToImportWithChanges.values()) {
      if (each.hasChanges()) return true;
    }
    return false;
  }

  private Map<MavenProject, MavenProjectChanges> collectProjectsToImport(Map<MavenProject, MavenProjectChanges> projectsToImport) {
    Map<MavenProject, MavenProjectChanges> result = new THashMap<MavenProject, MavenProjectChanges>(projectsToImport);
    result.putAll(collectNewlyCreatedProjects()); // e.g. when 'create modules fro aggregators' setting changes

    Set<MavenProject> allProjectsToImport = result.keySet();
    Set<MavenProject> selectedProjectsToImport = selectProjectsToImport(allProjectsToImport);

    Iterator<MavenProject> it = allProjectsToImport.iterator();
    while (it.hasNext()) {
      if (!selectedProjectsToImport.contains(it.next())) it.remove();
    }

    return result;
  }

  private Map<MavenProject, MavenProjectChanges> collectNewlyCreatedProjects() {
    Map<MavenProject, MavenProjectChanges> result = new THashMap<MavenProject, MavenProjectChanges>();

    for (MavenProject each : myAllProjects) {
      Module module = myFileToModuleMapping.get(each.getFile());
      if (module == null) {
        result.put(each, MavenProjectChanges.ALL);
      }
    }

    return result;
  }

  private Set<MavenProject> selectProjectsToImport(Collection<MavenProject> originalProjects) {
    Set<MavenProject> result = new THashSet<MavenProject>();
    for (MavenProject each : originalProjects) {
     /// if (!shouldCreateModuleFor(each)) continue;   //TODO [VISTALL] maven - search why it return true
      result.add(each);
    }
    return result;
  }

  private boolean shouldCreateModuleFor(MavenProject project) {
    if (myProjectsTree.isIgnored(project)) return false;
    return !project.isAggregator() || myImportingSettings.isCreateModulesForAggregators();
  }

  private boolean deleteIncompatibleModules() {
    final Pair<List<Pair<MavenProject, Module>>, List<Pair<MavenProject, Module>>> incompatible = collectIncompatibleModulesWithProjects();
    final List<Pair<MavenProject, Module>> incompatibleMavenized = incompatible.first;
    final List<Pair<MavenProject, Module>> incompatibleNotMavenized = incompatible.second;

    if (incompatibleMavenized.isEmpty() && incompatibleNotMavenized.isEmpty()) return false;

    boolean changed = false;

    // For already mavenized modules the type may change because maven project plugins were resolved and MavenImporter asked to create a module of a different type.
    // In such cases we must change module type silently.
    for (Pair<MavenProject, Module> each : incompatibleMavenized) {
      myFileToModuleMapping.remove(each.first.getFile());
      myModuleModel.disposeModule(each.second);
      changed |= true;
    }

    if (incompatibleNotMavenized.isEmpty()) return changed;

    final int[] result = new int[1];
    MavenUtil.invokeAndWait(myProject, myModelsProvider.getModalityStateForQuestionDialogs(), new Runnable() {
      public void run() {
        String message = ProjectBundle.message("maven.import.incompatible.modules",
                                               incompatibleNotMavenized.size(),
                                               formatProjectsWithModules(incompatibleNotMavenized));
        String[] options = {
          ProjectBundle.message("maven.import.incompatible.modules.recreate"),
          ProjectBundle.message("maven.import.incompatible.modules.ignore")
        };

        result[0] = Messages.showOkCancelDialog(myProject, message,
                                                ProjectBundle.message("maven.project.import.title"),
                                                options[0], options[1], Messages.getQuestionIcon());
      }
    });

    if (result[0] == 0) {
      for (Pair<MavenProject, Module> each : incompatibleNotMavenized) {
        myFileToModuleMapping.remove(each.first.getFile());
        myModuleModel.disposeModule(each.second);
      }
      changed |= true;
    }
    else {
      myProjectsTree.setIgnoredState(MavenUtil.collectFirsts(incompatibleNotMavenized), true, true);
      changed |= false;
    }

    return changed;
  }

  /**
   * Collects modules that need to change module type
   * @return the first List in returned Pair contains already mavenized modules, the second List - not mavenized
   */
  private Pair<List<Pair<MavenProject, Module>>, List<Pair<MavenProject, Module>>> collectIncompatibleModulesWithProjects() {
    List<Pair<MavenProject, Module>> incompatibleMavenized = new ArrayList<Pair<MavenProject, Module>>();
    List<Pair<MavenProject, Module>> incompatibleNotMavenized = new ArrayList<Pair<MavenProject, Module>>();

    MavenProjectsManager manager = MavenProjectsManager.getInstance(myProject);
    for (MavenProject each : myAllProjects) {
      Module module = myFileToModuleMapping.get(each.getFile());
      if (module == null) continue;

      if (shouldCreateModuleFor(each)) {
        (manager.isMavenizedModule(module) ? incompatibleMavenized : incompatibleNotMavenized).add(Pair.create(each, module));
      }
    }
    return Pair.create(incompatibleMavenized, incompatibleNotMavenized);
  }

  private static String formatProjectsWithModules(List<Pair<MavenProject, Module>> projectsWithModules) {
    return StringUtil.join(projectsWithModules, new Function<Pair<MavenProject, Module>, String>() {
        public String fun(Pair<MavenProject, Module> each) {
          MavenProject project = each.first;
          Module module = each.second;
          return module.getName() +
                 "' for Maven project " +
                 project.getMavenId().getDisplayString();
        }
      }, "<br>");
  }

  private boolean deleteObsoleteModules() {
    final List<Module> obsoleteModules = collectObsoleteModules();
    if (obsoleteModules.isEmpty()) return false;

    setMavenizedModules(obsoleteModules, false);

    final int[] result = new int[1];
    MavenUtil.invokeAndWait(myProject, myModelsProvider.getModalityStateForQuestionDialogs(), new Runnable() {
      public void run() {
        result[0] = Messages.showYesNoDialog(myProject,
                                             ProjectBundle.message("maven.import.message.delete.obsolete", formatModules(obsoleteModules)),
                                             ProjectBundle.message("maven.project.import.title"),
                                             Messages.getQuestionIcon());
      }
    });

    if (result[0] == 1) return false;// NO

    for (Module each : obsoleteModules) {
      myModuleModel.disposeModule(each);
    }

    return true;
  }

  private List<Module> collectObsoleteModules() {
    List<Module> remainingModules = new ArrayList<Module>();
    Collections.addAll(remainingModules, myModuleModel.getModules());

    for (MavenProject each : selectProjectsToImport(myAllProjects)) {
      remainingModules.remove(myMavenProjectToModule.get(each));
    }

    List<Module> obsolete = new ArrayList<Module>();
    final MavenProjectsManager manager = MavenProjectsManager.getInstance(myProject);
    for (Module each : remainingModules) {
      if (manager.isMavenizedModule(each)) {
        obsolete.add(each);
      }
    }
    return obsolete;
  }

  private static String formatModules(final Collection<Module> modules) {
    return StringUtil.join(modules, new Function<Module, String>() {
        public String fun(final Module m) {
          return "'" + m.getName() + "'";
        }
      }, "\n");
  }

  private static void doRefreshFiles(Set<File> files) {
    LocalFileSystem.getInstance().refreshIoFiles(files);
  }

  private void scheduleRefreshResolvedArtifacts(List<MavenProjectsProcessorTask> postTasks) {
    // We have to refresh all the resolved artifacts manually in order to
    // update all the VirtualFilePointers. It is not enough to call
    // VirtualFileManager.refresh() since the newly created files will be only
    // picked by FS when FileWatcher finishes its work. And in the case of import
    // it doesn't finish in time.
    // I couldn't manage to write a test for this since behaviour of VirtualFileManager
    // and FileWatcher differs from real-life execution.

    List<MavenArtifact> artifacts = new ArrayList<MavenArtifact>();
    for (MavenProject each : myProjectsToImportWithChanges.keySet()) {
      artifacts.addAll(each.getDependencies());
    }

    final Set<File> files = new THashSet<File>();
    for (MavenArtifact each : artifacts) {
      if (each.isResolved()) files.add(each.getFile());
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      doRefreshFiles(files);
    }
    else {
      postTasks.add(new MavenProjectsProcessorTask() {
        public void perform(Project project, MavenEmbeddersManager embeddersManager, MavenConsole console, MavenProgressIndicator indicator)
          throws MavenProcessCanceledException {
          indicator.setText("Refreshing files...");
          doRefreshFiles(files);
        }
      });
    }
  }

  private void mapMavenProjectsToModulesAndNames() {
    for (MavenProject each : myAllProjects) {
      Module module = myFileToModuleMapping.get(each.getFile());
      if (module != null) {
        myMavenProjectToModule.put(each, module);
      }
    }

    MavenModuleNameMapper.map(myAllProjects,
                              myMavenProjectToModule,
                              myMavenProjectToModuleName,
                              myMavenProjectToModulePath,
                              myImportingSettings.getDedicatedModuleDir());
  }

  private void removeOutdatedCompilerConfigSettings() {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    final JpsJavaCompilerOptions javacOptions = JavacCompilerConfiguration.getInstance(myProject);
    String options = javacOptions.ADDITIONAL_OPTIONS_STRING;
    options = options.replaceFirst("(-target (\\S+))", ""); // Old IDEAs saved
    javacOptions.ADDITIONAL_OPTIONS_STRING = options;
  }

  private void importModules(final List<MavenProjectsProcessorTask> postTasks) {
    Map<MavenProject, MavenProjectChanges> projectsWithChanges = myProjectsToImportWithChanges;

    Set<MavenProject> projectsWithNewlyCreatedModules = new THashSet<MavenProject>();

    for (MavenProject each : projectsWithChanges.keySet()) {
      if (ensureModuleCreated(each)) {
        projectsWithNewlyCreatedModules.add(each);
      }
    }

    List<Module> modulesToMavenize = new ArrayList<Module>();
    List<MavenModuleImporter> importers = new ArrayList<MavenModuleImporter>();

    for (Map.Entry<MavenProject, MavenProjectChanges> each : projectsWithChanges.entrySet()) {
      MavenProject project = each.getKey();
      Module module = myMavenProjectToModule.get(project);
      boolean isNewModule = projectsWithNewlyCreatedModules.contains(project);

      MavenModuleImporter moduleImporter = createModuleImporter(module, project, each.getValue());
      modulesToMavenize.add(module);
      importers.add(moduleImporter);

      moduleImporter.config(isNewModule);
    }

    for (MavenProject project : myAllProjects) {
      if (!projectsWithChanges.containsKey(project)) {
        Module module = myMavenProjectToModule.get(project);
        if (module == null) continue;

        importers.add(createModuleImporter(module, project, null));
      }
    }

    for (MavenModuleImporter importer : importers) {
      importer.preConfigFacets();
    }

    for (MavenModuleImporter importer : importers) {
      importer.configFacets(postTasks);
    }

    setMavenizedModules(modulesToMavenize, true);
  }

  private void setMavenizedModules(final Collection<Module> modules, final boolean mavenized) {
    MavenUtil.invokeAndWaitWriteAction(myProject, new Runnable() {
      @Override
      public void run() {
        for (Module module : modules) {
          final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

          final ModifiableRootModel modifiableModel = moduleRootManager.getModifiableModel();
          //noinspection ConstantConditions
          modifiableModel.getExtensionWithoutCheck(JavaMutableModuleExtension.class).setEnabled(true);
          //noinspection ConstantConditions
          modifiableModel.getExtensionWithoutCheck(MavenMutableModuleExtension.class).setEnabled(mavenized);
          modifiableModel.commit();
        }
      }
    });
  }

  private boolean ensureModuleCreated(MavenProject project) {
    if (myMavenProjectToModule.get(project) != null) return false;

    final String path = myMavenProjectToModulePath.get(project);
    final String name = myMavenProjectToModuleName.get(project);


    final Module module = myModuleModel.newModule(name, path);
    myMavenProjectToModule.put(project, module);
    myCreatedModules.add(module);
    return true;
  }


  private MavenModuleImporter createModuleImporter(Module module, MavenProject mavenProject, @Nullable MavenProjectChanges changes) {
    return new MavenModuleImporter(module,
                                   myProjectsTree,
                                   mavenProject,
                                   changes,
                                   myMavenProjectToModuleName,
                                   myImportingSettings,
                                   myModelsProvider);
  }

  private void configModuleGroups() {
    if (!myImportingSettings.isCreateModuleGroups()) return;

    final Stack<String> groups = new Stack<String>();
    final boolean createTopLevelGroup = myProjectsTree.getRootProjects().size() > 1;

    myProjectsTree.visit(new MavenProjectsTree.SimpleVisitor() {
      int depth = 0;

      @Override
      public boolean shouldVisit(MavenProject project) {
        // in case some project has been added while we were importing
        return myMavenProjectToModuleName.containsKey(project);
      }

      public void visit(MavenProject each) {
        depth++;

        String name = myMavenProjectToModuleName.get(each);

        if (shouldCreateGroup(each)) {
          groups.push(ProjectBundle.message("module.group.name", name));
        }

        if (!shouldCreateModuleFor(each)) {
          return;
        }

        Module module = myModuleModel.findModuleByName(name);
        if (module == null) return;
        myModuleModel.setModuleGroupPath(module, groups.isEmpty() ? null : ArrayUtil.toStringArray(groups));
      }

      public void leave(MavenProject each) {
        if (shouldCreateGroup(each)) {
          groups.pop();
        }
        depth--;
      }

      private boolean shouldCreateGroup(MavenProject project) {
        return !myProjectsTree.getModules(project).isEmpty()
               && (createTopLevelGroup || depth > 1);
      }
    });
  }

  private boolean removeUnusedProjectLibraries() {
    Set<Library> allLibraries = new THashSet<Library>();
    Collections.addAll(allLibraries, myModelsProvider.getAllLibraries());

    Set<Library> usedLibraries = new THashSet<Library>();
    for (ModuleRootModel eachModel : collectModuleModels()) {
      for (OrderEntry eachEntry : eachModel.getOrderEntries()) {
        if (eachEntry instanceof LibraryOrderEntry) {
          Library lib = ((LibraryOrderEntry)eachEntry).getLibrary();
          if (MavenRootModelAdapter.isMavenLibrary(lib)) usedLibraries.add(lib);
        }
      }
    }

    Set<Library> unusedLibraries = new THashSet<Library>(allLibraries);
    unusedLibraries.removeAll(usedLibraries);

    boolean removed = false;
    for (Library each : unusedLibraries) {
      if (MavenRootModelAdapter.isMavenLibrary(each) && !MavenRootModelAdapter.isChangedByUser(each)) {
        myModelsProvider.removeLibrary(each);
        removed = true;
      }
    }
    return removed;
  }

  private Collection<ModuleRootModel> collectModuleModels() {
    Map<Module, ModuleRootModel> rootModels = new THashMap<Module, ModuleRootModel>();
    for (MavenProject each : myProjectsToImportWithChanges.keySet()) {
      Module module = myMavenProjectToModule.get(each);
      ModifiableRootModel rootModel = myModelsProvider.getRootModel(module);
      rootModels.put(module, rootModel);
    }
    for (Module each : myModuleModel.getModules()) {
      if (rootModels.containsKey(each)) continue;
      rootModels.put(each, myModelsProvider.getRootModel(each));
    }
    return rootModels.values();
  }

  public List<Module> getCreatedModules() {
    return myCreatedModules;
  }
}
