package org.tillerino.irc.server;

import org.tillerino.irc.server.compiler.Handler;

public class BaseHandler {
  @Handler("PING")
  public Response ping(Connection conn, String payload) {
    return (a, i) -> a.colon().token(conn.getServerPrefix()).token("PONG").token(payload);
  }

  @Handler("QUIT")
  public Response quit(Connection conn) {
    conn.close();
    return Response.NO_RESPONSE;
  }
}
