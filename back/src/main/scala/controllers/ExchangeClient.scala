package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import models.Assets
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object ExchangeClient extends DefaultJsonProtocol {
  val host: Uri = Uri("https://eapi.binance.com")
  val path: Uri.Path = Uri.Path./("eapi")./("v1")
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private def makeRequest[T: RootJsonFormat](route: String, params: Map[String, Any] = Map()): Future[T] = {
    val uri = host.withPath(path./(route)).withQuery(Uri.Query(params.view.mapValues(_.toString).toMap))
    Http().singleRequest(HttpRequest(uri = uri)).flatMap(Unmarshal(_).to[T])
  }

  case class MarketInfo(underlyingInfo: List[Assets.Underlying], optionInfo: List[Assets.Option], timestamp: Long)

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
