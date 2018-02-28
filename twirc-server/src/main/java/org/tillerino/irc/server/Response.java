package org.tillerino.irc.server;

import java.util.ArrayList;
import java.util.List;

public interface Response {
  void write(ResponseWriter appendable, Connection info);

  static class ResponseList implements Response {
    List<Response> responses = new ArrayList<>();

    @Override
    public void write(ResponseWriter appendable, Connection info) {
      throw new RuntimeException("Can't be invoked directly");
    }
  }

  default Response andThen(Response response) {
    if (response == null) {
      throw new NullPointerException();
    }
    if (this == NO_RESPONSE) {
      return response;
    }
    if (response == NO_RESPONSE) {
      return this;
    }
    ResponseList collector = new ResponseList();
    if (this instanceof ResponseList) {
      collector.responses.addAll(((ResponseList) this).responses);
    } else {
      collector.responses.add(this);
    }
    if (response instanceof ResponseList) {
      collector.responses.addAll(((ResponseList) response).responses);
    } else {
      collector.responses.add(response);
    }
    return collector;
  }

  static final Response NO_RESPONSE = (a, i) -> {
  };
}
