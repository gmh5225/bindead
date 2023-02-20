package bindead.domains.intervals;

import bindead.data.properties.DebugProperties;

public class IntervalProperties extends DebugProperties {
  public static final IntervalProperties INSTANCE = new IntervalProperties(); 
  private IntervalProperties () {
    super(Intervals.NAME);
  }
}