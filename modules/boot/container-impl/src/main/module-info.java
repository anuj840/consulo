module consulo.container.impl {
  requires consulo.container.api;
  requires consulo.util.nodep;

  exports consulo.container.impl;
  exports consulo.container.impl.classloader;
  exports consulo.container.impl.parser;

  uses consulo.container.boot.ContainerStartup;

  provides consulo.container.plugin.internal.PluginManagerInternal with consulo.container.impl.PluginManagerInternalImpl;
}