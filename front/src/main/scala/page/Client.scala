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
    render(dom.document.head, titleTag("Nettuno"))
    render(dom.document.head, linkTag(rel := "icon", tpe := "image/x-icon", href := "trident-black.png"))
    renderOnDomContentLoaded(dom.document.body, appElement())
  }

  def appElement(): HtmlElement = {
    div(
      navbar(),
      div(cls := "container-fluid",
        div(cls := "row vh-100",
          sidebar(),
          plotCanvas())),
      )
  }

  def navbar(): HtmlElement = {
    div(cls := "navbar sticky-top bg-dark shadow p-1",
      a(cls := "navbar-brand text-white d-flex align-items-center gap-2 px-3 fs-7", href := "",
        img(width := "32", height := "32", src := "trident-white.png"), "NETTUNO"))
  }

  def sidebar(): HtmlElement = {
    div(cls := "sidebar bg-body-tertiary border border-right col-md-3 col-lg-2 p-1",
      ul(cls := "nav flex-column",
        children <-- fetchData[List[String]]().map(_.toList.flatten.map(createListItem)),
        termsSignal.map(_.headOption) --> termVar))
  }

  def createListItem(symbol: String): HtmlElement = {
    val currency = symbol.dropRight(4).toLowerCase
    li(cls := "nav-item",
      input(tpe := "radio", cls := "btn-check", nameAttr := "assets", idAttr := symbol,
        onChange.mapTo(symbol).flatMap(fetchData[UnderlyingAsset](_)) --> assetVar, checked(symbol == "BTCUSDT"),
        (if (symbol == "BTCUSDT") fetchData[UnderlyingAsset](symbol) else Signal.fromValue(None)) --> assetVar),
      label(cls := "nav-link btn btn-secondary d-flex align-items-center gap-2", forId := symbol,
        img(src := s"https://raw.githubusercontent.com/spothq/cryptocurrency-icons/master/32/color/$currency.png"),
        symbol))
  }

  def plotCanvas(): HtmlElement = {
    div(cls := "col-md-9 ms-sm-auto col-lg-10 px-md-4",
      div(cls := "d-flex justify-content-between flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom",
        h1(cls := "h2", "Volatility Smile"),
        div(cls := "d-flex align-items-center gap-2",
          label(forId := "expiryDate", "Expiry:"),
          select(cls := "form-select", idAttr := "expiryDate",
            children <-- termsSignal.map(_.map(createSelectOption)),
            onChange.mapToValue.map(Some(_).map(_.toLong)) --> termVar))),
      Plot.renderDataChart(),
      div(cls := "d-flex justify-content-between flex-md-wrap align-items-center",
        a(cls := "text-muted", "Data provided by Binance", target := "_blank",
          href <-- optionsSignal.map(_.map(createDataProvidedText).getOrElse(""))),
        a(cls := "text-muted link-underline link-underline-opacity-0",
          child.text <-- assetVar.signal.map(_.map(createLastUpdatedText).getOrElse("")))))
  }

  def createSelectOption(ts: Long): HtmlElement = {
    option(value := ts.toString, new js.Date(ts).toISOString.take(10))
  }

  def fetchData[T: Decoder](path: String = null): Signal[Option[T]] = {
    AjaxStream.get(List("data").appendedAll(Option(path)).mkString("/"))
      .map(_.responseText).map(decode[T](_).toOption).toSignal(None)
  }

  def createDataProvidedText(pair: (UnderlyingAsset, Long)): String = pair match { case (asset, term) =>
    s"https://www.binance.com/en/eoptions/${asset.symbol}?expirationDate=${new js.Date(term).toISOString.take(10)}"
  }

  def createLastUpdatedText(asset: UnderlyingAsset): String = {
    s"Last updated at ${new js.Date(asset.currentTimestamp).toISOString}"
  }
}
