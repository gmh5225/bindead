package bindead.domains.gauge;

import bindead.data.properties.DebugProperties;

public class GaugeProperties extends DebugProperties {
  public static final GaugeProperties INSTANCE = new GaugeProperties();

  private GaugeProperties () {
    super(Gauge.NAME);
  }

}
