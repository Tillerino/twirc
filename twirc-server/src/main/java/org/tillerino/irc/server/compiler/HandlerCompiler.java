package org.tillerino.irc.server.compiler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class HandlerCompiler {
  private final Finders finders = new Finders();
  /*
   * We need to consider the possibility our own class loader is nested, so we'll keep one modified
   * class loader per class loader of the original handler objects
   */
  private final Map<ClassLoader, CommandClassLoader> classLoaders = new LinkedHashMap<>();
  private final Map<Method, Constructor<? extends CommandHandler>> handlerConstructors =
      new LinkedHashMap<>();

  public Constructor<? extends CommandHandler> getHandlerConstructor(Method m) {
    return handlerConstructors.computeIfAbsent(m,
        method -> (Constructor<? extends CommandHandler>) createClass(method).getConstructors()[0]);
  }

  @SuppressWarnings("unchecked")
  public Class<? extends CommandHandler> createClass(Method m) {
    byte[] clazz = new CommandHandlerWriter(finders, m).write();
    // for debugging
    try (FileOutputStream fos = new FileOutputStream("class.class")) {
      fos.write(clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Class<? extends CommandHandler> loaded = (Class<? extends CommandHandler>) classLoaders
        .computeIfAbsent(m.getDeclaringClass().getClassLoader(), CommandClassLoader::new)
        .defineClass(clazz);
    return loaded;
  }

}
