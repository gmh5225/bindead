package bindead.domains.syntacticstripes;

import bindead.data.properties.DebugProperties;

public class StripeProperties extends DebugProperties {
  public static final StripeProperties INSTANCE = new StripeProperties(); 
  private StripeProperties () {
    super(Stripes.NAME);
  }
}
