package object models {
  case class UnderlyingAsset(
      symbol: String = null,
      currencyPair: (String, String) = null,
      currentTimestamp: Long = Long.MinValue,
      spot: Double = Double.NaN,
      options: List[OptionContract] = null,
      bestSurface: VolatilitySurface = null) {

    def withOptionList(optionList: List[OptionContract]): UnderlyingAsset = copy(
      options = optionList.filter(_.isValid).sortBy(_.symbol).map(_.withAssetPrice(this)))

    def isValid: Boolean = options.nonEmpty
  }

  case class OptionContract(
      symbol: String = null,
      underlying: String = null,
      termTimestamp: Long = Long.MinValue,
      strike: Double = Double.NaN,
      side: String = null,
      volatility: Double = Double.NaN,
      spread: Double = Double.NaN,
      timeToExpiry: Double = Double.NaN,
      logMoneyness: Double = Double.NaN) {

    def withAssetPrice(asset: UnderlyingAsset): OptionContract = copy(
      timeToExpiry = (termTimestamp - asset.currentTimestamp) / (365 * 24 * 60 * 60 * 1000D),
      logMoneyness = math.log(strike) - math.log(asset.spot))

    def isValid: Boolean = volatility > 0.01 && volatility < 10 - 0.01
  }
}
