package simple

import scalatags.Text.TypedTag
import scalatags.Text.all._

object Page {
  val skeleton: TypedTag[String] =
    html(
      head(link(rel := "stylesheet", href := "https://cdnjs.cloudflare.com/ajax/libs/pure/0.5.0/pure-min.css")),
      body(script(src := "main.js")))
}
