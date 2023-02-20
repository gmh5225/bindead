package bindead.debug;

import bindead.data.properties.BoolProperty;

public class DomainPrintProperties {
  public static final String NAME = "DOMAINS";
  private static final String keyFmt = "analysis.%s.debug.%s";
  public static final DomainPrintProperties INSTANCE = new DomainPrintProperties();
  public final BoolProperty printCompact;

  private DomainPrintProperties () {
    printCompact = new BoolProperty(String.format(keyFmt, NAME.toLowerCase(), "compactPrinting"));
  }
}
