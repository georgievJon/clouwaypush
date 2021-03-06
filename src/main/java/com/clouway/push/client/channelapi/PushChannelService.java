package com.clouway.push.client.channelapi;

import com.clouway.push.shared.PushEvent;
import com.google.gwt.user.client.rpc.RemoteService;

/**
 * @author Ivan Lazov <ivan.lazov@clouway.com>
 */
public interface PushChannelService extends RemoteService {

  String connect(String subscriber);

  void subscribe(String subscriber,PushEvent.Type type);

  void unsubscribe(String subscriber, PushEvent.Type event);

  void keepAlive(String subscriber);

  PushEvent dummyMethod();
}
