package org.jetbrains.jps;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author max
 */
public class ModuleChunk {
  private static final NotNullFunction<JpsModule,String> GET_NAME = new NotNullFunction<JpsModule, String>() {
    @NotNull
    @Override
    public String fun(JpsModule dom) {
      return dom.getName();
    }
  };
  private Set<JpsModule> myModules;
  private final boolean myTests;
  private Set<ModuleBuildTarget> myTargets;

  public ModuleChunk(Set<ModuleBuildTarget> targets, boolean tests) {
    myTests = tests;
    myTargets = targets;
    myModules = new LinkedHashSet<JpsModule>();
    for (ModuleBuildTarget target : targets) {
      myModules.add(target.getModule());
    }
  }

  public String getName() {
    if (myModules.size() == 1) return myModules.iterator().next().getName();
    return "ModuleChunk(" + StringUtil.join(myModules, GET_NAME, ",") + ")";
  }

  public Set<JpsModule> getModules() {
    return myModules;
  }

  public boolean isTests() {
    return myTests;
  }

  public Set<ModuleBuildTarget> getTargets() {
    return myTargets;
  }

  public String toString() {
    return getName();
  }

  public JpsProject getProject() {
    return representativeModule().getProject();
  }

  public JpsModule representativeModule() {
    return myModules.iterator().next();
  }
}