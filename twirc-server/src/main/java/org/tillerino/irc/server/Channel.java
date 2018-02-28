package org.tillerino.irc.server;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import org.tillerino.irc.server.compiler.FoundBy;
import org.tillerino.irc.server.responses.NamesReply;
import org.tillerino.irc.server.responses.NumericResponse;

@FoundBy(Channels.class)
public class Channel {
  private final String name;

  private final Map<String, Set<Connection>> connections = new ConcurrentHashMap<>();

  private final Response RPL_ENDOFNAMES;

  private final Response JOIN;

  public Channel(String name) {
    super();
    if (!name.startsWith("#")) {
      throw new IllegalArgumentException();
    }
    this.name = name;
    RPL_ENDOFNAMES =
        NumericResponse.create(366, (a, i) -> a.token(name).colon().token("End of NAMES list"));
    JOIN = (a, i) -> a.colon().token(i.getUserPrefix()).token("JOIN").colon().token(name);
  }

  public String getName() {
    return name;
  }

  public Set<String> getNicks() {
    return connections.keySet();
  }

  public Response join(Connection info) {
    connections.values().stream().flatMap(Set::stream).forEach(con -> con.respond(
        (a, i) -> a.colon().token(info.getUserPrefix()).token("JOIN").colon().token(getName())));
    connections.computeIfAbsent(info.getNick(), nick -> new ConcurrentSkipListSet<>()).add(info);
    return JOIN.andThen(new NamesReply(getNicks(), getName())).andThen(RPL_ENDOFNAMES);
  }

  public Response sendMessage(CharSequence msg, Connection sender) {
    connections.values().stream().flatMap(Set::stream).filter(receiver -> receiver != sender)
        .forEach(receiver -> receiver.respond((a, i) -> {
          a.colon().token(sender.getUserPrefix()).token("PRIVMSG").token(getName()).colon()
              .token(msg);
        }));
    return Response.NO_RESPONSE;
  }

  public Response who() {
    // TODO implement
    return Response.NO_RESPONSE;
  }
}
