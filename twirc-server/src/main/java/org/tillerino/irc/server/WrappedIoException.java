package org.tillerino.irc.server;

import java.io.IOException;

public class WrappedIoException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public WrappedIoException(IOException cause) {
    super(cause);
  }

  @Override
  public IOException getCause() {
    return (IOException) super.getCause();
  }
}
