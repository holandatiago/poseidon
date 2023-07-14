package page

import com.raquo.laminar.api.L._

import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.JSConverters._

object Plot {
  val config = literal(displayModeBar = false, scrollZoom = true, responsive = true)

  val layout = literal(
    dragmode = "pan", margin = literal(b = 0, l = 0, r = 0, t = 0),
    xaxis = literal(automargin = true, range = js.Array(-1, 1), scaleanchor = "y"),
    yaxis = literal(automargin = true, range = js.Array(0, 2), constraintoward = "bottom"))

  val traces = js.Array(
    literal(name = "CALL", `type` = "scatter", mode = "markers", marker = literal(color = "green")),
    literal(name = "PUT", `type` = "scatter", mode = "markers", marker = literal(color = "blue")))

  def renderDataChart(): Element = {
    val plot = div(onMountCallback(nodeCtx => global.Plotly.newPlot(nodeCtx.thisNode.ref, traces, layout, config)))
    plot.amend(Client.optionsSignal --> { options =>
      val (calls, puts) = options.partition(_.side == "CALL")
      global.Plotly.animate(plot.ref, literal(data = js.Array(
        literal(x = calls.map(_.logMoneyness).toJSArray, y = calls.map(_.volatility).toJSArray,
          error_y = literal(array = calls.map(_.spread).toJSArray)),
        literal(x = puts.map(_.logMoneyness).toJSArray, y = puts.map(_.volatility).toJSArray,
          error_y = literal(array = puts.map(_.spread).toJSArray)))))
    })
  }
}
