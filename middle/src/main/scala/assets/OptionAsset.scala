package assets

case class OptionAsset(
    symbol: String = null,
    underlying: String = null,
    termTimestamp: Long = Long.MinValue,
    strike: Double = Double.NaN,
    side: String = null,
    volatility: Double = Double.NaN,
    spread: Double = Double.NaN,
    timeToExpiry: Double = Double.NaN,
    logMoneyness: Double = Double.NaN) {

  def withAssetPrice(asset: UnderlyingAsset): OptionAsset = copy(
    timeToExpiry = (termTimestamp - asset.currentTimestamp) / (365 * 24 * 60 * 60 * 1000D),
    logMoneyness = math.log(strike) - math.log(asset.spot))

  def isValid: Boolean = volatility > 0.01 && volatility < 10 - 0.01
}
