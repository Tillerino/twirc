package org.tillerino.irc.server.util;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import org.junit.Test;

public class IrcLineSplitterTest {
  @Test
  public void testSimple() throws Exception {
    IrcLineSplitter adv = new IrcLineSplitter("hello these,are these,are :some strings");
    assertEquals("hello", adv.getString());
    assertEquals("these,are", adv.getString());
    assertEquals(Arrays.asList("these", "are"), adv.getList());
    assertEquals("some strings", adv.getString());
  }
}
