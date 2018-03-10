package org.tillerino.irc.server;

import static org.tillerino.irc.server.Response.NO_RESPONSE;
import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tillerino.irc.server.compiler.Handler;
import org.tillerino.irc.server.compiler.HandlerCompiler;
import org.tillerino.irc.server.responses.NumericResponse;

public class ConnectionHandler {
  private static final Response WELCOME =
      NumericResponse.create(1, (a, i) -> a.colon().token("Welcome"));

  private final Logger log;

  final Connection info;

  @Nullable
  private CommandSwitcher handshake;

  private CommandSwitcher loop;

  public static class Handshake {
    private final Users users;

    public Handshake(Users users) {
      super();
      this.users = users;
    }

    @Handler("PASS")
    public Response pass() {
      return NO_RESPONSE;
    }

    @Handler("CAP")
    public Response cap() {
      return NO_RESPONSE;
    }

    @Handler("NICK")
    public Response nick(Connection connection, String nick) {
      connection.setNick(nick);
      connection.setUserPrefix(nick + "!" + connection.getServerPrefix());
      return NO_RESPONSE;
    }

    @Handler("USER")
    public Response user(Connection connection) {
      users.registerConnection(connection);
      return WELCOME;
    }
  }

  public static class Loop {
    private final Users users;
    private final Channels channels;

    public Loop(Users users, Channels channels) {
      super();
      this.users = users;
      this.channels = channels;
    }

    @Handler("PRIVMSG")
    public Response pass(Connection conn, String target, String message) {
      if (target.startsWith("#")) {
        return channels.sendMessage(target.substring(1), message, conn);
      }
      return users.sendMessage(target, message, conn);
    }

    @Handler("WHO")
    public Response who(Channel channel) {
      return channel.who();
    }

    @Handler("PING")
    public Response ping(Connection conn, String payload) {
      return (a, i) -> a.colon().token(conn.getServerPrefix()).token("PONG").token(payload);
    }
  }

  public ConnectionHandler(Socket socket, Channels channels, Users users, HandlerCompiler compiler)
      throws IOException {
    super();
    log = LoggerFactory.getLogger(
        ConnectionHandler.class.getCanonicalName() + "." + socket.getRemoteSocketAddress());
    info = new Connection(socket);

    handshake = new CommandSwitcher(compiler).add(new Handshake(users));

    loop = new CommandSwitcher(compiler, channels).add(new Loop(users, channels)).add(channels);
  }

  public void handle(CharSequence line) {
    log.debug("received {}", line);
    Response response;
    if (handshake != null) {
      response = handshake.handle(info, line);
    } else {
      response = loop.handle(info, line);
    }
    if (response == null) {
      log.warn("Unhandled command: {}", line);
      return;
    }
    if (response == Response.NO_RESPONSE) {
      return;
    }
    info.respond(response);
    if (response == WELCOME) {
      handshake = null;
    }
  }
}
