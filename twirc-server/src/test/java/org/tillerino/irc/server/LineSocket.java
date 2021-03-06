package org.tillerino.irc.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class LineSocket implements Closeable {
  private final Socket socket;
  private BufferedReader reader;
  private final BufferedWriter writer;

  public LineSocket(String host, int port) throws IOException {
    socket = new Socket(host, port);
    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
  }

  @Override
  public void close() {
    try {
      reader.close();
      writer.close();
      socket.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void write(String... line) {
    try {
      for (String element : line) {
        writer.write(element);
        writer.write('\n');
        writer.flush();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String read() {
    try {
      return reader.readLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public LineSocket writef(String format, Object... args) {
    write(String.format(format, args));
    return this;
  }

  public void consumeLine() {
    try {
      for (;;) {
        if (reader.read() == '\n') {
          break;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }
}
