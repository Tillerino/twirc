package org.tillerino.irc.server;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractServerTest {
  private ServerSocketChannel socket;
  private ExecutorService exec;
  private List<LineSocket> connections = new ArrayList<>();

  @Before
  public void startServer() throws Exception {
    socket = ServerSocketChannel.open().bind(new InetSocketAddress("localhost", 0));
    exec = Executors.newCachedThreadPool();
    exec.submit(new ConnectionInitiator(socket));
  }

  @After
  public void stopServer() throws Exception {
    connections.forEach(LineSocket::close);
    exec.shutdownNow();
    assertTrue(exec.awaitTermination(1, TimeUnit.SECONDS));
  }

  protected LineSocket connect(int port) {
    try {
      return new LineSocket("localhost", port != 0 ? port : socket.socket().getLocalPort());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
