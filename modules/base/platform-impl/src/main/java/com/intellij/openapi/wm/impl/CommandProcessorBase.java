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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class CommandProcessorBase implements Runnable {
  private static final Logger LOG = Logger.getInstance(CommandProcessorBase.class);
  private final Object myLock = new Object();

  private final List<CommandGroup> myCommandGroupList = new ArrayList<>();
  private int myCommandCount;
  private boolean myFlushed;

  public final int getCommandCount() {
    synchronized (myLock) {
      return myCommandCount;
    }
  }

  public void flush() {
    synchronized (myLock) {
      myFlushed = true;
      //noinspection StatementWithEmptyBody
      while (run(true)) ;
    }
  }

  /**
   * Executes passed batch of commands. Note, that the processor surround the
   * commands with BlockFocusEventsCmd - UnblockFocusEventsCmd. It's required to
   * prevent focus handling of events which is caused by the commands to be executed.
   */
  public final void execute(@Nonnull List<FinalizableCommand> commandList, @Nonnull Condition expired) {
    synchronized (myLock) {
      final boolean isBusy = myCommandCount > 0 || !myFlushed;

      final CommandGroup commandGroup = new CommandGroup(commandList, expired);
      myCommandGroupList.add(commandGroup);
      myCommandCount += commandList.size();

      if (!isBusy) {
        run(false);
      }
    }
  }

  @Override
  public final void run() {
    run(true);
  }

  private boolean run(boolean synchronously) {
    synchronized (myLock) {
      final CommandGroup commandGroup = getNextCommandGroup();
      if (commandGroup == null || commandGroup.isEmpty()) return false;
      final Condition conditionForGroup = commandGroup.getExpireCondition();

      final FinalizableCommand command = commandGroup.takeNextCommand();
      myCommandCount--;

      Condition expire = command.getExpireCondition() != null ? command.getExpireCondition() : conditionForGroup;
      if (expire == null) expire = ApplicationManager.getApplication().getDisposed();
      if (expire.value(null)) return true;
      if (LOG.isDebugEnabled()) {
        LOG.debug("CommandProcessor.run " + command);
      }
      if (synchronously) {
        command.run();
        return true;
      }
      // max. I'm not actually quite sure this should have NON_MODAL modality but it should
      // definitely have some since runnables in command list may (and do) request some PSI activity
      final boolean queueNext = myCommandCount > 0;

      invokeLater(command, expire).doWhenDone(() -> {
        if (queueNext) {
          run(false);
        }
      });
      return true;
    }
  }

  @Nonnull
  protected abstract AsyncResult<Void> invokeLater(@Nonnull Runnable command, @Nonnull Condition<?> condition);

  @Nullable
  private CommandGroup getNextCommandGroup() {
    while (!myCommandGroupList.isEmpty()) {
      final CommandGroup candidate = myCommandGroupList.get(0);
      if (!candidate.isEmpty()) {
        return candidate;
      }
      myCommandGroupList.remove(candidate);
    }

    return null;
  }

  private static class CommandGroup {
    private final List<FinalizableCommand> myList;
    private Condition myExpireCondition;

    private CommandGroup(@Nonnull List<FinalizableCommand> list, @Nonnull Condition expireCondition) {
      myList = list;
      myExpireCondition = expireCondition;
    }

    public Condition getExpireCondition() {
      return myExpireCondition;
    }

    public boolean isEmpty() {
      return myList.isEmpty();
    }

    public FinalizableCommand takeNextCommand() {
      FinalizableCommand command = myList.remove(0);
      if (isEmpty()) {
        // memory leak otherwise
        myExpireCondition = Conditions.alwaysTrue();
      }
      return command;
    }
  }
}
