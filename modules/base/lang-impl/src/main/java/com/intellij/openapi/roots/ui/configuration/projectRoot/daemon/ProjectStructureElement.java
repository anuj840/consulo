package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author nik
 */
public abstract class ProjectStructureElement {
  protected final StructureConfigurableContext myContext;

  protected ProjectStructureElement(@Nullable StructureConfigurableContext context) {
    myContext = context;
  }

  public abstract String getPresentableName();

  @Nullable
  public String getDescription() {
    return null;
  }

  public abstract String getTypeName();

  public abstract String getId();

  public abstract void check(ProjectStructureProblemsHolder problemsHolder);

  public abstract List<ProjectStructureElementUsage> getUsagesInElement();


  public boolean shouldShowWarningIfUnused() {
    return false;
  }

  @Nullable
  public ProjectStructureProblemDescription createUnusedElementWarning() {
    return null;
  }


  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public String toString() {
    return getId();
  }
}
