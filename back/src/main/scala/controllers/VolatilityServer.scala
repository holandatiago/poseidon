package controllers

import cats.effect.{ExitCode, IO, IOApp}
//import ch.qos.logback.classic.{Level, LoggerContext}
import com.comcast.ip4s.IpLiteralSyntax
import io.circe.generic.auto._
import models.UnderlyingAsset
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.scalatags._
import org.http4s.{HttpRoutes, StaticFile}
//import org.slf4j.{Logger, LoggerFactory}
import com.comcast.ip4s.Port
import scalatags.Text.TypedTag
import scalatags.Text.all._

import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}
import scala.jdk.CollectionConverters._

object VolatilityServer extends IOApp {
  val cache = new ConcurrentHashMap[String, UnderlyingAsset]()

  val indexPage: TypedTag[String] = {
    html(
      lang := "en",
      head(
        meta(charset := "utf-8"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1"),
        script(
          src := "https://cdn.plot.ly/plotly-basic-2.24.2.min.js"),
        script(
          src := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js",
          integrity := "sha384-geWF76RCwLtnZ8qwWowPQNguL3RmwHVBC9FhGdlKrxdiJJigb/j/68SIy3Te4Bkz",
          crossorigin := "anonymous"),
        link(
          rel := "stylesheet",
          href := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css",
          integrity := "sha384-9ndCyUaIbzAi2FUVXJi0CjmCapSmO7SnpJef0486qhLnuZ2cdeRhO02iuK6FUUVM",
          crossorigin := "anonymous")),
      body(script(src := "main.js")))
  }

  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root => Ok(indexPage)
    case GET -> Root / "data" => Ok(cache.keySet.asScala.toList.sorted)
    case GET -> Root / file => StaticFile.fromResource[IO](file).getOrElseF(NotFound())
    case GET -> Root / "data" / asset => Option(cache.get(asset)).map(Ok(_)).getOrElse(NotFound())
  }

  def updateCache(): Unit = {
    MarketService.fetchMarketPrices.foreach(asset => cache.compute(asset.symbol, (_, oldAsset) =>
      Option(oldAsset).filter(_.currentTimestamp > asset.currentTimestamp).getOrElse(asset)))
  }

//  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF)

  def run(args: List[String]): IO[ExitCode] = {
    Executors
//      .newSingleThreadExecutor()
//      .submit((() => updateCache()): Runnable)
      .newSingleThreadScheduledExecutor()
      .scheduleAtFixedRate(() => updateCache(), 0, 10, TimeUnit.SECONDS)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromString(sys.env.getOrElse("PORT", "8080")).get)
      .withHttpApp(service.orNotFound)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
