package assets

case class UnderlyingAsset(
    symbol: String = null,
    currencyPair: (String, String) = null,
    currentTimestamp: Long = Long.MinValue,
    spot: Double = Double.NaN,
    options: List[OptionAsset] = null,
    bestSurface: models.Surface = null)
