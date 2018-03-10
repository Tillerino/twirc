package org.tillerino.irc.server;

import java.io.IOException;

public class WrappedIoException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final IOException cause;

  public WrappedIoException(IOException cause) {
    super(cause);
    this.cause = cause;
  }

  @Override
  public IOException getCause() {
    return cause;
  }
}
