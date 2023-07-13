package page

import com.raquo.laminar.api.L._
import org.scalajs.dom

object Client {
  def main(args: Array[String]): Unit = {
    renderOnDomContentLoaded(dom.document.body, appElement())
  }

  def appElement(): Element = {
    div(
      h1("File Search"),
    )
  }
}
