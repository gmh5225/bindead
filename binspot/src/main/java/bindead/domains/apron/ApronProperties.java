package bindead.domains.apron;

import bindead.data.properties.DebugProperties;

public class ApronProperties extends DebugProperties {
  public static final ApronProperties INSTANCE = new ApronProperties();

  private ApronProperties () {
    super(Apron.NAME);
  }

}
