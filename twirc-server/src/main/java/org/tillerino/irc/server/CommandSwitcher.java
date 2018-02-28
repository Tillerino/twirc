package org.tillerino.irc.server;

import static java.lang.String.format;
import static java.util.stream.Stream.concat;
import java.lang.reflect.Constructor;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.tillerino.irc.server.compiler.CommandHandler;
import org.tillerino.irc.server.compiler.Handler;
import org.tillerino.irc.server.compiler.HandlerCompiler;
import org.tillerino.irc.server.util.IrcException;
import org.tillerino.irc.server.util.IrcLineSplitter;

public class CommandSwitcher {
  private final TreeMap<String, CommandHandler> handlers = new TreeMap<>();

  private final Object[] environment;

  private final HandlerCompiler compiler;

  public CommandSwitcher(HandlerCompiler compiler, Object... environment) {
    this.compiler = compiler;
    this.environment = environment;
  }

  public CommandSwitcher add(Object handler) {
    concat(Stream.of(handler.getClass().getMethods()),
            Stream.of(handler.getClass().getDeclaredMethods()))
        .distinct().filter(m -> m.isAnnotationPresent(Handler.class)).forEach(m -> {
          Constructor<? extends CommandHandler> constructor = compiler.getHandlerConstructor(m);

          Object[] args = new Object[constructor.getParameterCount()];
          required: for (int i = 0; i < args.length; i++) {
            Class<?> type = constructor.getParameterTypes()[i];
            if (type.equals(handler.getClass())) {
              args[i] = handler;
              continue;
            }
            for (int j = 0; j < environment.length; j++) {
              if (type.isInstance(environment[j])) {
                args[i] = environment[j];
                break required;
              }
            }
            throw new RuntimeException("No object of " + type + " provided");
          }

          try {
            String key = m.getAnnotation(Handler.class).value();
            if (handlers.containsKey(key)) {
              throw new RuntimeException(format("Duplicate key %s. Already handled by %s.", key, handlers.get(key)));
            }
            handlers.put(key, constructor.newInstance(args));
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    return this;
  }

  @Nullable Response handle(Connection conn, CharSequence line) {
    IrcLineSplitter splitter = new IrcLineSplitter(line);
    CommandHandler handler = handlers.get(splitter.getString());
    if (handler == null) {
      return null;
    }
    try {
      return handler.handle(conn, splitter);
    } catch (IrcException e) {
      return e.getResponse();
    }
  }
}