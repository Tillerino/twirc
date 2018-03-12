package org.tillerino.irc.server;

import java.io.IOException;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tillerino.irc.server.Response.ResponseList;
import org.tillerino.irc.server.util.ResponseWriter;
import org.tillerino.irc.server.util.Utf8LineBuffer;

public class Connection implements Comparable<Connection> {
  private final Logger log;

  @Nullable
  private String nick;
  private final String serverPrefix;
  @Nullable
  private String userPrefix;

  private final SelectionKey selectionKey;

  private final BlockingQueue<ByteBuffer> output = new ArrayBlockingQueue<>(1000);

  private final Utf8LineBuffer inputBuffer = new Utf8LineBuffer(511);

  @Nullable
  ByteBuffer writeBuffer = null;

  public Connection(Socket socket, SelectionKey selectionKey) throws IOException {
    super();

    serverPrefix = socket.getLocalAddress().getHostName();

    this.selectionKey = selectionKey;

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
    Appendable sWriter = new StringBuffer();
    ResponseWriter writer = new ResponseWriter(sWriter);
    response.write(writer, this);
    writer.crlf();
    queue(ByteBuffer.wrap(sWriter.toString().getBytes(StandardCharsets.UTF_8)));
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

  public void queue(ByteBuffer buffer) {
    if (output.offer(buffer)) {
      selectionKey.interestOps(SelectionKey.OP_WRITE);
    }
  }

  public List<CharSequence> read(ReadableByteChannel channel) throws IOException {
    return inputBuffer.read(channel);
  }

  public void tryWrite(SocketChannel channel) throws IOException {
    for (;;) {
      if (writeBuffer == null) {
        writeBuffer = output.poll();
      }
      if (writeBuffer != null) {
        for (; channel.write(writeBuffer) > 0;) {
        }
        if (!writeBuffer.hasRemaining()) {
          writeBuffer = null;
        }
      } else {
        selectionKey.interestOps(SelectionKey.OP_READ);
        break;
      }
    }
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
