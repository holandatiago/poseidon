package page

import com.raquo.airstream.web.AjaxStream
import com.raquo.laminar.api.L._
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode
import models.UnderlyingAsset
import org.scalajs.dom

import scala.scalajs.js

object Client {
  val assetVar: Var[Option[UnderlyingAsset]] = Var(None)
  val termVar: Var[Option[Long]] = Var(None)

  val termsSignal: Signal[List[Long]] = assetVar.signal.map { asset =>
    asset.toList.flatMap(_.options).map(_.termTimestamp).distinct.sorted
  }

  val optionsSignal: Signal[Option[(UnderlyingAsset, Long)]] = assetVar.signal.combineWith(termVar).map {
    case (Some(asset), Some(term)) => Some((asset, term))
    case _ => None
  }

  def main(args: Array[String]): Unit = {
    renderOnDomContentLoaded(dom.document.body, appElement())
  }

  def appElement(): HtmlElement = {
    div(
      h1("Cryptocurrency Volatility Surface"),
      select(
        children <-- fetchData[List[String]]().map(_.toList.flatten.map(asset => option(value := asset, asset))),
        onChange.mapToValue.flatMap(fetchData[UnderlyingAsset](_)) --> assetVar,
        termsSignal.map(_.headOption) --> termVar),
      select(
        children <-- termsSignal.map(_.map(ts => option(value := ts.toString, new js.Date(ts).toISOString.take(10)))),
        onChange.mapToValue.map(Some(_).map(_.toLong)) --> termVar),
      Plot.renderDataChart())
  }

  def fetchData[T: Decoder](path: String = null): Signal[Option[T]] = {
    AjaxStream.get(List("data").appendedAll(Option(path)).mkString("/"))
      .map(_.responseText).map(decode[T](_).toOption).toSignal(None)
  }
}
