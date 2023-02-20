package bindead.domains.pointsto;

import bindead.data.properties.DebugProperties;


public class PointsToProperties extends DebugProperties {
  public static final PointsToProperties INSTANCE = new PointsToProperties();
  private PointsToProperties () {
    super(PointsTo.NAME);
  }
}
