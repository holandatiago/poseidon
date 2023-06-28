package simple

import autowire._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html
import scalatags.JsDom.all._

import scala.scalajs.concurrent.JSExecutionContext.Implicits._
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("Client")
object Client {
  val inputBox = input.render
  val outputBox = ul.render
  val ajaxer: ClientServer.Ajaxer = (path, data) => Ajax.post(path.mkString("/"), data).map(_.responseText)

  def update() = ajaxer[Api].list(inputBox.value).call().foreach { data =>
    outputBox.innerHTML = ""
    for (FileData(name, size) <- data) {
      outputBox.appendChild(li(b(name), " - ", size, " bytes").render)
    }
  }

  @JSExport
  def main(container: html.Div) = {
    inputBox.onkeyup = (_: dom.Event) => update()
    update()
    container.appendChild(
      div(
        h1("File Search"),
        inputBox,
        outputBox
      ).render
    )
  }
}
