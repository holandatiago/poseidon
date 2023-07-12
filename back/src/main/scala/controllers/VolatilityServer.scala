package controllers

import cats.effect.{ExitCode, IO, IOApp}
import ch.qos.logback.classic.{Level, LoggerContext}
import com.comcast.ip4s.IpLiteralSyntax
import io.circe.generic.auto._
import models.UnderlyingAsset
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.scalatags._
import org.http4s.{HttpRoutes, StaticFile}
import org.slf4j.{Logger, LoggerFactory}
import scalatags.Text.TypedTag
import scalatags.Text.all._

import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}
import scala.jdk.CollectionConverters._

object VolatilityServer extends IOApp {
  val cache = new ConcurrentHashMap[String, UnderlyingAsset]()

  val indexPage: TypedTag[String] =
    html(
      head(link(rel := "stylesheet", href := "https://cdnjs.cloudflare.com/ajax/libs/pure/0.5.0/pure-min.css")),
      body(script(src := "main.js")))

  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root => Ok(indexPage)
    case GET -> Root / file => StaticFile.fromResource[IO](file).getOrElseF(NotFound())
    case GET -> Root / "data" => Ok(cache.keySet.asScala.toList.sorted)
    case GET -> Root / "data" / asset => Option(cache.get(asset)).map(Ok(_)).getOrElse(NotFound())
  }

  def updateCache(): Unit = {
    MarketService.fetchMarketPrices.foreach(asset => cache.compute(asset.symbol, (_, oldAsset) =>
      Option(oldAsset).filter(_.currentTimestamp > asset.currentTimestamp).getOrElse(asset)))
  }

  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF)

  def run(args: List[String]): IO[ExitCode] = {
    Executors
      .newScheduledThreadPool(1)
      .scheduleAtFixedRate(() => updateCache(), 0, 10, TimeUnit.SECONDS)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(service.orNotFound)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
