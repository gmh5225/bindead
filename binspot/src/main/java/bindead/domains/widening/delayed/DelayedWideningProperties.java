package bindead.domains.widening.delayed;

import bindead.data.properties.BoolProperty;
import bindead.data.properties.DebugProperties;

public class DelayedWideningProperties extends DebugProperties {
  public final BoolProperty semanticConstants;
  public static final DelayedWideningProperties INSTANCE = new DelayedWideningProperties();

  private DelayedWideningProperties () {
    super(DelayedWidening.NAME);
    semanticConstants = makeOptionProperty(DelayedWidening.NAME, "semanticConstants");
    semanticConstants.setValue(true);
  }
}