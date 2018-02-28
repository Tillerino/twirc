package org.tillerino.irc.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.tillerino.irc.server.compiler.Finds;
import org.tillerino.irc.server.responses.NumericResponse;
import org.tillerino.irc.server.util.IrcException;

public class Users {
  Map<String, User> users = new ConcurrentHashMap<>();

  public Response sendMessage(CharSequence nick, CharSequence msg, Connection info) {
    return withUser(nick, user -> user.sendMessage(msg, info));
  }

  Response withUser(CharSequence channelName, Function<User, Response> fun) {
    String nick = extractNick(channelName);
    User user = users.get(nick.toLowerCase());
    if (user == null) {
      return noSuchNick(nick);
    }
    return fun.apply(user);
  }

  private static NumericResponse noSuchNick(String nick) {
    return NumericResponse.create(401,
        (a, i) -> a.token(nick).colon().token("No such nick/channel"));
  }

  String extractNick(CharSequence seq) {
    int end = 0;
    for (; end < seq.length() && seq.charAt(end) != '!' && seq.charAt(end) != ' '; end++) {

    }
    return seq.subSequence(0, end).toString();
  }

  public void registerConnection(Connection info) {
    users.computeIfAbsent(info.getNick().toLowerCase(), x -> new User()).registerConnection(info);
  }

  @Finds(User.class)
  public User getUser(String name) {
    User user = users.get(name.toLowerCase());
    if (user == null) {
      throw new IrcException("No suck nick: " + name, noSuchNick(name));
    }
    return user;
  }
}
