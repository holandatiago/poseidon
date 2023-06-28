package poseidon

import org.knowm.xchart.XYSeries.XYSeriesRenderStyle
import org.knowm.xchart._
import org.knowm.xchart.style.Styler.ChartTheme
import org.knowm.xchart.style.markers.SeriesMarkers

import scala.jdk.CollectionConverters._

object Plotter {
  implicit def iterableToPlotter[T](it: Iterable[T]): Plotter[T] = Plotter[T](it.toList)
}

case class Plotter[T](
    data: List[T],
    xMapper: T => Double = null,
    yMapper: T => Double = null,
    zGrouper: T => Any = (_: T) => "",
    wSplitter: T => Double = (_: T) => 0D,
    vDeviator: T => Double = null,
    uLines: List[(String, (Double, Double) => Double)] = Nil,
    xLimits: (Double, Double) = null,
    yLimits: (Double, Double) = null) {
  def plot(xMapper: T => Double, yMapper: T => Double): Plotter[T] = copy(xMapper = xMapper, yMapper = yMapper)
  def groupBy(zGrouper: T => Any): Plotter[T] = copy(zGrouper = zGrouper)
  def splitBy(wSplitter: T => Double): Plotter[T] = copy(wSplitter = wSplitter)
  def deviateBy(vDeviator: T => Double): Plotter[T] = copy(vDeviator = vDeviator)
  def addCurve(uName: String, uFunc: (Double, Double) => Double): Plotter[T] = copy(uLines = (uName, uFunc) :: uLines)
  def withinLimits(xLim: (Double, Double), yLim: (Double, Double)): Plotter[T] = copy(xLimits = xLim, yLimits = yLim)

  def display(title: String = ""): Unit = if (data.nonEmpty) {
    val (minXValue, maxXValue) = if (xLimits == null) (data.map(xMapper).min, data.map(xMapper).max) else xLimits
    val (minYValue, maxYValue) = if (yLimits == null) (data.map(yMapper).min, data.map(yMapper).max) else yLimits
    val step = (maxXValue - minXValue) / 80
    val range = Range.BigDecimal.inclusive(minXValue - 4 * step, maxXValue + 4 * step, step).toList.map(_.toDouble)

    val charts = data
      .groupBy(wSplitter)
      .toList.sortBy(_._1)
      .map { case (part, wValues) =>
        val chart = new XYChart(800, 600, ChartTheme.GGPlot2)
        chart.setTitle(part.toString)
        chart.getStyler.setCursorEnabled(true)
        chart.getStyler
          .setXAxisMin(minXValue).setXAxisMax(maxXValue)
          .setYAxisMin(minYValue).setYAxisMax(maxYValue)
        uLines.foreach { case (uName, uFunc) =>
          chart
            .addSeries(uName, range.asJava, range.map(uFunc(_, part)).map(Double.box).asJava)
            .setXYSeriesRenderStyle(XYSeriesRenderStyle.Line).setMarker(SeriesMarkers.NONE)
        }
        wValues
          .groupBy(zGrouper)
          .toList.sortBy(_._1.toString)
          .foreach { case (group, zValues) =>
            chart
              .addSeries(group.toString,
                zValues.map(xMapper).asJava,
                zValues.map(yMapper).map(Double.box).asJava,
                zValues.map(vDeviator).map(Double.box).asJava)
              .setXYSeriesRenderStyle(XYSeriesRenderStyle.Scatter).setMarker(SeriesMarkers.CIRCLE)
          }
        chart
      }
    new SwingWrapper(charts.asJava).setTitle(title).displayChartMatrix()
  } else println(s"data for plot $title is empty")
}
