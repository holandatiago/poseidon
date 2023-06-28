package poseidon

import poseidon.Models.UnderlyingAsset
import poseidon.Optimizer._

case class Surface(sigma: Double, rho: Double, eta: Double) {
  def volatility(k: Double, t: Double): Double = {
    val theta = sigma * sigma * t
    val phi = eta / Math.sqrt(theta)
    val sqrt = Math.sqrt(1 + phi * k * (2 * rho + phi * k))
    val w = theta / 2 * (1 + phi * k * rho + sqrt)
    Math.sqrt(w / t)
  }

  def rootMeanSquareError(asset: UnderlyingAsset): Double = {
    val errors = asset.options
      .map(option => (option.volatility - volatility(option.logMoneyness, option.timeToExpiry)) / option.spread)
    Math.sqrt(errors.map(error => error * error).sum / asset.options.size)
  }
}

object Surface {
  def apply(params: Array[Double]): Surface = {
    val Array(logSigma, logitRho, logitEta) = params
    Surface(Math.exp(logSigma) / 2, 2 / (Math.exp(-logitRho) + 1) - 1, 2 / (Math.exp(-logitEta) + 1))
  }

  def calibrate(asset: UnderlyingAsset): Surface = {
    Surface(Array.fill(3)(0D).minimizeBy(Surface(_).rootMeanSquareError(asset)))
  }
}
