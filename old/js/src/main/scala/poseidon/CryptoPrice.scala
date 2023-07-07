package poseidon

import org.scalajs.dom
import org.scalajs.dom.html
import scalatags.JsDom.all._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits._
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("CryptoPrice")
object CryptoPrice {
  val url = "https://api.binance.com/api/v3/ticker/price"
  val submitButton: html.Input = input(tpe := "button", value := "Submit").render
  val cryptoList: html.DataList = datalist(id := "cryptos").render
  val inputBox: html.Input = input(list := "cryptos").render
  val output: html.Div = div().render

  def fetchData(query: String = ""): Future[Any] = {
    dom.fetch(url + query).flatMap(_.json)
  }

  def updateCryptoList(json: Any): Unit = {
    json.asInstanceOf[js.Array[js.Dynamic]].map(_.symbol.toString)
      .map(s => option(value := s).render).foreach(cryptoList.appendChild)
  }

  def displayOutput(json: Any): Unit = {
    val jsonObj = json.asInstanceOf[js.Dynamic]
    val symbolName = jsonObj.symbol.toString
    val symbolPrice = jsonObj.price.asInstanceOf[Double]
    output.innerHTML = ""
    output.appendChild(p(s"The current price of $symbolName is $symbolPrice").render)
  }

  def submitRequest(e: dom.Event): Unit = {
    val inputData = inputBox.value
    if (cryptoList.options.map(_.getAttribute(value.name)).contains(inputData)) {
      fetchData("?symbol=" + inputData).foreach(displayOutput)
    } else {
      output.innerHTML = ""
      output.appendChild(p(s"There is no currency pair $inputData").render)
    }
  }

  @JSExport
  def main(target: html.Div): Unit = {
    fetchData().foreach(updateCryptoList)
    submitButton.onclick = submitRequest

    target.replaceWith(div(
      h1("Crypto Price"),
      p("Enter the name of a crypto pair to pull it's latest price from Binance!"),
      inputBox,
      cryptoList,
      submitButton,
      output).render)
  }
}
