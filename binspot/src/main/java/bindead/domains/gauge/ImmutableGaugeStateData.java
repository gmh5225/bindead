package bindead.domains.gauge;

public interface ImmutableGaugeStateData extends GaugeStateDataGetters {

  public MutableGaugeStateData getMutableCopy ();

}