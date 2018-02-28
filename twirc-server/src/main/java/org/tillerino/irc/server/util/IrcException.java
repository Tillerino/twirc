package org.tillerino.irc.server.util;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.tillerino.irc.server.Response;

/**
 * An exception that can be translated into a {@link Response}.
 */
public class IrcException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final Response response;

  public IrcException(String message, @NonNull Throwable cause, Response response) {
    super(message, cause, false, false);
    this.response = response;
  }

  public IrcException(String message, Response response) {
    super(message, null, false, false);
    this.response = response;
  }

  public Response getResponse() {
    return response;
  }
}
