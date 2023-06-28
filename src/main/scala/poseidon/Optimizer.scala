package poseidon

import breeze.linalg._
import breeze.optimize._
import ch.qos.logback.classic.{Level, LoggerContext}
import org.slf4j.{Logger, LoggerFactory}

object Optimizer {
  LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext].getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF)

  implicit def arrayToOptimizer(s: Array[Double]): Optimizer = Optimizer(s)
}

case class Optimizer(start: Array[Double]) {
  def minimizeBy(objectiveFunction: Array[Double] => Double): Array[Double] = {
    val function = new ApproximateGradientFunction((x: DenseVector[Double]) => objectiveFunction(x.toArray))
    new LBFGS[DenseVector[Double]].minimize(function, DenseVector(start)).toArray
  }
}
