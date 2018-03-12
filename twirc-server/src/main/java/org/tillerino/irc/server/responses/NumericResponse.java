package org.tillerino.irc.server.responses;

import java.text.DecimalFormat;
import org.tillerino.irc.server.Connection;
import org.tillerino.irc.server.Response;
import org.tillerino.irc.server.util.ResponseWriter;

public class NumericResponse implements Response {
  private final String number;

  private static final DecimalFormat format = new DecimalFormat("000");

  public NumericResponse(int number) {
    super();
    this.number = format.format(number);
  }

  @Override
  public void write(ResponseWriter appendable, Connection info) {
    appendable.colon().token(info.getServerPrefix()).token(number).token(info.getNick());
  }

  public static NumericResponse create(int number, Response msg) {
    return new NumericResponse(number) {
      @Override
      public void write(ResponseWriter appendable, Connection info) {
        super.write(appendable, info);
        msg.write(appendable, info);
      }
    };
  }
}
