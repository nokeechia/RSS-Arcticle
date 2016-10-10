package com.dbaktor.askdemo

import akka.actor.{Actor, ActorRef}
import akka.util.Timeout
import com.dbaktor.messages._
import akka.pattern.ask
import scala.concurrent.Future

/**
  * Created by Keech on 09/10/2016.
  */
class AskDemoArticleParser (cacheActorPath: String, httpClientActorPath: String,
                             articleParserActorPath: String,implicit val timeout: Timeout)
  extends Actor {
  val cacheActor = context.actorSelection(cacheActorPath)
  val httpClientActor = context.actorSelection(httpClientActorPath)
  val articleParserActor = context.actorSelection(articleParserActorPath)
  import scala.concurrent.ExecutionContext.Implicits.global

  override def receive: Receive = {
    case ParseArticle(uri) =>
      val senderRef = sender() //sender ref needed for closure

      val cacheResult = cacheActor ? GetRequest(uri) //ask cache actor

      val result = cacheResult.recoverWith { //if request fails, then ask the articleParseActor
        case _: Exception =>
          val fRawResult = httpClientActor ? uri

          fRawResult flatMap {
            case HttpResponse(rawArticle) =>
              articleParserActor ? ParseHtmlArticle(uri, rawArticle)
            case x =>
              Future.failed(new Exception("unknown response"))
          }
      }

      result onComplete { //could use Pipe (covered later)
        case scala.util.Success(x: String) =>
          println("cached result!")
          senderRef ! x //cached result
        case scala.util.Success(x: ArticleBody) =>
          cacheActor ! SetRequest(uri, x.body)
          senderRef ! x
        case scala.util.Failure(t) =>
          senderRef ! akka.actor.Status.Failure(t)
        case x =>
          println("unknown message! " + x)
      }
  }
}