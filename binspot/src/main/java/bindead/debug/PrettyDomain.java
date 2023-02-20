package bindead.debug;

import javalx.xml.XmlPrintable;
import rreil.lang.MemVar;
import bindead.data.NumVar;

/**
 * Methods to be able to display the information contained in domains for various clients and in various flavors.
 */
public interface PrettyDomain extends XmlPrintable {

  /**
   * Print the domain to a more advanced string builder that has filtering capabilities.
   */
  public void toString (DomainStringBuilder builder);

  /**
   * Add representation of variable to the current line. The domain decides how to print the variable.
   * This is useful to inline the values stored in child domains in the output of parent domains.
   */
  public void varToCompactString (StringBuilder builder, NumVar var);
  /**
   * Add representation of variable to the current line. The domain decides how to print the variable.
   * This is useful to inline the values stored in child domains in the output of parent domains.
   */
  public void memVarToCompactString (StringBuilder builder, MemVar var);

  /**
   * Print the domain with the remaining information that was not inlined already by parent domains through
   * calls to {@link #varToCompactString(StringBuilder, NumVar)}.
   */
  public void toCompactString (StringBuilder builder);
}
