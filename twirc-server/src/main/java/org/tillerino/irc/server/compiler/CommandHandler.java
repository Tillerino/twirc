package org.tillerino.irc.server.compiler;

import org.tillerino.irc.server.Connection;
import org.tillerino.irc.server.Response;
import org.tillerino.irc.server.util.IrcLineSplitter;

public abstract class CommandHandler {
  public abstract Response handle(Connection con, IrcLineSplitter seq);
}
