/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.usages.impl.rules;

import com.intellij.openapi.vcs.FileStatus;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.*;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.SingleParentUsageGroupingRule;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public class UsageTypeGroupingRule extends SingleParentUsageGroupingRule {
  @Nullable
  @Override
  protected UsageGroup getParentGroupFor(@Nonnull Usage usage, @Nonnull UsageTarget[] targets) {
    if (usage instanceof PsiElementUsage) {
      PsiElementUsage elementUsage = (PsiElementUsage)usage;

      PsiElement element = elementUsage.getElement();
      UsageType usageType = getUsageType(element, targets);

      if (usageType == null && element instanceof PsiFile && elementUsage instanceof UsageInfo2UsageAdapter) {
        usageType = ((UsageInfo2UsageAdapter)elementUsage).getUsageType();
      }

      if (usageType != null) return new UsageTypeGroup(usageType);

      if (usage instanceof ReadWriteAccessUsage) {
        ReadWriteAccessUsage u = (ReadWriteAccessUsage)usage;
        if (u.isAccessedForWriting()) return new UsageTypeGroup(UsageType.WRITE);
        if (u.isAccessedForReading()) return new UsageTypeGroup(UsageType.READ);
      }

      return new UsageTypeGroup(UsageType.UNCLASSIFIED);
    }

    return null;
  }

  @Nullable
  private static UsageType getUsageType(PsiElement element, @Nonnull UsageTarget[] targets) {
    if (element == null) return null;

    if (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null) { return UsageType.COMMENT_USAGE; }

    for(UsageTypeProvider provider: UsageTypeProvider.EP_NAME.getExtensionList()) {
      UsageType usageType;
      if (provider instanceof UsageTypeProviderEx) {
        usageType = ((UsageTypeProviderEx) provider).getUsageType(element, targets);
      }
      else {
        usageType = provider.getUsageType(element);
      }
      if (usageType != null) {
        return usageType;
      }
    }

    return null;
  }

  private static class UsageTypeGroup implements UsageGroup {
    private final UsageType myUsageType;

    private UsageTypeGroup(@Nonnull UsageType usageType) {
      myUsageType = usageType;
    }

    @Override
    public void update() {
    }

    @Override
    public Image getIcon() {
      return null;
    }

    @Override
    @Nonnull
    public String getText(@Nullable UsageView view) {
      return view == null ? myUsageType.toString() : myUsageType.toString(view.getPresentation());
    }

    @Override
    public FileStatus getFileStatus() {
      return null;
    }

    @Override
    public boolean isValid() { return true; }
    @Override
    public void navigate(boolean focus) { }
    @Override
    public boolean canNavigate() { return false; }

    @Override
    public boolean canNavigateToSource() {
      return false;
    }

    @Override
    public int compareTo(@Nonnull UsageGroup usageGroup) {
      return getText(null).compareTo(usageGroup.getText(null));
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof UsageTypeGroup)) return false;
      final UsageTypeGroup usageTypeGroup = (UsageTypeGroup)o;
      return myUsageType.equals(usageTypeGroup.myUsageType);
    }

    public int hashCode() {
      return myUsageType.hashCode();
    }

    @Override
    public String toString() {
      return "Type:" + myUsageType.toString(new UsageViewPresentation());
    }
  }
}
