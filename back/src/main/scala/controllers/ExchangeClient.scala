package controllers

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import models.{OptionContract, UnderlyingAsset}
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContext, Future}

object ExchangeClient extends DefaultJsonProtocol {
  val host: Uri = Uri("https://eapi.binance.com")
  val path: Uri.Path = Uri.Path./("eapi")./("v1")
  implicit val system: ActorSystem = ActorSystem()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private def makeRequest[T: RootJsonFormat](route: String, params: Map[String, Any] = Map()): Future[T] = {
    val uri = host.withPath(path./(route)).withQuery(Uri.Query(params.view.mapValues(_.toString).toMap))
    Http().singleRequest(HttpRequest(uri = uri)).flatMap {
      case response@HttpResponse(StatusCodes.OK, _, _, _) => Unmarshal(response).to[T].map { obj =>
        println(s"OK REQUEST => ${obj.toString.take(100)}")
        obj
      }
      case response => Unmarshal(response).to[JsValue].map { jsValue =>
        println(s"BAD REQUEST => ${jsValue.toString.take(100)}")
        jsValue.convertTo[T]
      }
    }
  }

  case class MarketDef(assetDefs: List[UnderlyingAsset], optionDefs: List[OptionContract], timestamp: Long)

  def fetchMarketDef: Future[MarketDef] = {
    implicit val assetDefCodec: RootJsonFormat[UnderlyingAsset] = lift((json: JsValue) => UnderlyingAsset(
      symbol = fromField[String](json, "underlying"),
      currencyPair = (fromField[String](json, "baseAsset"), fromField[String](json, "quoteAsset"))))
    implicit val optionDefCodec: RootJsonFormat[OptionContract] = lift((json: JsValue) => OptionContract(
      symbol = fromField[String](json, "symbol"),
      underlying = fromField[String](json, "underlying"),
      termTimestamp = fromField[Long](json, "expiryDate"),
      strike = fromField[BigDecimal](json, "strikePrice").doubleValue,
      side = fromField[String](json, "side")))
    implicit val marketDefCodec: RootJsonFormat[MarketDef] = lift((json: JsValue) => MarketDef(
      assetDefs = fromField[List[UnderlyingAsset]](json, "optionContracts"),
      optionDefs = fromField[List[OptionContract]](json, "optionSymbols"),
      timestamp = fromField[Long](json, "serverTime")))
    makeRequest[MarketDef]("exchangeInfo")
  }

  def fetchAssetPrice(symbol: String): Future[UnderlyingAsset] = {
    implicit val assetPriceCodec: RootJsonFormat[UnderlyingAsset] = lift((json: JsValue) => UnderlyingAsset(
      spot = fromField[BigDecimal](json, "indexPrice").doubleValue))
    makeRequest[UnderlyingAsset]("index", Map("underlying" -> symbol)).map(_.copy(symbol = symbol))
  }

  def fetchOptionVols: Future[List[OptionContract]] = {
    implicit val optionVolCodec: RootJsonFormat[OptionContract] = lift((json: JsValue) => OptionContract(
      symbol = fromField[String](json, "symbol"),
      volatility = (fromField[BigDecimal](json, "askIV") + fromField[BigDecimal](json, "bidIV")).doubleValue / 2,
      spread = (fromField[BigDecimal](json, "askIV") - fromField[BigDecimal](json, "bidIV")).doubleValue / 2))
    makeRequest[List[OptionContract]]("mark")
  }
}
