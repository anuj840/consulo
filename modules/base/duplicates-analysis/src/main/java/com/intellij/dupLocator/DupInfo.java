package com.intellij.dupLocator;

import com.intellij.dupLocator.util.PsiFragment;
import com.intellij.usageView.UsageInfo;

import javax.annotation.Nullable;

public interface DupInfo {
  int getPatterns();
  int getPatternCost(int number);
  int getPatternDensity(int number);
  PsiFragment[] getFragmentOccurences(int pattern);
  UsageInfo[] getUsageOccurences(int pattern);
  int getFileCount(final int pattern);
  @Nullable
  String getTitle(int pattern);
  @Nullable
  String getComment(int pattern);

  int getHash(final int i);
}
