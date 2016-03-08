package com.imaginea

import akka.actor.{Props, ActorSystem, ActorRef}
import akka.routing.{RoundRobinPool, RoundRobinGroup}
import com.typesafe.config.ConfigFactory

object MasterApp {
  def getRemoteActorPath(ip :String) = (s"akka.tcp://twitter@$ip:2552/user/router")
  val config = ConfigFactory.load("master.conf")
  val actorSystem = ActorSystem("twitter", config)

  def main(args: Array[String]) {
    val ips = "127.0.0.1".split(",")
    val paths = ips.map(getRemoteActorPath(_)).toList

    println(paths.toList)

    val remoteRouter: ActorRef =
      actorSystem.actorOf(RoundRobinGroup(paths).props())
    List("Obama", "Steve Jobs").foreach(term => remoteRouter ! QueryTwitter(term))
  }
}
