package satDomain;

import java.util.HashSet;
import java.util.Set;

abstract class DomainComponent<T extends DomainComponent<T>> {
  final static void showBlanks (StringBuilder sb, int indent) {
    for (int i = 0; i < indent; i++)
      sb.append(' ');
  }

  /** all flags contained in this object */
  final Set<Flag> allFlags () {
    final HashSet<Flag> flags = new HashSet<Flag>();
    allFlags(flags);
    return flags;
  }

  /** collect all flags contained in this object */
  abstract void allFlags (Set<Flag> flags);

  /**
   * Make this and other compatible so that they can be compared or joined by
   * comparing or joining their numeric sub-domains
   */
  abstract void makeCompatible (T other, Renamer rthis, Renamer rother);

  /**
   * Make this and other compatible so that they can be compared or joined by
   * comparing or joining their numeric sub-domains
   */
  abstract void makeCompressed (Renamer rthis);

  /**
   * rename all flags
   */
  abstract void renameFlags (Substitution subst);
}