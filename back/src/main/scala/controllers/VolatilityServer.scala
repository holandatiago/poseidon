package controllers

import assets.UnderlyingAsset
import cats.effect.{ExitCode, IO, IOApp}
import ch.qos.logback.classic.{Level, LoggerContext}
import com.comcast.ip4s.IpLiteralSyntax
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.{Executors, TimeUnit}
import scala.collection.concurrent.TrieMap

object VolatilityServer extends IOApp {
  val cache: TrieMap[String, UnderlyingAsset] = TrieMap()

  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "data" => Ok(cache.keySet.toList.sorted)
    case GET -> Root / "data" / asset => Ok(cache(asset))
  }

  def updateCache(): Unit = {
    ExchangeClient.fetchMarketPrices.foreach(asset => cache.put(asset.symbol, asset))
  }

  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF)

  def run(args: List[String]): IO[ExitCode] = {
    Executors
      .newSingleThreadScheduledExecutor()
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
