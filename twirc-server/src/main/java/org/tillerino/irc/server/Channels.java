package org.tillerino.irc.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.tillerino.irc.server.compiler.Finds;
import org.tillerino.irc.server.compiler.Handler;
import org.tillerino.irc.server.responses.NumericResponse;
import org.tillerino.irc.server.util.IrcException;

public class Channels {
  Map<String, Channel> channels = new ConcurrentHashMap<>();

  @Handler("JOIN")
  public Response join(Connection info, CharSequence channelName) {
    if (channelName.length() <= 1 || channelName.charAt(0) != '#') {
      return Response.NO_RESPONSE;
    }
    return channels.computeIfAbsent(channelName.toString(), name -> new Channel(name))
        .join(info);
  }

  public Response sendMessage(CharSequence channelName, CharSequence msg, Connection info) {
    return withChannel(channelName, channel -> channel.sendMessage(msg, info));
  }

  Response withChannel(CharSequence channelName, Function<Channel, Response> fun) {
    Channel channel = channels.get(channelName.toString());
    if (channel == null) {
      return noSuchChannel(channelName);
    }
    return fun.apply(channel);
  }

  private static NumericResponse noSuchChannel(CharSequence channelName) {
    return NumericResponse.create(403,
        (a, i) -> a.token(channelName).colon().token("No such channel"));
  }

  @Finds(Channel.class)
  public Channel getChannel(String channelName) {
    Channel channel = channels.get(channelName);
    if (channel == null) {
      throw new IrcException("channel not found: " + channelName, noSuchChannel(channelName));
    }
    return channel;
  }
}
