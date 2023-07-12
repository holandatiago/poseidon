package controllers

import breeze.linalg.DenseVector
import breeze.optimize.{ApproximateGradientFunction, LBFGS}
import models.{UnderlyingAsset, VolatilitySurface}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}

object MarketService {
  implicit val executionContext: ExecutionContext = ExchangeClient.executionContext

  def fetchMarketPrices: List[UnderlyingAsset] = {
    val marketInfoFuture = ExchangeClient.fetchMarketDef
    val volsFuture = ExchangeClient.fetchOptionVols
    val spotsFuture = marketInfoFuture
      .flatMap(m => Future.sequence(m.assetDefs.map(a => ExchangeClient.fetchAssetPrice(a.symbol))))
    Await.result(for (marketInfo <- marketInfoFuture; vols <- volsFuture; spots <- spotsFuture) yield {
      val spotsMap = spots.map(a => a.symbol -> a.spot).toMap
      val volsMap = vols.groupBy(_.symbol).view.mapValues(_.head).toMap
      val options = marketInfo.optionDefs.map(option => (option, volsMap(option.symbol)))
        .map { case (option, optionVol) => option.copy(volatility = optionVol.volatility, spread = optionVol.spread) }
        .groupBy(_.underlying).withDefaultValue(Nil)
      val assets = marketInfo.assetDefs.sortBy(_.symbol)
        .map(asset => asset.copy(spot = spotsMap(asset.symbol), currentTimestamp = marketInfo.timestamp))
        .map(asset => asset.withOptionList(options(asset.symbol))).filter(_.isValid)
        .map(asset => asset.copy(fittedSurface = surfaceOptimizer.calibrate(asset.options)))
      assets.foreach(printAsset)
      assets
    }, Duration(30, SECONDS))
  }

  def printAsset(asset: UnderlyingAsset): Unit = {
    val rmse = asset.fittedSurface.rootMeanSquareError(asset.options)
    println(s"Calibrated ${asset.symbol}\t$rmse\t${asset.fittedSurface}")
  }

  val surfaceOptimizer: VolatilitySurface.Optimizer = (objectiveFunction, startArray) => {
    val function = new ApproximateGradientFunction((x: DenseVector[Double]) => objectiveFunction(x.toArray))
    new LBFGS[DenseVector[Double]].minimize(function, DenseVector(startArray)).toArray
  }
}
