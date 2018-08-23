/*
 * Copyright 2013-2018 consulo.io
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
package consulo.vfs.newvfs;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl;
import com.intellij.openapi.vfs.newvfs.RefreshSessionImpl;

import javax.annotation.Nonnull;
import javax.inject.Singleton;

/**
 * @author VISTALL
 * @since 2018-05-13
 */
@Singleton
public class NewRefreshQueueImpl extends RefreshQueueImpl {
  @Override
  public void execute(@Nonnull RefreshSessionImpl session) {
    queueSession(session, session.getTransaction());
  }

  @Nonnull
  @Override
  protected AccessToken createHeavyLatch(String id) {
    return AccessToken.EMPTY_ACCESS_TOKEN;
  }
}
