package bindead.domainnetwork.combinators;

import bindead.debug.PrettyDomain;
import bindead.domainnetwork.interfaces.FunctorState;

import com.jamesmurty.utils.XMLBuilder;

/**
 * A placeholder state for domains that do not use an own state but operate only on the child-state and want to extend
 * a domain functor.
 */
public class VoidDomainState extends FunctorState {
  private static final VoidDomainState EMPTY = new VoidDomainState();

  private VoidDomainState () {
  }

  public static VoidDomainState empty () {
    return EMPTY;
  }

  @Override public XMLBuilder toXML (XMLBuilder builder) {
    return builder;
  }

  @Override public String toString () {
    return "";
  }

  @Override public void toCompactString (String domainName, StringBuilder builder, PrettyDomain childDomain) {
    // nop
  }
}
