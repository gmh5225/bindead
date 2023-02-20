package bindead.domains.congruences;

import bindead.data.properties.DebugProperties;

public class CongruenceProperties extends DebugProperties {
  public static final CongruenceProperties INSTANCE = new CongruenceProperties();
  private CongruenceProperties () {
    super(Congruences.NAME);
  }
}
