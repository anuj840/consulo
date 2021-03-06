/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.vfs;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.ex.dummy.DummyFileSystem;
import consulo.testFramework.MockApplicationTestCase;

import javax.annotation.Nonnull;

public abstract class DummyFileSystemTest extends MockApplicationTestCase {
  private DummyFileSystem fs;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    fs = new DummyFileSystem();
  }

  public void testDeletionEvents() throws Exception {
    final VirtualFile root = fs.createRoot("root");
    VirtualFile f = WriteAction.compute(() -> {
      VirtualFile res = root.createChildData(this, "f");
      return res;
    });

    final VirtualFileEvent[] events = new VirtualFileEvent[2];
    fs.addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void fileDeleted(@Nonnull VirtualFileEvent e) {
        events[0] = e;
      }

      @Override
      public void beforeFileDeletion(@Nonnull VirtualFileEvent e) {
        events[1] = e;
      }
    });

    f.delete(this);

    for (int i = 0; i < 2; i++) {
      assertNotNull(events[i]);
      assertEquals(f, events[i].getFile());
      assertEquals("f", events[i].getFileName());
      assertEquals(root, events[i].getParent());
    }
  }
}
