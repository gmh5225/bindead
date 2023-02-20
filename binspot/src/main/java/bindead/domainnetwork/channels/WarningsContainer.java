package bindead.domainnetwork.channels;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Mutable container to manage the warnings that are accumulated during the analysis.
 */
public class WarningsContainer implements Iterable<WarningMessage> {
  private final List<WarningMessage> warnings;

  public WarningsContainer () {
    this.warnings = new LinkedList<WarningMessage>();
  }

  public void addWarning (WarningMessage warning) {
    warnings.add(warning);
  }

  public void addWarnings (WarningsContainer newWarnings) {
    warnings.addAll(newWarnings.warnings);
  }

  @Override public WarningsContainer clone () {
    WarningsContainer result = new WarningsContainer();
    result.warnings.addAll(warnings);
    return result;
  }

  public boolean isEmpty () {
    return warnings.isEmpty();
  }

  public int size () {
    return warnings.size();
  }

  @Override public String toString () {
    return warnings.toString();
  }

  @Override public Iterator<WarningMessage> iterator () {
    return warnings.iterator();
  }

}
