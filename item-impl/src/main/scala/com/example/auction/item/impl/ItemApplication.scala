package com.example.auction.item.impl

import com.example.auction.bidding.api.BiddingService
import com.example.auction.item.api.ItemService
import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import play.api.libs.ws.ahc.AhcWSComponents
import com.softwaremill.macwire._

abstract class ItemApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with CassandraPersistenceComponents
  with LagomKafkaComponents {

  override lazy val lagomServer = LagomServer.forServices(
    bindService[ItemService].to(wire[ItemServiceImpl])
  )
  lazy val itemRepository = wire[ItemRepository]
  lazy val biddingService = serviceClient.implement[BiddingService]

  persistentEntityRegistry.register(wire[ItemEntity])
  readSide.register(wire[ItemEventProcessor])
  wire[BiddingServiceSubscriber]

}

class ItemApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new ItemApplication(context) with LagomDevModeComponents

  override def load(context: LagomApplicationContext): LagomApplication = new ItemApplication(context) {
    override def serviceLocator: ServiceLocator = NoServiceLocator
  }
}
