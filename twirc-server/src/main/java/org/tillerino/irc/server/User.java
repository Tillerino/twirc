package org.tillerino.irc.server;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.tillerino.irc.server.compiler.FoundBy;

@FoundBy(Users.class)
public class User {
  private final Set<Connection> connections = new ConcurrentSkipListSet<>();

  public Response sendMessage(CharSequence msg, Connection sender) {
    connections.forEach(con -> con.respond((a, i) -> a.colon().token(sender.getUserPrefix())
        .token("PRIVMSG").token(con.getNick()).colon().token(msg)));
    return Response.NO_RESPONSE;
  }

  public void registerConnection(Connection info) {
    connections.add(info);
  }
}
