package org.tillerino.irc.server;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.tillerino.irc.server.compiler.CommandHandler;
import org.tillerino.irc.server.compiler.FindersTest.SimpleClass;
import org.tillerino.irc.server.compiler.FindersTest.SimpleFinder;
import org.tillerino.irc.server.compiler.Handler;
import org.tillerino.irc.server.compiler.HandlerCompiler;
import org.tillerino.irc.server.util.IrcLineSplitter;

@RunWith(MockitoJUnitRunner.class)
public class CommandSwitcherTest {
  public static class Handleru {
    List<SimpleClass> users = null;

    @Handler("FOO")
    public Response handle(Connection con, List<SimpleClass> users) {
      this.users = users;
      return Response.NO_RESPONSE;
    }
  }

  @Mock
  SimpleFinder finder;

  @Mock
  Connection connection;

  CommandSwitcher switcher;

  Handleru handler = new Handleru();

  SimpleClass a = new SimpleClass();
  SimpleClass b = new SimpleClass();

  HandlerCompiler compiler = new HandlerCompiler();

  @Before
  public void initMocks() {
    when(finder.finds("a")).thenReturn(a);
    when(finder.finds("b")).thenReturn(b);
    switcher = new CommandSwitcher(compiler, finder);
  }

  @Test
  public void testDirect() throws Exception {
    CommandHandler instance = compiler
        .createClass(Handleru.class.getMethod("handle", Connection.class, List.class))
        .getConstructor(Handleru.class, SimpleFinder.class).newInstance(handler, finder);

    assertNull(handler.users);
    instance.handle(connection, new IrcLineSplitter("a,b"));
    assertEquals(asList(a, b), handler.users);
  }

  @Test
  public void testIndirect() throws Exception {
    switcher.add(handler);
    assertNull(handler.users);
    switcher.handle(connection, "FOO b");
    assertEquals(asList(b), handler.users);
  }
}
