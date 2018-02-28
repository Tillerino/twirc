package org.tillerino.irc.server;

import java.io.IOException;

public class ResponseWriter {
  private final Appendable out;

  private int written = 0;

  private boolean space = true;

  private boolean crlf = true;

  public ResponseWriter(Appendable out) {
    super();
    this.out = out;
  }

  public boolean canWriteToken(CharSequence token) {
    return canWrite(token.length() + (space ? 0 : 1));
  }

  public ResponseWriter token(CharSequence token) {
    space();
    return append(token);
  }

  public ResponseWriter crlf() {
    if (crlf) {
      return this;
    }
    try {
      out.append('\r');
      out.append('\n');
    } catch (IOException e) {
      throw new WrappedIoException(e);
    }
    written = 0;
    crlf = true;
    space = true;
    return this;
  }

  public ResponseWriter colon() {
    space();
    append(':');
    space = true;
    return this;
  }

  public ResponseWriter space() {
    if (space) {
      return this;
    }
    return append(' ');
  }

  public ResponseWriter append(char c) {
    ensureLimit(1);
    space = c == ' ';
    crlf = false;
    try {
      out.append(c);
    } catch (IOException e) {
      throw new WrappedIoException(e);
    }
    return this;
  }

  public ResponseWriter append(CharSequence seq) {
    ensureLimit(seq.length());
    space = false;
    crlf = false;
    try {
      out.append(seq);
    } catch (IOException e) {
      throw new WrappedIoException(e);
    }
    return this;
  }

  private void ensureLimit(int requested) {
    if (!canWrite(requested)) {
      throw new RuntimeException(
          "Can't write " + requested + " characters without violating line length limit!");
    }
  }

  public boolean canWrite(int requested) {
    return written + requested <= 510;
  }
}
