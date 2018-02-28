package org.tillerino.irc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tillerino.irc.server.compiler.HandlerCompiler;
import org.tillerino.irc.server.util.Utf8LineBuffer;

public class ConnectionInitiator implements Runnable {
  private final ServerSocketChannel serverSocket;

  private final Logger log;

  public ConnectionInitiator(ServerSocketChannel serverSocket)
      throws IOException {
    super();
    this.serverSocket = serverSocket;
    log = LoggerFactory.getLogger(
        ConnectionInitiator.class.getCanonicalName() + "." + serverSocket.getLocalAddress());
  }

  private static class ChannelBuffer {
    final ConnectionHandler handler;

    private ChannelBuffer(ConnectionHandler handler) {
      super();
      this.handler = handler;
    }

    Utf8LineBuffer buffer = new Utf8LineBuffer(511);

    @Nullable
    ByteBuffer writeBuffer = null;

    void tryWrite(SocketChannel channel) throws IOException {
      if (writeBuffer == null) {
        writeBuffer = handler.info.output.poll();
      }
      if (writeBuffer != null) {
        for (; channel.write(writeBuffer) > 0;) {
        }
        if (!writeBuffer.hasRemaining()) {
          writeBuffer = null;
        }
      }
    }
  }

  @Override
  public void run() {
    try {
      Channels channels = new Channels();
      Users users = new Users();
      HandlerCompiler compiler = new HandlerCompiler();
      try (Selector selector = Selector.open()) {
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        for (; selector.select() > 0;) {
          Set<SelectionKey> keys = selector.selectedKeys();
          for (SelectionKey key : keys) {
            if (Thread.interrupted()) {
              return;
            }
            // System.out.println(key);
            if (key.isAcceptable()) {
              System.out.println("acceptable");
              SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
              channel.configureBlocking(false);
              ConnectionHandler handler =
                  new ConnectionHandler(channel.socket(), channels, users, compiler);
              channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE,
                  new ChannelBuffer(handler));
            }
            if (key.isReadable()) {
              System.out.println("readable");
              SocketChannel channel = (SocketChannel) key.channel();
              ChannelBuffer reader = (ChannelBuffer) key.attachment();
              List<CharSequence> lines = reader.buffer.read(channel);
              for (CharSequence line : lines) {
                reader.handler.handle(line);
              }
            }
            if (key.isWritable()) {
              // System.out.println("writable");
              SocketChannel channel = (SocketChannel) key.channel();
              ChannelBuffer writer = (ChannelBuffer) key.attachment();
              writer.tryWrite(channel);
            }
          }
          keys.clear();
        }
      }
      log.error("zero channels selected");
    } catch (IOException e) {
      log.error("Error while accepting connection", e);
    }
  }

  public static void main(String[] args) throws IOException {
    ServerSocketChannel socket =
        ServerSocketChannel.open().bind(new InetSocketAddress("localhost", 6667));
    new ConnectionInitiator(socket).run();
  }
}
