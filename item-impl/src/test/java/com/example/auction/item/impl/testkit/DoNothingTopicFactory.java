package com.example.auction.item.impl.testkit;

import akka.Done;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.internal.api.broker.TopicFactory;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.broker.Subscriber;
import com.lightbend.lagom.javadsl.api.broker.Topic;

import java.util.concurrent.CompletionStage;


public class DoNothingTopicFactory implements TopicFactory {
  @Override
  public <Message> Topic<Message> create(Descriptor.TopicCall<Message> topicCall) {
    return new Topic<Message>() {
      @Override
      public TopicId topicId() {
        return topicCall.topicId();
      }

      @Override
      public Subscriber<Message> subscribe() {
        return new Subscriber<Message>() {
          @Override
          public Subscriber<Message> withGroupId(String groupId) throws IllegalArgumentException {
            return null;
          }

          @Override
          public Source<Message, ?> atMostOnceSource() {
            return null;
          }

          @Override
          public CompletionStage<Done> atLeastOnce(Flow<Message, Done, ?> flow) {
            return null;
          }
        };
      }
    };
  }
}
