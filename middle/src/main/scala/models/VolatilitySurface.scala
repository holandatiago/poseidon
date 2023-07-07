package models

case class VolatilitySurface(params: List[Double]) {
  val List(sigma, rho, eta) = params

  def volatility(k: Double, t: Double): Double = {
    val theta = sigma * sigma * t
    val phi = eta / math.sqrt(theta)
    val sqrt = math.sqrt(1 + phi * k * (2 * rho + phi * k))
    val w = theta / 2 * (1 + phi * k * rho + sqrt)
    math.sqrt(w / t)
  }

  def rootMeanSquareError(options: List[OptionContract]): Double = {
    val errors = options.map(o => (o.volatility - volatility(o.logMoneyness, o.timeToExpiry)) / o.spread)
    math.sqrt(errors.map(error => error * error).sum / options.size)
  }
}

object VolatilitySurface {
  case class ParamDef(char: Char, min: Double, mean: Double, max: Double) {
    def bound(unbounded: Double): Double = (min, max) match {
      case (Double.MinValue, Double.MaxValue) => mean + unbounded
      case (Double.MinValue, _) => max - (max - mean) * math.exp(-unbounded)
      case (_, Double.MaxValue) => min + (mean - min) * math.exp(+unbounded)
      case _ => min + (max - min) / (1 + (max - mean) * math.exp(-unbounded) / (mean - min))
    }
  }

  val paramDefs: List[ParamDef] = List(
    ParamDef('σ', 0, 0.5, Double.MaxValue),
    ParamDef('ρ', -1, 0, 1),
    ParamDef('η', 0, 1, 2))

  def fromUnbounded(unboundedParams: Array[Double]): VolatilitySurface = {
    apply(paramDefs.zip(unboundedParams).map { case (paramDef, unbounded) => paramDef.bound(unbounded) })
  }

  trait Optimizer {
    def calibrate(options: List[OptionContract]): VolatilitySurface = {
      val objectiveFunction = fromUnbounded(_: Array[Double]).rootMeanSquareError(options)
      fromUnbounded(minimize(objectiveFunction, Array.fill(paramDefs.size)(0D)))
    }

    def minimize(objectiveFunction: Array[Double] => Double, startArray: Array[Double]): Array[Double]
  }
}
