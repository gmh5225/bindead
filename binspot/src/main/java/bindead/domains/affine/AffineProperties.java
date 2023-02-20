package bindead.domains.affine;

import bindead.data.properties.DebugProperties;

public class AffineProperties extends DebugProperties {
  public static final AffineProperties INSTANCE = new AffineProperties(); 
  private AffineProperties () {
    super(Affine.NAME);
  }
}
