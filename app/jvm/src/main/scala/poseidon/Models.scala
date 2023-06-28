package poseidon

import java.time._

object Models {
  sealed trait OptionSide
  object OptionSide {
    case object CALL extends OptionSide
    case object PUT extends OptionSide
  }

  case class MarketInfo(
      underlyingInfo: List[UnderlyingAsset],
      optionInfo: List[OptionAsset],
      currentTime: LocalDateTime)

  case class UnderlyingAsset(
      symbol: String,
      baseAsset: String,
      quoteAsset: String,
      spot: Double,
      interestRate: Double,
      currentTime: LocalDateTime) {
    var options: List[OptionAsset] = null
  }

  case class OptionAsset(
      symbol: String,
      underlying: String,
      term: LocalDate,
      strike: Double,
      side: OptionSide,
      volatility: Double,
      spread: Double) {
    var asset: UnderlyingAsset = null

    lazy val timeToExpiry: Double = {
      val termEpochSecond = term.atTime(8, 0).toEpochSecond(ZoneOffset.UTC).toDouble
      val currentEpochSecond = asset.currentTime.toEpochSecond(ZoneOffset.UTC).toDouble
      (termEpochSecond - currentEpochSecond) / (365 * 24 * 60 * 60)
    }

    lazy val logMoneyness: Double = {
      Math.log(strike) - Math.log(asset.spot) - asset.interestRate * timeToExpiry
    }

    lazy val isValid: Boolean = volatility > 0.01 && volatility < 10 - 0.01
  }
}
