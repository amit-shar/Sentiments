package com.imaginea

import akka.actor.Actor
import akka.actor.{Props, ActorSystem, ActorRef}
import akka.routing.{RoundRobinGroup}
import com.typesafe.config.ConfigFactory

object MasterApp {
  def getRemoteActorPath(ip :String) = (s"akka.tcp://twitter@$ip:2552/user/router")
  val config = ConfigFactory.load("master.conf")
  val actorSystem = ActorSystem("twitter", config)

  def main(args: Array[String]) {
    val ips = "127.0.0.1".split(",")
    val paths = ips.map(getRemoteActorPath(_)).toList

    val remoteRouter: ActorRef =
      actorSystem.actorOf(RoundRobinGroup(paths).props())

    val aggregator: ActorRef = actorSystem.actorOf(Props(classOf[Aggregator], remoteRouter))

    List("Obama", "Steve Jobs").foreach(term => aggregator ! term)
  }
}

class Aggregator(remoteRouter: ActorRef) extends Actor {
  var sendCount = 0
  var receiveCount = 0
  def receive :Receive = {
    case term: String => remoteRouter ! QueryTwitter(term, self)
      sendCount = sendCount + 1
    case Done => receiveCount = receiveCount + 1
      if(receiveCount == sendCount) {
        println("done")
        Launcher.runSparkJob("/opt/software/spark-1.3.1-bin-hadoop2.6/",
          "/home/piyushm/Sentiments/target/scala-2.11/sentiment-assembly-1.0.jar",
          "com.imaginea.WordCount", "spark://PRINHYLTPDL0378:7077")
      }
  }
}