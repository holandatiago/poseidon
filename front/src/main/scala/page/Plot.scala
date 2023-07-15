package page

import com.raquo.laminar.api.L._

import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.JSConverters._

object Plot {
  val config = literal(displayModeBar = false, scrollZoom = true, responsive = true)

  val layout = literal(
    title = literal(text = "Volatility Smile", automargin = true),
    dragmode = "pan", margin = literal(b = 0, l = 0, r = 0, t = 0),
    xaxis = literal(title = "logMoneyness", automargin = true, range = js.Array(-1, 1), scaleanchor = "y", scaleratio = 2),
    yaxis = literal(title = "volatility", automargin = true, range = js.Array(0, 2), autorange = true, constraintoward = "bottom"))

  val traces = js.Array(
    literal(name = "CALL", `type` = "scatter", mode = "markers", marker = literal(color = "seagreen"),
      error_y = literal(color = "seagreen", opacity = .5), hoverinfo = "text"),
    literal(name = "PUT", `type` = "scatter", mode = "markers", marker = literal(color = "royalblue"),
      error_y = literal(color = "royalblue", opacity = .5), hoverinfo = "text"),
    literal(name = "fitted", `type` = "scatter", mode = "lines",
      line = literal(color = "orangered", shape = "spline"), hoverinfo = "x+y"))

  def renderDataChart(): Element = {
    val plot = div(onMountCallback(nodeCtx => global.Plotly.newPlot(nodeCtx.thisNode.ref, traces, layout, config)))
    plot.amend(Client.optionsSignal --> (_.foreach { case (asset, term) =>
      val (calls, puts) = asset.options.filter(_.termTimestamp == term).toJSArray.partition(_.side == "CALL")
      val timeToExpiry = (term - asset.currentTimestamp) / (365 * 24 * 60 * 60 * 1000D)
      val range = Range.BigDecimal.inclusive(-1, 1, .01).toJSArray.map(_.toDouble)
      val fitted = range.map(asset.fittedSurface.volatility(_, timeToExpiry))
      global.Plotly.animate(plot.ref, literal(data = js.Array(
        literal(x = calls.map(_.logMoneyness), y = calls.map(_.volatility), text = calls.map(_.symbol),
          error_y = literal(array = calls.map(_.spread))),
        literal(x = puts.map(_.logMoneyness), y = puts.map(_.volatility), text = puts.map(_.symbol),
          error_y = literal(array = puts.map(_.spread))),
        literal(x = range, y = fitted))))
    }))
  }
}
