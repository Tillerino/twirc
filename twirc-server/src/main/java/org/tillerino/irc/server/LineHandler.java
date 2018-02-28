package org.tillerino.irc.server;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface LineHandler {
  @Nullable
  Response handle(CharSequence command);

  static class Alternatives implements LineHandler {
    private final List<LineHandler> handlers = new ArrayList<>();

    @Override
    public @Nullable Response handle(CharSequence command) {
      for (LineHandler lineHandler : handlers) {
        Response response = lineHandler.handle(command);
        if (response != null) {
          return response;
        }
      }
      return null;
    }
  }

  static class Start implements LineHandler {
    private final CharSequence token;

    private final LineHandler handler;

    private Start(CharSequence token, LineHandler handler) {
      super();
      this.token = token;
      this.handler = handler;
    }

    @Override
    public @Nullable Response handle(CharSequence cmd) {
      for (int i = 0; i < token.length(); i++) {
        if (i >= cmd.length() || token.charAt(i) != cmd.charAt(i)) {
          return null;
        }
      }
      return handler.handle(cmd.subSequence(token.length(), cmd.length()));
    }
  }

  static class NoResponse implements LineHandler {
    private final Consumer<String> action;

    public NoResponse(Consumer<String> action) {
      super();
      this.action = action;
    }

    @Override
    public Response handle(CharSequence cmd) {
      action.accept(cmd.toString());
      return Response.NO_RESPONSE;
    }
  }

  default LineHandler or(LineHandler alternative) {
    Alternatives collector = new Alternatives();
    if (this instanceof Alternatives) {
      collector.handlers.addAll(((Alternatives) this).handlers);
    } else {
      collector.handlers.add(this);
    }
    if (alternative instanceof Alternatives) {
      collector.handlers.addAll(((Alternatives) alternative).handlers);
    } else {
      collector.handlers.add(alternative);
    }
    return collector;
  }

  static LineHandler start(CharSequence start, LineHandler handler) {
    return new Start(start, handler);
  }

  static LineHandler silently(Consumer<String> action) {
    return new NoResponse(action);
  }

  static LineHandler split(boolean stripColon,
      BiFunction<CharSequence, CharSequence, Response> handler) {
    return cmd -> {
      for (int i = 0; i < cmd.length(); i++) {
        if (cmd.charAt(i) == ' ') {
          CharSequence head = cmd.subSequence(0, i);
          CharSequence tail;
          if (stripColon && i + 1 < cmd.length() && cmd.charAt(i + 1) == ':') {
            tail = cmd.subSequence(i + 2, cmd.length());
          } else {
            tail = cmd.subSequence(i + 1, cmd.length());
          }
          return handler.apply(head, tail);
        }
      }
      return null;
    };
  }

  static final LineHandler IGNORE = cmd -> Response.NO_RESPONSE;
}
