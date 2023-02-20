package bindead.domains.widening.thresholds;

import bindead.data.properties.DebugProperties;
import bindead.domains.widening.oldthresholds.ThresholdsWidening;

public class ThresholdsWideningProperties extends DebugProperties {
  public static final ThresholdsWideningProperties INSTANCE = new ThresholdsWideningProperties();

  private ThresholdsWideningProperties () {
    super(ThresholdsWidening.NAME);
  }
}
