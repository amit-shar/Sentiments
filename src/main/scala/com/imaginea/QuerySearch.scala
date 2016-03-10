package com.imaginea

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.typesafe.config.ConfigFactory
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import twitter4j.{Query, Status, TwitterFactory, TwitterObjectFactory}

import scala.collection.JavaConversions._

case class TweetsSentiment(tweet: Status, sentiment: Double)

trait TwitterInstance {
  val twitter = new TwitterFactory(ConfigurationBuilderUtil.buildConfiguration).getInstance()
}


object QuerySearch extends TwitterInstance {
  val host = "127.0.1.1"
  val esport = 9300
  val transportClient = new TransportClient(ImmutableSettings.settingsBuilder().put("cluster.name", "elasticsearch")
    .put("client.transport.sniff", true).build())
  val client = transportClient.addTransportAddress(new InetSocketTransportAddress(host, esport))

  val config = ConfigFactory.load("application.conf")
  val actorSystem = ActorSystem("twitter", config)


  def main(args: Array[String]): Unit = {
    val router: ActorRef =
      actorSystem.actorOf(RoundRobinPool(2 * Runtime.getRuntime.availableProcessors()).props(Props[TwitterQueryFetcher]), "router")

  }

  def fetchAndSaveTweets(term: String): Unit = {
    val bulkRequest = client.prepareBulk()
    var query = new Query(term).lang("en")
    query.setCount(100)
    var queryResult = twitter.search(query)
    var tweetCount = 0

    while (queryResult.hasNext) {
      tweetCount = tweetCount + queryResult.getCount
      queryResult.getTweets.foreach {
        x =>
          bulkRequest.add(client.prepareIndex("twitter", "tweet").setSource(TwitterObjectFactory.getRawJSON(x)))
      }
      query = queryResult.nextQuery()
      queryResult = twitter.search(query)
    }
    bulkRequest.execute()
    println("count " + tweetCount, "term " + term)
  }

  def toJson[T <: AnyRef <% Product with Serializable](t: T, addESHeader: Boolean = true,
                                                       isToken: Boolean = false): String = {
    import org.json4s._
    import org.json4s.jackson.Serialization
    import org.json4s.jackson.Serialization.write
    implicit val formats = Serialization.formats(NoTypeHints)
    val indexName = t.productPrefix.toLowerCase
    if (addESHeader && isToken) {
      """|{ "index" : { "_index" : "twitter", "_type" : "custom" } }
        | """.stripMargin + write(t)
    } else if (addESHeader) {
      s"""|{ "index" : { "_index" : "$indexName", "_type" : "type$indexName" } }
                                                                             |""".stripMargin + write(t)
    } else "" + write(t)
  }
}


case class QueryTwitter(term: String , actorRef: ActorRef)
case object Done

class TwitterQueryFetcher extends Actor {
  override def receive: Receive = {
    case QueryTwitter(term, actor) => QuerySearch.fetchAndSaveTweets(term)
      actor ! Done
  }
}