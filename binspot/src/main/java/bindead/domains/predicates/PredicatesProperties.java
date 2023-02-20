package bindead.domains.predicates;

import bindead.data.properties.DebugProperties;
import bindead.domains.predicates.finite.Predicates;

public class PredicatesProperties extends DebugProperties {
  public static final PredicatesProperties INSTANCE = new PredicatesProperties();
  private PredicatesProperties () {
    super(Predicates.NAME);
  }
}
