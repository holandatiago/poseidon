package page

import com.raquo.airstream.web.AjaxStream
import com.raquo.laminar.api.L._
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode
import models.{OptionContract, UnderlyingAsset}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.JSConverters._

object Client {
  val assetVar: Var[Option[UnderlyingAsset]] = Var(None)
  val termVar: Var[Option[Long]] = Var(None)

  val termsSignal: Signal[List[Long]] = assetVar.signal.map { asset =>
    asset.toList.flatMap(_.options).map(_.termTimestamp).distinct.sorted
  }

  val optionsSignal: Signal[List[OptionContract]] = assetVar.signal.combineWith(termVar).map {
    case (Some(asset), Some(term)) => asset.options.filter(_.termTimestamp == term)
    case _ => Nil
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
      renderDataChart())
  }

  def fetchData[T: Decoder](path: String = null): Signal[Option[T]] = {
    AjaxStream.get(List("data").appendedAll(Option(path)).mkString("/"))
      .map(_.responseText).map(decode[T](_).toOption).toSignal(None)
  }

  def renderDataChart(): Element = {
    val plot = div(onMountCallback { nodeCtx =>
      global.Plotly.newPlot(nodeCtx.thisNode.ref, js.Array(
        literal(name = "CALL", `type` = "scatter", mode = "markers", marker = literal(color = "green")),
        literal(name = "PUT", `type` = "scatter", mode = "markers", marker = literal(color = "blue"))))
    })
    plot.amend(optionsSignal --> { options =>
      val (calls, puts) = options.partition(_.side == "CALL")
      global.Plotly.animate(plot.ref, literal(data = js.Array(
        literal(x = calls.map(_.logMoneyness).toJSArray, y = calls.map(_.volatility).toJSArray,
          error_y = literal(array = calls.map(_.spread).toJSArray)),
        literal(x = puts.map(_.logMoneyness).toJSArray, y = puts.map(_.volatility).toJSArray,
          error_y = literal(array = puts.map(_.spread).toJSArray)))))
    })
  }
}
