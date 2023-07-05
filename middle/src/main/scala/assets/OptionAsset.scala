package assets

case class OptionAsset(
    symbol: String,
    underlying: String,
    termTimestamp: Long,
    strike: Double,
    side: String,
    volatility: Double,
    spread: Double,
    timeToExpiry: Double,
    logMoneyness: Double) {

  def withTimeToExpiry(asset: UnderlyingAsset): OptionAsset = copy(
    timeToExpiry = (termTimestamp - asset.currentTimestamp) / (365 * 24 * 60 * 60 * 1000D))

  def withLogMoneyness(asset: UnderlyingAsset): OptionAsset = copy(
    logMoneyness = math.log(strike) - math.log(asset.spot))

  def isValid: Boolean = volatility > 0.01 && volatility < 10 - 0.01
}
