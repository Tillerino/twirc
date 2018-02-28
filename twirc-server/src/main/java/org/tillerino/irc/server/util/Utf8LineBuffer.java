package org.tillerino.irc.server.util;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps a byte buffer dedicated to iteratively buffering one line of UTF-8 characters from a stream
 * of bytes.
 */
public class Utf8LineBuffer {
  private final ByteBuffer bytes;
  private CharBuffer chars;

  private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
      .onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);

  private boolean overflow = false;
  int lineStart = 0;
  List<CharSequence> lines = Collections.emptyList();

  /**
   * @param capacity the maximum line length in UTF-8 characters
   */
  public Utf8LineBuffer(int capacity) {
    super();
    bytes = ByteBuffer.allocateDirect(capacity * 5);
    bytes.mark();
    chars = CharBuffer.allocate(capacity);
  }

  public List<CharSequence> read(ReadableByteChannel channel) throws IOException {
    lines = Collections.emptyList();
    reading: for (; channel.read(bytes) > 0;) {
      bytes.limit(bytes.position());
      bytes.reset();
      decoding: for (;;) {
        int lastPos = chars.position();
        CoderResult result = decoder.decode(bytes, chars, false);
        addLines(lastPos);
        if (result.isUnderflow() && bytes.limit() == bytes.capacity()) {
          bytes.compact();
          markAtZero(bytes);
          continue reading;
        }
        if (result.isOverflow()) {
          if (lineStart == 0) {
            overflow = true;
            chars.clear();
            continue decoding;
          } else {
            CharBuffer newBuffer = CharBuffer.allocate(chars.capacity());
            chars.limit(chars.position());
            chars.position(lineStart);
            chars.read(newBuffer);
            lineStart = 0;
            chars = newBuffer;
            continue decoding;
          }
        }
        break;
      }
      bytes.mark();
      bytes.position(bytes.limit());
      bytes.limit(bytes.capacity());
    }
    return lines;
  }

  private void markAtZero(Buffer buffer) {
    int position = buffer.position();
    buffer.rewind();
    buffer.mark();
    buffer.position(position);
  }

  int addLines(int start) {
    int linesBefore = lines.size();
    int limit = chars.position();
    for (int i = start; i < limit; i++) {
      if (chars.get(i) == '\n') {
        chars.position(lineStart);
        chars.limit(limit);
        CharSequence line;
        if (overflow) {
          line = "<overflow>";
          overflow = false;
        } else {
          boolean cr = i > lineStart && chars.get(i - 1) == '\r';
          line = chars.subSequence(0, i - lineStart - (cr ? 1 : 0));
        }
        lineStart = i + 1;
        addLine(line);
      }
    }
    chars.limit(chars.capacity());
    chars.position(limit);
    return lines.size() - linesBefore;
  }

  private void addLine(CharSequence line) {
    if (lines.size() == 0) {
      lines = Collections.singletonList(line);
    } else if (lines.size() == 1) {
      lines = new ArrayList<>(lines);
      lines.add(line);
    } else {
      lines.add(line);
    }
  }
}
