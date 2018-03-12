package org.tillerino.irc.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tillerino.irc.server.compiler.HandlerCompiler;

public class ConnectionInitiator implements Runnable {
  private final ServerSocketChannel serverSocket;

  private final Logger log;

  public ConnectionInitiator(ServerSocketChannel serverSocket) throws IOException {
    super();
    this.serverSocket = serverSocket;
    log = LoggerFactory.getLogger(
        ConnectionInitiator.class.getCanonicalName() + "." + serverSocket.getLocalAddress());
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
//             System.out.println(key);
            if (key.isAcceptable()) {
//              System.out.println("acceptable");
              SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
              channel.configureBlocking(false);
              SelectionKey newSelectionKey = channel.register(selector, SelectionKey.OP_READ);
              ConnectionHandler handler =
                  new ConnectionHandler(channel.socket(), channels, users, compiler, newSelectionKey);
              newSelectionKey.attach(handler);
            }
            if (key.isReadable()) {
//              System.out.println("readable");
              SocketChannel channel = (SocketChannel) key.channel();
              ConnectionHandler reader = (ConnectionHandler) key.attachment();
              List<CharSequence> lines = reader.read(channel);
              for (CharSequence line : lines) {
                reader.handle(line);
              }
            }
            if (key.isWritable()) {
//              System.out.println("writable");
              SocketChannel channel = (SocketChannel) key.channel();
              ConnectionHandler writer = (ConnectionHandler) key.attachment();
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
