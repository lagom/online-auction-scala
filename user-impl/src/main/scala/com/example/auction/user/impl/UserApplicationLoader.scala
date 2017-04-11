package com.example.auction.user.impl

import com.example.auction.user.api.UserService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.typesafe.conductr.bundlelib.lagom.scaladsl.ConductRApplicationComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class UserApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with CassandraPersistenceComponents {

  override lazy val lagomServer = LagomServer.forServices(
    bindService[UserService].to(wire[UserServiceImpl])
  )
  override lazy val jsonSerializerRegistry = UserSerializerRegistry

  persistentEntityRegistry.register(wire[UserEntity])
}

class UserApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new UserApplication(context) with ConductRApplicationComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new UserApplication(context) with LagomDevModeComponents

  override def describeServices = List(
    readDescriptor[UserService]
  )
}
