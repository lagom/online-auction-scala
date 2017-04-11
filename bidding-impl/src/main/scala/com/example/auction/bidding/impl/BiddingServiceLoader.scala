package com.example.auction.bidding.impl

import com.example.auction.bidding.api.BiddingService
import com.example.auction.item.api.ItemService
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class BiddingApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with CassandraPersistenceComponents
  with LagomKafkaComponents {

  lazy val itemService = serviceClient.implement[ItemService]
  override lazy val lagomServer = LagomServer.forServices(
    bindService[BiddingService].to(wire[BiddingServiceImpl])
  )
  override lazy val jsonSerializerRegistry = BiddingSerializerRegistry

  // Initialise everything
  persistentEntityRegistry.register(wire[AuctionEntity])
  readSide.register(wire[AuctionSchedulerProcessor])
  wire[AuctionScheduler]
  wire[ItemServiceSubscriber]
}

class BiddingApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new BiddingApplication(context) with ConductRApplicationComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new BiddingApplication(context) with LagomDevModeComponents

  override def describeServices = List(
    readDescriptor[BiddingService]
  )
}
