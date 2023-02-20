package bindead.domainnetwork.interfaces;

import javalx.xml.XmlPrintable;
import bindead.data.NumVar;
import bindead.debug.PrettyDomain;
import bindead.debug.StringHelpers;

public abstract class FunctorState implements XmlPrintable {

  /**
   * Print the domain with the remaining information that was not inlined already by parent domains through
   * calls to {@link #varToCompactString(StringBuilder, NumVar)}.
   *
   * Fallback method for domains that do not support the new compact prettyprinting style
   *
   * @see #PrettyDomain
   */
  public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    builder.append(StringHelpers.indentMultiline(domainName + ": ", toString()) + "\n");
  }
}
