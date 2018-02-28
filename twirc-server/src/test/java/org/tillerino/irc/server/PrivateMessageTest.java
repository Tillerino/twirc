package org.tillerino.irc.server;

import static org.awaitility.Awaitility.await;
import org.junit.Test;

public class PrivateMessageTest extends AbstractServerTest {
  @Test
  public void testDirectMessage() throws Exception {
    LineSocket userOne = connect();
    userOne.write("NICK one", "USER one * * :user one");
    await().until(() -> userOne.read().contains(" 001 "));
    LineSocket userTwo = connect();
    userTwo.write("NICK two", "USER two * * :user two");
    await().until(() -> userTwo.read().contains(" 001 "));
    
    userOne.write("PRIVMSG two :what's up");
    await().until(() -> {
      String line = userTwo.read();
      return line.endsWith("PRIVMSG two :what's up");
    });
  }
}
