package assets

case class UnderlyingAsset(
    symbol: String,
    currencyPair: (String, String),
    currentTimestamp: Long,
    spot: Double,
    options: List[OptionAsset],
    bestSurface: models.Surface)
