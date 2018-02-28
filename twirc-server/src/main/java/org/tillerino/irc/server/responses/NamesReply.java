package org.tillerino.irc.server.responses;

import java.util.Collection;
import org.tillerino.irc.server.Connection;
import org.tillerino.irc.server.ResponseWriter;

public class NamesReply extends NumericResponse {
  private final Collection<String> names;

  public NamesReply(Collection<String> names, String channelName) {
    super(353);
    this.names = names;
    this.channelName = channelName;
  }

  private final String channelName;

  @Override
  public void write(ResponseWriter appendable, Connection info) {
    prefix(appendable, info);
    for (String string : names) {
      if (!appendable.canWriteToken(string)) {
        appendable.crlf();
        prefix(appendable, info);
      }
      appendable.token(string);
    }
  }

  void prefix(ResponseWriter appendable, Connection info) {
    super.write(appendable, info);
    appendable.token("=").token(channelName).colon();
  }
}
