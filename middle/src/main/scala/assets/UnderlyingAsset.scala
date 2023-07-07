package assets

case class UnderlyingAsset(
    symbol: String = null,
    currencyPair: (String, String) = null,
    currentTimestamp: Long = Long.MinValue,
    spot: Double = Double.NaN,
    options: List[OptionAsset] = null,
    bestSurface: models.Surface = null) {

  def withOptionList(optionList: List[OptionAsset]): UnderlyingAsset = copy(
    options = optionList.filter(_.isValid).sortBy(_.symbol).map(_.withAssetPrice(this)))

  def isValid: Boolean = options.nonEmpty
}
