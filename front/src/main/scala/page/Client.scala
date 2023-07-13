package page

import com.raquo.airstream.web.AjaxStream
import com.raquo.laminar.api.L._
import io.circe.generic.auto._
import io.circe.parser.decode
import models.UnderlyingAsset
import org.scalajs.dom

import scala.scalajs.js

object Client {
  def main(args: Array[String]): Unit = {
    renderOnDomContentLoaded(dom.document.body, appElement())
  }

  def appElement(): HtmlElement = {
    val termsVar = Var[List[Long]](Nil)

    div(
      h1("Cryptocurrency Volatility Surface"),
      p(
        "Underlying Asset",
        select(
          children <-- AjaxStream.get("data").map(_.responseText)
            .map(decode[List[String]](_).toTry.get)
            .map(_.map(asset => option(value := asset, asset))),
          onChange.mapToValue.flatMapStream(asset => AjaxStream.get("data/" + asset).map(_.responseText)
            .map(decode[UnderlyingAsset](_).toTry.get.options.map(_.termTimestamp).distinct.sorted)) --> termsVar
        )
      ),
      p(
        "Option Term",
        select(
          children <-- termsVar.signal
            .map(_.map(ts => option(value := ts.toString, new js.Date(ts).toISOString().take(10)))),
        )
      )
    )
  }
}
