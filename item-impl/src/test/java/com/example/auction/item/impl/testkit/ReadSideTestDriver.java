package com.example.auction.item.impl.testkit;

import akka.Done;
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.example.auction.item.impl.CompletionStageUtils;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.lightbend.lagom.javadsl.persistence.*;
import org.pcollections.PSequence;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 *
 */
@Singleton
public class ReadSideTestDriver implements ReadSide {

  private final Injector injector;
  private final Materializer materializer;

  private ConcurrentMap<Class<?>, List<Pair<ReadSideProcessor.ReadSideHandler<?>, Offset>>> processors = new ConcurrentHashMap<>();

  @Inject
  public ReadSideTestDriver(Injector injector, Materializer materializer) {
    this.injector = injector;
    this.materializer = materializer;
  }


  @Override
  public <Event extends AggregateEvent<Event>> void register(Class<? extends ReadSideProcessor<Event>> processorClass) {
    ReadSideProcessor<Event> processor = injector.getInstance(processorClass);
    PSequence<AggregateEventTag<Event>> eventTags = processor.aggregateTags();

    CompletionStage<Done> processorInit = processor.buildHandler().globalPrepare().thenCompose(x -> {
      AggregateEventTag<Event> tag = eventTags.get(0);
      ReadSideProcessor.ReadSideHandler<Event> handler = processor.buildHandler();
      return handler.prepare(tag).thenApply(offset -> {
        List<Pair<ReadSideProcessor.ReadSideHandler<?>, Offset>> currentHandlers =
            processors.computeIfAbsent(tag.eventType(), (z) -> new ArrayList<>());
        currentHandlers.add(Pair.create(handler, offset));
        return Done.getInstance();
      });
    });

    try {
      processorInit.toCompletableFuture().get(30, SECONDS);
    } catch (Exception e) {
      throw new RuntimeException("Couldn't register the processor on the testkit.", e);
    }

  }

  public <Event extends AggregateEvent<Event>> CompletionStage<Done> feed(Event e, Offset offset) {
    AggregateEventTagger<Event> tag = e.aggregateTag();

    List<Pair<ReadSideProcessor.ReadSideHandler<?>, Offset>> list = processors.get(tag.eventType());

    if (list == null) {
      throw new RuntimeException("No processor registered for Event " + tag.eventType().getCanonicalName());
    }

    List<CompletionStage<Done>> stages = list.stream().map(pHandlerOffset -> {
          Flow<Pair<Event, Offset>, Done, ?> flow = ((ReadSideProcessor.ReadSideHandler<Event>) pHandlerOffset.first()).handle();
          return Source.single(Pair.create(e, offset)).via(flow).runWith(Sink.ignore(), materializer);
        }
    ).collect(Collectors.toList());
    return CompletionStageUtils.doAll(stages);

  }

}
