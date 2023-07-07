package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import breeze.linalg.DenseVector
import breeze.optimize.{ApproximateGradientFunction, LBFGS}
import models.{Assets, Surface}
import spray.json._

import scala.concurrent._
import scala.concurrent.duration.Duration

object ExchangeClient extends DefaultJsonProtocol {
  val host: Uri = Uri("https://eapi.binance.com")
  val path: Uri.Path = Uri.Path./("eapi")./("v1")
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private def makeRequest[T: RootJsonFormat](route: String, params: Map[String, Any] = Map()): Future[T] = {
    val uri = host.withPath(path./(route)).withQuery(Uri.Query(params.view.mapValues(_.toString).toMap))
    Http().singleRequest(HttpRequest(uri = uri)).flatMap(Unmarshal(_).to[T])
  }

  case class MarketInfo(underlyingInfo: List[Assets.Underlying], optionInfo: List[Assets.Option], timestamp: Long)

  def fetchMarketPrices: List[Assets.Underlying] = {
    val marketInfoFuture = fetchMarketInfo
    val volsFuture = fetchOptionPrices
    val spotsFuture = marketInfoFuture
      .flatMap(m => Future.sequence(m.underlyingInfo.map(a => fetchUnderlyingPrice(a.symbol))))
    Await.result(for (marketInfo <- marketInfoFuture; vols <- volsFuture; spots <- spotsFuture) yield {
      val spotsMap = spots.map(a => a.symbol -> a.spot).toMap
      val volsMap = vols.groupBy(_.symbol).view.mapValues(_.head).toMap
      val options = marketInfo.optionInfo.map(option => (option, volsMap(option.symbol)))
        .map { case (option, optionVol) => option.copy(volatility = optionVol.volatility, spread = optionVol.spread) }
        .groupBy(_.underlying).withDefaultValue(Nil)
      val underlyings = marketInfo.underlyingInfo.sortBy(_.symbol)
        .map(asset => asset.copy(spot = spotsMap(asset.symbol), currentTimestamp = marketInfo.timestamp))
        .map(asset => asset.withOptionList(options(asset.symbol))).filter(_.isValid)
        .map(asset => asset.copy(bestSurface = surfaceOptimizer.calibrate(asset.options)))
      underlyings.foreach(printAsset)
      underlyings
    }, Duration.Inf)
  }

  def printAsset(asset: Assets.Underlying): Unit = {
    val surface = asset.bestSurface
    println(s"Calibrated ${asset.symbol}\t${surface.rootMeanSquareError(asset.options)}\t$surface")
  }

  val surfaceOptimizer: Surface.Optimizer = (startArray, objectiveFunction) => {
    val function = new ApproximateGradientFunction((x: DenseVector[Double]) => objectiveFunction(x.toArray))
    new LBFGS[DenseVector[Double]].minimize(function, DenseVector(startArray)).toArray
  }

  def fetchMarketInfo: Future[MarketInfo] = {
    implicit val underlyingInfoCodec: RootJsonFormat[Assets.Underlying] = lift((json: JsValue) => Assets.Underlying(
      symbol = fromField[String](json, "underlying"),
      currencyPair = (fromField[String](json, "baseAsset"), fromField[String](json, "quoteAsset"))))
    implicit val optionInfoCodec: RootJsonFormat[Assets.Option] = lift((json: JsValue) => Assets.Option(
      symbol = fromField[String](json, "symbol"),
      underlying = fromField[String](json, "underlying"),
      termTimestamp = fromField[Long](json, "expiryDate"),
      strike = fromField[BigDecimal](json, "strikePrice").doubleValue,
      side = fromField[String](json, "side")))
    implicit val marketInfoCodec: RootJsonFormat[MarketInfo] = lift((json: JsValue) => MarketInfo(
      underlyingInfo = fromField[List[Assets.Underlying]](json, "optionContracts"),
      optionInfo = fromField[List[Assets.Option]](json, "optionSymbols"),
      timestamp = fromField[Long](json, "serverTime")))
    makeRequest[MarketInfo]("exchangeInfo")
  }

  def fetchUnderlyingPrice(underlying: String): Future[Assets.Underlying] = {
    implicit val underlyingPriceCodec: RootJsonFormat[Assets.Underlying] = lift((json: JsValue) => Assets.Underlying(
      spot = fromField[BigDecimal](json, "indexPrice").doubleValue))
    makeRequest[Assets.Underlying]("index", Map("underlying" -> underlying)).map(_.copy(symbol = underlying))
  }

  def fetchOptionPrices: Future[List[Assets.Option]] = {
    implicit val optionPriceCodec: RootJsonFormat[Assets.Option] = lift((json: JsValue) => Assets.Option(
      symbol = fromField[String](json, "symbol"),
      volatility = (fromField[BigDecimal](json, "askIV") + fromField[BigDecimal](json, "bidIV")).doubleValue / 2,
      spread = (fromField[BigDecimal](json, "askIV") - fromField[BigDecimal](json, "bidIV")).doubleValue / 2))
    makeRequest[List[Assets.Option]]("mark")
  }
}
