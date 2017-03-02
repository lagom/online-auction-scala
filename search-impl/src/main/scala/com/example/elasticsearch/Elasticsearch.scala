package com.example.elasticsearch

import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.api.Service._
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{CircuitBreaker, Descriptor, Service, ServiceCall}
import com.example.elasticsearch.request._
import com.example.elasticsearch.response._

trait Elasticsearch extends Service {
  override def descriptor: Descriptor =
    named("elastic-search")
      .withCalls(
        restCall(Method.GET, "/:index/items/_search", search _)
          .withCircuitBreaker(CircuitBreaker.identifiedBy("elasticsearch-circuitbreaker")),
        restCall(Method.POST, "/:index/items/:id/_update", updateIndex _)
          .withCircuitBreaker(CircuitBreaker.identifiedBy("elasticsearch-circuitbreaker"))
      ).withAutoAcl(true)

  def search(index: String): ServiceCall[QueryRoot, SearchResult]

  def updateIndex(index: String, itemId: UUID): ServiceCall[UpdateIndexItem, Done]
}




