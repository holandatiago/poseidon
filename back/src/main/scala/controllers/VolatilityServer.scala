package controllers

import assets.UnderlyingAsset
import breeze.linalg._
import breeze.optimize._
import cats.effect.{ExitCode, IO, IOApp}
import ch.qos.logback.classic.{Level, LoggerContext}
import com.comcast.ip4s.IpLiteralSyntax
import io.circe.generic.auto._
import models.Surface
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.concurrent.TrieMap

object VolatilityServer extends IOApp {
  val cache: TrieMap[String, UnderlyingAsset] = TrieMap()

  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "data" => Ok(cache.keySet)
    case GET -> Root / "data" / asset => Ok(cache(asset))
  }

  new Thread(() => while (true) {
    ExchangeClient.fetchMarketPrices
      .map(_.map(asset => asset.copy(bestSurface = calibrate(asset)))
        .foreach(asset => cache.put(asset.symbol, asset)))(ExchangeClient.executionContext)
    Thread.sleep(10000)
  }).start()

  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF)

  def run(args: List[String]): IO[ExitCode] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(service.orNotFound)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  def calibrate(asset: UnderlyingAsset): Surface = {
    val surface = if (asset.options.isEmpty) Surface.fromUnbounded(Array.fill(3)(0D))
    else Surface.fromUnbounded(minimizeBy(3)(Surface.fromUnbounded(_).rootMeanSquareError(asset)))
    println(s"Calibrated ${asset.symbol}\t${surface.rootMeanSquareError(asset)}\t$surface")
    surface
  }

  def minimizeBy(arraySize: Int)(objectiveFunction: Array[Double] => Double): Array[Double] = {
    val function = new ApproximateGradientFunction((x: DenseVector[Double]) => objectiveFunction(x.toArray))
    new LBFGS[DenseVector[Double]].minimize(function, DenseVector(Array.fill(arraySize)(0D))).toArray
  }
}
