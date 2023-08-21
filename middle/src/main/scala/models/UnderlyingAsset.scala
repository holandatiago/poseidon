package models

case class UnderlyingAsset(
    symbol: String = null,
    currencyPair: (String, String) = null,
    currentTimestamp: Long = Long.MinValue,
    spot: Double = Double.NaN,
    options: List[OptionContract] = null,
    fittedSurface: VolatilitySurface = null) {

  def withOptionList(optionList: List[OptionContract]): UnderlyingAsset = copy(
    options = optionList.map(_.withAssetPrice(this)).filter(_.isValid).sortBy(_.symbol))

  def isValid: Boolean = options.nonEmpty
}
