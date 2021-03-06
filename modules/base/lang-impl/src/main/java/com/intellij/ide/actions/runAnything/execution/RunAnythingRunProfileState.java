// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.RunAnythingUtil;
import com.intellij.ide.actions.runAnything.handlers.RunAnythingCommandHandler;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Objects;

public class RunAnythingRunProfileState extends CommandLineState {
  public RunAnythingRunProfileState(@Nonnull ExecutionEnvironment environment, @Nonnull String originalCommand) {
    super(environment);

    RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);
    if (handler != null) {
      setConsoleBuilder(handler.getConsoleBuilder(environment.getProject()));
    }
  }

  @Nonnull
  private RunAnythingRunProfile getRunProfile() {
    RunProfile runProfile = getEnvironment().getRunProfile();
    if (!(runProfile instanceof RunAnythingRunProfile)) {
      throw new IllegalStateException("Got " + runProfile + " instead of RunAnything profile");
    }
    return (RunAnythingRunProfile)runProfile;
  }

  @Nonnull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    RunAnythingRunProfile runProfile = getRunProfile();
    GeneralCommandLine commandLine = runProfile.getCommandLine();
    String originalCommand = runProfile.getOriginalCommand();
    KillableColoredProcessHandler processHandler = new KillableColoredProcessHandler(commandLine) {
      @Override
      protected void notifyProcessTerminated(int exitCode) {
        print(IdeBundle.message("run.anything.console.process.finished", exitCode), ConsoleViewContentType.SYSTEM_OUTPUT);
        printCustomCommandOutput();

        super.notifyProcessTerminated(exitCode);
      }

      private void printCustomCommandOutput() {
        RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);
        if (handler != null) {
          String customOutput = handler.getProcessTerminatedCustomOutput();
          if (customOutput != null) {
            print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
            print(customOutput, ConsoleViewContentType.SYSTEM_OUTPUT);
          }
        }
      }

      @Override
      public final boolean shouldKillProcessSoftly() {
        RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);
        return handler != null ? handler.shouldKillProcessSoftly() : super.shouldKillProcessSoftly();
      }

      private void print(@Nonnull String message, @Nonnull ConsoleViewContentType consoleViewContentType) {
        ConsoleView console = getConsoleView();
        if (console != null) console.print(message, consoleViewContentType);
      }

      @Nullable
      private ConsoleView getConsoleView() {
        RunContentDescriptor contentDescriptor = ExecutionManager.getInstance(getEnvironment().getProject()).getContentManager().findContentDescriptor(getEnvironment().getExecutor(), this);

        ConsoleView console = null;
        if (contentDescriptor != null && contentDescriptor.getExecutionConsole() instanceof ConsoleView) {
          console = (ConsoleView)contentDescriptor.getExecutionConsole();
        }
        return console;
      }
    };

    processHandler.addProcessListener(new ProcessAdapter() {
      boolean myIsFirstLineAdded;

      @Override
      public void onTextAvailable(@Nonnull ProcessEvent event, @Nonnull Key outputType) {
        if (!myIsFirstLineAdded) {
          Objects.requireNonNull(RunAnythingUtil.getOrCreateWrappedCommands(getEnvironment().getProject())).add(Pair.create(StringUtil.trim(event.getText()), originalCommand));
          myIsFirstLineAdded = true;
        }
      }
    });
    processHandler.setHasPty(true);
    return processHandler;
  }
}
