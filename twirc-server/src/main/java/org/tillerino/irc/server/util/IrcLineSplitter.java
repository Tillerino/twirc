package org.tillerino.irc.server.util;

import java.util.ArrayList;
import java.util.List;

public class IrcLineSplitter {
  int offset = 0;
  CharSequence seq;

  public IrcLineSplitter(CharSequence seq) {
    super();
    this.seq = seq;
  }

  public CharSequence getSeq() {
    for (; offset < seq.length() && seq.charAt(offset) == ' '; offset++) {
    }
    if (seq.charAt(offset) == ':') {
      int start = offset + 1;
      offset = seq.length();
      return seq.subSequence(start, seq.length());
    }
    int start = offset;
    for (; offset < seq.length() && seq.charAt(offset) != ' '; offset++) {
    }
    return seq.subSequence(start, offset);
  }

  public String getString() {
    return getSeq().toString();
  }

  public List<String> getList() {
    for (; offset < seq.length() && seq.charAt(offset) == ' '; offset++) {
    }
    List<String> list = new ArrayList<>();
    int start = offset;
    for (; offset < seq.length() && seq.charAt(offset) != ' '; offset++) {
      if (seq.charAt(offset) == ',') {
        list.add(seq.subSequence(start, offset).toString());
        offset++;
        start = offset;
      }
    }
    if (start < offset) {
      list.add(seq.subSequence(start, offset).toString());
    }
    return list;
  }
}
