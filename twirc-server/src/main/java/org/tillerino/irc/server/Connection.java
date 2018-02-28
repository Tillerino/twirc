package org.tillerino.irc.server;

import java.io.IOException;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tillerino.irc.server.Response.ResponseList;

public class Connection implements Comparable<Connection> {
  private final Logger log;

  @Nullable
  private String nick;
  private final String serverPrefix;
  @Nullable
  private String userPrefix;

  final BlockingQueue<ByteBuffer> output = new LinkedBlockingQueue<>();

  public Connection(Socket socket) throws IOException {
    super();

    serverPrefix = socket.getLocalAddress().getHostName();

    log = LoggerFactory
        .getLogger(Connection.class.getCanonicalName() + "." + socket.getRemoteSocketAddress());
  }

  public void respond(Response response) {
    if (response instanceof ResponseList) {
      for (Response r : ((ResponseList) response).responses) {
        respond(r);
      }
      return;
    }
    logResponse(response);
    StringWriter sWriter = new StringWriter();
    ResponseWriter writer = new ResponseWriter(sWriter);
    response.write(writer, this);
    writer.crlf();
    output.add(ByteBuffer.wrap(sWriter.toString().getBytes(StandardCharsets.UTF_8)));
  }

  void logResponse(Response r) {
    if (!log.isDebugEnabled()) {
      return;
    }
    StringWriter sWriter = new StringWriter();
    ResponseWriter writer = new ResponseWriter(sWriter);
    r.write(writer, this);
    log.debug("writing response {}", sWriter);
  }

  public String getNick() {
    if (nick == null) {
      throw new IllegalStateException("Connection has not been initialized correctly.");
    }
    return nick;
  }

  public void setNick(String nick) {
    this.nick = nick;
  }

  public String getServerPrefix() {
    return serverPrefix;
  }

  public String getUserPrefix() {
    if (userPrefix == null) {
      throw new IllegalStateException("Connection has not been initialized correctly.");
    }
    return userPrefix;
  }

  public void setUserPrefix(String userPrefix) {
    this.userPrefix = userPrefix;
  }

  @Override
  public int compareTo(Connection o) {
    return o.hashCode() - hashCode();
  }
}
