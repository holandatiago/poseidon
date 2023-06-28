package poseidon

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import poseidon.Models._
import spray.json._

import java.time._
import scala.concurrent._
import scala.concurrent.duration._

object Client extends DefaultJsonProtocol {
  val host: Uri = Uri("https://eapi.binance.com")
  val path: Uri.Path = Uri.Path./("eapi")./("v1")
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private def makeRequest[T: RootJsonFormat](route: String, params: Map[String, Any] = Map()): T = {
    val uri = host.withPath(path./(route)).withQuery(Uri.Query(params.view.mapValues(_.toString).toMap))
    Await.result(Http().singleRequest(HttpRequest(uri = uri)).flatMap(Unmarshal(_).to[T]), (30, SECONDS))
  }

  def fetchMarketPrices: List[UnderlyingAsset] = {
    val marketInfo = fetchMarketInfo
    val optionVols = fetchOptionPrices.groupBy(_.symbol).view.mapValues(_.head).toMap
    val options = marketInfo.optionInfo.map(option => (option, optionVols(option.symbol)))
      .map { case (option, optionVol) => option.copy(volatility = optionVol.volatility, spread = optionVol.spread) }
      .groupBy(_.underlying).view.mapValues(_.filter(_.isValid).sortBy(_.symbol)).toMap.withDefaultValue(Nil)
    val underlyings = marketInfo.underlyingInfo.sortBy(_.symbol)
      .map(asset => asset.copy(spot = fetchUnderlyingPrice(asset.symbol).spot, currentTime = marketInfo.currentTime))
    underlyings.foreach { asset => asset.options = options(asset.symbol); asset.options.foreach(_.asset = asset) }
    underlyings
  }

  def fetchMarketInfo: MarketInfo = {
    implicit val underlyingInfoCodec: RootJsonFormat[UnderlyingAsset] = lift((json: JsValue) => UnderlyingAsset(
      symbol = fromField[String](json, "underlying"),
      baseAsset = fromField[String](json, "baseAsset"),
      quoteAsset = fromField[String](json, "quoteAsset"),
      spot = Double.NaN,
      interestRate = 0D,
      currentTime = null))
    implicit val optionInfoCodec: RootJsonFormat[OptionAsset] = lift((json: JsValue) => OptionAsset(
      symbol = fromField[String](json, "symbol"),
      underlying = fromField[String](json, "underlying"),
      term = LocalDate.ofInstant(Instant.ofEpochMilli(fromField[Long](json, "expiryDate")), ZoneOffset.UTC),
      strike = fromField[BigDecimal](json, "strikePrice").doubleValue,
      side = List(OptionSide.CALL, OptionSide.PUT).find(_.toString == fromField[String](json, "side")).get,
      volatility = Double.NaN,
      spread = Double.NaN))
    implicit val marketInfoCodec: RootJsonFormat[MarketInfo] = lift((json: JsValue) => MarketInfo(
      underlyingInfo = fromField[List[UnderlyingAsset]](json, "optionContracts"),
      optionInfo = fromField[List[OptionAsset]](json, "optionSymbols"),
      currentTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(fromField[Long](json, "serverTime")), ZoneOffset.UTC)))
    makeRequest[MarketInfo]("exchangeInfo")
  }

  def fetchUnderlyingPrice(underlying: String): UnderlyingAsset = {
    implicit val underlyingPriceCodec: RootJsonFormat[UnderlyingAsset] = lift((json: JsValue) => UnderlyingAsset(
      symbol = null,
      baseAsset = null,
      quoteAsset = null,
      spot = fromField[BigDecimal](json, "indexPrice").doubleValue,
      interestRate = Double.NaN,
      currentTime = null))
    makeRequest[UnderlyingAsset]("index", Map("underlying" -> underlying)).copy(symbol = underlying)
  }

  def fetchOptionPrices: List[OptionAsset] = {
    implicit val optionPriceCodec: RootJsonFormat[OptionAsset] = lift((json: JsValue) => OptionAsset(
      symbol = fromField[String](json, "symbol"),
      underlying = null,
      term = null,
      strike = Double.NaN,
      side = null,
      volatility = (fromField[BigDecimal](json, "askIV") + fromField[BigDecimal](json, "bidIV")).doubleValue / 2,
      spread = (fromField[BigDecimal](json, "askIV") - fromField[BigDecimal](json, "bidIV")).doubleValue / 2))
    makeRequest[List[OptionAsset]]("mark")
  }
}
