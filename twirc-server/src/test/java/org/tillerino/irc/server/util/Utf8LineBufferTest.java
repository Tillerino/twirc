package org.tillerino.irc.server.util;

import static org.junit.Assert.assertEquals;
import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.Test;

public class Utf8LineBufferTest {
  @Test
  public void testBasic() throws Exception {
    ReadableByteChannel in = Channels.newChannel(new ByteArrayInputStream(
        "sup\n\n\r\nthislineistoolong\nabcdefg\r\ntrailing".getBytes(StandardCharsets.UTF_8)));

    Utf8LineBuffer buffer = new Utf8LineBuffer(10);

    List<CharSequence> read = buffer.read(in);

    assertEquals(Arrays.asList("sup", "", "", "<overflow>", "abcdefg"),
        read.stream().map(CharSequence::toString).collect(Collectors.toList()));
  }

  @Test
  public void testRandom() throws Exception {
    int capacity = 10;
    Utf8LineBuffer buffer = new Utf8LineBuffer(capacity);

    List<String> generated = new ArrayList<>();
    generated.add(new String(new char[] {(char) 36383, (char) 39911, (char) 8747}));
    Random random = new Random(2);
    for (int i = 0; i < 1000000; i++) {
      char[] chars = new char[random.nextInt(2 * capacity)];
      for (int j = 0; j < chars.length; j++) {
        char nextChar = (char) (32 + random.nextInt(65536 - 32));
        if (Character.isSurrogate(nextChar)) {
          j--;
          continue;
        }
        chars[j] = nextChar;
      }
      generated.add(new String(chars));
    }

    String hugeString = generated.stream().collect(Collectors.joining("\r\n", "", "\r\n"));

    int pos = 0;
    for (int lower = 0, upper = 0; lower < hugeString.length(); upper =
        Math.min(hugeString.length(), upper + random.nextInt(capacity * 2))) {
      ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(
          hugeString.substring(lower, upper).getBytes(StandardCharsets.UTF_8)));
      List<CharSequence> collected = buffer.read(channel);
      for (CharSequence charSequence : collected) {
        String g = generated.get(pos++);
        String c = charSequence.toString();
        try {
          assertEquals(g.length() > capacity - 2 ? "<overflow>" : g, c);
        } catch (AssertionError e) {
          throw e;
        }
      }
      lower = upper;
    }

    assertEquals(generated.size(), pos);
  }

  @Test
  public void testString() throws Exception {
    char[] data = new char[] {(char) 31655};
    System.out.println((int) data[0]);
    String string = String.valueOf(data);
    System.out.println((int) string.charAt(0));
    byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
    for (int i = 0; i < bytes.length; i++) {
      System.out.print(Integer.toHexString((bytes[i] + 256) % 256) + " ");
    }
    System.out.println();
    System.out.println(string);
  }
}
