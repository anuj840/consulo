/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.plugin;

import consulo.container.plugin.internal.PluginManagerInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public final class PluginManager {
  private static final PluginManagerInternal ourInternal;

  static {
    ServiceLoader<PluginManagerInternal> loader = ServiceLoader.load(PluginManagerInternal.class, PluginManagerInternal.class.getClassLoader());

    Iterator<PluginManagerInternal> iterator = loader.iterator();
    if (iterator.hasNext()) {
      ourInternal = iterator.next();
    }
    else {
      throw new IllegalArgumentException("no plugin manager internal");
    }
  }

  @Nonnull
  public static List<PluginDescriptor> getPlugins() {
    return ourInternal.getPlugins();
  }

  public static int getPluginsCount() {
    return getPlugins().size();
  }

  @Nullable
  public static PluginDescriptor findPlugin(@Nonnull PluginId pluginId) {
    for (PluginDescriptor descriptor : getPlugins()) {
      if (descriptor.getPluginId().equals(pluginId)) {
        return descriptor;
      }
    }

    return null;
  }

  @Nullable
  public static PluginDescriptor getPlugin(@Nonnull Class<?> pluginClass) {
    return ourInternal.getPlugin(pluginClass);
  }

  @Nullable
  public static PluginId getPluginId(@Nonnull Class<?> pluginClass) {
    PluginDescriptor plugin = getPlugin(pluginClass);
    return plugin == null ? null : plugin.getPluginId();
  }

  @Nullable
  public static File getPluginPath(@Nonnull Class<?> pluginClass) {
    return ourInternal.getPluginPath(pluginClass);
  }

  public static boolean isInitialized() {
    return ourInternal.isInitialized();
  }
}
