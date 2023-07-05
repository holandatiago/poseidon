package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import assets._
import spray.json._

import scala.concurrent._

object ExchangeClient extends DefaultJsonProtocol {
  val host: Uri = Uri("https://eapi.binance.com")
  val path: Uri.Path = Uri.Path./("eapi")./("v1")
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private def makeRequest[T: RootJsonFormat](route: String, params: Map[String, Any] = Map()): Future[T] = {
    val uri = host.withPath(path./(route)).withQuery(Uri.Query(params.view.mapValues(_.toString).toMap))
    Http().singleRequest(HttpRequest(uri = uri)).flatMap(Unmarshal(_).to[T])
  }

  case class MarketInfo(underlyingInfo: List[UnderlyingAsset], optionInfo: List[OptionAsset], timestamp: Long)

  def fetchMarketPrices: Future[List[UnderlyingAsset]] = {
    val marketInfoFuture = fetchMarketInfo
    val volsFuture = fetchOptionPrices
    val spotsFuture = marketInfoFuture
      .flatMap(m => Future.sequence(m.underlyingInfo.map(a => fetchUnderlyingPrice(a.symbol))))
    for (marketInfo <- marketInfoFuture; vols <- volsFuture; spots <- spotsFuture) yield {
      val spotsMap = spots.map(a => a.symbol -> a.spot).toMap
      val volsMap = vols.groupBy(_.symbol).view.mapValues(_.head).toMap
      val options = marketInfo.optionInfo.map(option => (option, volsMap(option.symbol)))
        .map { case (option, optionVol) => option.copy(volatility = optionVol.volatility, spread = optionVol.spread) }
        .groupBy(_.underlying).view.mapValues(_.filter(_.isValid).sortBy(_.symbol)).toMap.withDefaultValue(Nil)
      val underlyings = marketInfo.underlyingInfo.sortBy(_.symbol)
        .map(asset => asset.copy(spot = spotsMap(asset.symbol), currentTimestamp = marketInfo.timestamp))
        .map(a => a.copy(options = options(a.symbol).map(_.withTimeToExpiry(a).withLogMoneyness(a))))
      underlyings
    }
  }

  def fetchMarketInfo: Future[MarketInfo] = {
    implicit val underlyingInfoCodec: RootJsonFormat[UnderlyingAsset] = lift((json: JsValue) => UnderlyingAsset(
      symbol = fromField[String](json, "underlying"),
      currencyPair = (fromField[String](json, "baseAsset"), fromField[String](json, "quoteAsset"))))
    implicit val optionInfoCodec: RootJsonFormat[OptionAsset] = lift((json: JsValue) => OptionAsset(
      symbol = fromField[String](json, "symbol"),
      underlying = fromField[String](json, "underlying"),
      termTimestamp = fromField[Long](json, "expiryDate"),
      strike = fromField[BigDecimal](json, "strikePrice").doubleValue,
      side = fromField[String](json, "side")))
    implicit val marketInfoCodec: RootJsonFormat[MarketInfo] = lift((json: JsValue) => MarketInfo(
      underlyingInfo = fromField[List[UnderlyingAsset]](json, "optionContracts"),
      optionInfo = fromField[List[OptionAsset]](json, "optionSymbols"),
      timestamp = fromField[Long](json, "serverTime")))
    makeRequest[MarketInfo]("exchangeInfo")
  }

  def fetchUnderlyingPrice(underlying: String): Future[UnderlyingAsset] = {
    implicit val underlyingPriceCodec: RootJsonFormat[UnderlyingAsset] = lift((json: JsValue) => UnderlyingAsset(
      spot = fromField[BigDecimal](json, "indexPrice").doubleValue))
    makeRequest[UnderlyingAsset]("index", Map("underlying" -> underlying)).map(_.copy(symbol = underlying))
  }

  def fetchOptionPrices: Future[List[OptionAsset]] = {
    implicit val optionPriceCodec: RootJsonFormat[OptionAsset] = lift((json: JsValue) => OptionAsset(
      symbol = fromField[String](json, "symbol"),
      volatility = (fromField[BigDecimal](json, "askIV") + fromField[BigDecimal](json, "bidIV")).doubleValue / 2,
      spread = (fromField[BigDecimal](json, "askIV") - fromField[BigDecimal](json, "bidIV")).doubleValue / 2))
    makeRequest[List[OptionAsset]]("mark")
  }
}
