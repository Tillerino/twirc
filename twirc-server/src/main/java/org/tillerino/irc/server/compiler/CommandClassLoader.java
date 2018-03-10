package org.tillerino.irc.server.compiler;

public class CommandClassLoader extends ClassLoader {
  public CommandClassLoader(ClassLoader parent) {
    super(parent);
  }

  public Class<?> defineClass(byte[] b) {
    return defineClass(null, b, 0, b.length);
  }
}
