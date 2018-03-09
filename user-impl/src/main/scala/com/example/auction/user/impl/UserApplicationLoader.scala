package com.example.auction.user.impl

import com.example.auction.user.api.UserService
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.lightbend.rp.servicediscovery.lagom.scaladsl.LagomServiceLocatorComponents
import play.api.libs.ws.ahc.AhcWSComponents

abstract class UserApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents
    with CassandraPersistenceComponents {

  override lazy val lagomServer = serverFor[UserService](wire[UserServiceImpl])
  override lazy val jsonSerializerRegistry = UserSerializerRegistry

  persistentEntityRegistry.register(wire[UserEntity])
}

class UserApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext) =
    new UserApplication(context) with LagomServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext) =
    new UserApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[UserService])
}
