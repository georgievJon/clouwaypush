package com.clouway.push.server;

import com.clouway.push.shared.PushEvent;
import com.clouway.push.shared.PushEventHandler;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import static com.clouway.push.server.Subscription.aNewSubscription;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Ivan Lazov <ivan.lazov@clouway.com>
 */
public class MemcachSubscriptionsRepositoryTest {

  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig());

  private SubscriptionsRepository repository;

  private final String subscriber = "john@gmail.com";

  private final Subscription subscription = aNewSubscription().subscriber(subscriber)
                                                              .eventType(SimpleEvent.TYPE)
                                                              .build();

  Subscription anotherSubscription = aNewSubscription().subscriber(subscriber)
                                                       .eventType(AnotherEvent.TYPE)
                                                       .build();

  @Before
  public void setUp() {

    helper.setUp();

    repository = new MemcachSubscriptionsRepository(MemcacheServiceFactory.getMemcacheService());
  }

  @Test
  public void putSingleSubscription() throws Exception {

    storeSubscriptions(subscription);

    assertTrue(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
  }

  @Test
  public void putTwoSubscriptions() throws Exception {

    storeSubscriptions(subscription, anotherSubscription);

    assertTrue(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
    assertTrue(repository.hasSubscription(AnotherEvent.TYPE, subscriber));
  }

  @Test
  public void putSubscriptionsOfSameEventTypeForTwoSubscribers() throws Exception {

    Subscription anotherSubscription = aNewSubscription().subscriber("peter@gmail.com").eventType(SimpleEvent.TYPE).build();

    storeSubscriptions(subscription, anotherSubscription);

    assertTrue(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
    assertTrue(repository.hasSubscription(SimpleEvent.TYPE, "peter@gmail.com"));
  }

  @Test
  public void putSubscriptionsOfSameEventTypeForSingleSubscriber() throws Exception {

    Subscription anotherSubscription = aNewSubscription().subscriber(subscriber).eventType(SimpleEvent.TYPE).build();

    storeSubscriptions(subscription, anotherSubscription);

    List<Subscription> subscriptions = repository.findSubscriptions(subscriber);
    assertThat(subscriptions.size(), is(equalTo(1)));
  }

  @Test
  public void findSubscriberForEvent() throws Exception {

    storeSubscriptions(subscription, anotherSubscription);

    List<Subscription> subscriptions = repository.findSubscriptions(SimpleEvent.TYPE);

    assertThat(subscriptions.size(), is(equalTo(1)));
    assertThat(subscriptions.get(0).getSubscriber(), is(equalTo(subscriber)));
    assertThat(subscriptions.get(0).getEventName(), is(equalTo(SimpleEvent.TYPE.getEventName())));
  }

  @Test
  public void findSubscribersForEvent() throws Exception {

    storeSubscriptions(subscription, anotherSubscription,
                       aNewSubscription().eventType(SimpleEvent.TYPE).subscriber("peter@gmail.com").build(),
                       aNewSubscription().eventType(AnotherEvent.TYPE).subscriber("peter@gmail.com").build());

    List<Subscription> subscriptions = repository.findSubscriptions(SimpleEvent.TYPE);

    assertThat(subscriptions.size(), is(equalTo(2)));

    assertThat(subscriptions.get(0).getSubscriber(), is(equalTo(subscriber)));
    assertThat(subscriptions.get(0).getEventName(), is(equalTo(SimpleEvent.TYPE.getEventName())));

    assertThat(subscriptions.get(1).getSubscriber(), is(equalTo("peter@gmail.com")));
    assertThat(subscriptions.get(1).getEventName(), is(equalTo(SimpleEvent.TYPE.getEventName())));
  }

  @Test
  public void removeSingleSubscription() throws Exception {

    storeSubscriptions(subscription);

    repository.removeSubscription(subscription);

    assertFalse(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
  }

  @Test
  public void removeAllSubscriptionsForSubscriber() throws Exception {

    storeSubscriptions(subscription, anotherSubscription,
                       aNewSubscription().subscriber("peter@gmail.com").eventType(SimpleEvent.TYPE).build(),
                       aNewSubscription().subscriber("peter@gmail.com").eventType(AnotherEvent.TYPE).build());

    repository.removeAllSubscriptions(subscriber);

    assertFalse(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
    assertFalse(repository.hasSubscription(AnotherEvent.TYPE, subscriber));

    assertTrue(repository.hasSubscription(SimpleEvent.TYPE, "peter@gmail.com"));
    assertTrue(repository.hasSubscription(AnotherEvent.TYPE, "peter@gmail.com"));
  }

  @Test
  public void putTwoSubscriptionsRemoveOnlyOneOfThem() throws Exception {

    storeSubscriptions(subscription, anotherSubscription);

    repository.removeSubscription(anotherSubscription);

    assertTrue(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
    assertFalse(repository.hasSubscription(AnotherEvent.TYPE, subscriber));
  }

  @Test
  public void removeSingleSubscriptionByEventTypeAndSubscriber() throws Exception {

    storeSubscriptions(subscription);

    repository.removeSubscription(SimpleEvent.TYPE, subscriber);

    assertFalse(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
  }

  @Test
  public void removeAllSubscriptionsWhenSubscriberDoNotHaveAny() throws Exception {

    repository.removeAllSubscriptions(subscriber);

    assertFalse(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
  }

  @Test
  public void putTwoSubscriptionsRemoveOnlyOneByTypeAndSubscriber() throws Exception {

    storeSubscriptions(subscription, anotherSubscription);

    repository.removeSubscription(AnotherEvent.TYPE, subscriber);

    assertTrue(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
    assertFalse(repository.hasSubscription(AnotherEvent.TYPE, subscriber));
  }

  @Test
  public void findSubscriptionAfterRemovingSubscriptionByTypeAndSubscriber() {

    storeSubscriptions(subscription);

    repository.removeSubscription(SimpleEvent.TYPE, subscriber);

    List<Subscription> subscriptions = repository.findSubscriptions(SimpleEvent.TYPE);
    assertThat(subscriptions.size(), is(0));
  }

  @Test
  public void findSubscriptionAfterRemovingGivenSubscription() {

    storeSubscriptions(subscription);

    repository.removeSubscription(subscription);

    List<Subscription> subscriptions = repository.findSubscriptions(SimpleEvent.TYPE);
    assertThat(subscriptions.size(), is(0));
  }

  @Test
  public void findSubscriptionAfterRemovingAllSubscriptionForSubscriber() {

    storeSubscriptions(subscription, anotherSubscription);

    repository.removeAllSubscriptions(subscriber);

    assertFalse(repository.hasSubscription(SimpleEvent.TYPE, subscriber));
    assertFalse(repository.hasSubscription(AnotherEvent.TYPE, subscriber));
  }

  private void storeSubscriptions(Subscription... subscriptions) {
    for (Subscription subscription : subscriptions) {
      repository.put(subscription);
    }
  }

  private interface AnotherEventHandler extends PushEventHandler {
  }

  private static class AnotherEvent extends PushEvent<AnotherEventHandler> {

    public static Type<AnotherEventHandler> TYPE = new Type<AnotherEventHandler>("AnotherEvent");

    @Override
    public Type<AnotherEventHandler> getAssociatedType() {
      return TYPE;
    }

    @Override
    public void dispatch(AnotherEventHandler handler) {
    }
  }

  private interface SimpleEventHandler extends PushEventHandler {
  }

  private static class SimpleEvent extends PushEvent<SimpleEventHandler> {

    public static Type<SimpleEventHandler> TYPE = new Type<SimpleEventHandler>("SimpleEvent");

    @Override
    public PushEvent.Type<SimpleEventHandler> getAssociatedType() {
      return TYPE;
    }

    @Override
    public void dispatch(SimpleEventHandler handler) {
    }
  }
}
