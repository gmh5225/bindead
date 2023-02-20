package bindead.analyses.warnings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import bindead.domainnetwork.channels.WarningMessage;
import bindead.domainnetwork.channels.WarningsContainer;
import bindead.domainnetwork.interfaces.ProgramPoint;

/**
 * Extends the map interface to not return {@code null} for program points
 * without warnings but an empty warnings list.
 */
public class WarningsMap {
  private final Map<ProgramPoint, WarningsContainer> map;
  // we assume that the state only grows, thus a warning if once was raised will not disappear again.
  // Thus it is enough to store the first time the warning occurred
  private final Map<ProgramPoint, Integer> iterationOfOccurrence;

  public WarningsMap () {
    map = new TreeMap<>();
    iterationOfOccurrence = new HashMap<>();
  }

  /**
   * Get the warnings for a program point.
   */
  public WarningsContainer get (ProgramPoint location) {
    WarningsContainer value = map.get(location);
    if (value == null)
      value = new WarningsContainer();
    return value;
  }

  /**
   * Get the iteration count at which warnings for this program point were emitted, if any.
   */
  public int getIterationOfWarnings (ProgramPoint location) {
    return iterationOfOccurrence.get(location);
  }

  /**
   * Associate new warnings with a program point at which the warnings were emitted.
   * Additionally associate the warnings with the iteration number at which they were emitted.
   */
  public void put (ProgramPoint location, int iteration, WarningsContainer warnings) {
    map.put(location, warnings);
    if (iterationOfOccurrence.get(location) == null) // only overwrite the first time we see the warning
      iterationOfOccurrence.put(location, iteration);
    else
      assert map.get(location).size() == warnings.size();
    // TODO: warnings could actually become more if the state grows. Need to get the added warnings and
    // associate them with the latest iteration count.
  }

  public Collection<WarningsContainer> values () {
    return map.values();
  }

  public Set<Entry<ProgramPoint, WarningsContainer>> entrySet () {
    return map.entrySet();
  }

  public int totalNumberOfWarnings () {
    int numberOfWarnings = 0;
    for (WarningsContainer entry : values()) {
      numberOfWarnings += entry.size();
    }
    return numberOfWarnings;
  }

  @Override public String toString () {
    StringBuilder builder = new StringBuilder();
    builder.append("Warnings: " + totalNumberOfWarnings());
    builder.append('\n');
    for (Entry<ProgramPoint, WarningsContainer> entry : entrySet()) {
      ProgramPoint point = entry.getKey();
      WarningsContainer warningsHere = entry.getValue();
      if (!warningsHere.isEmpty()) {
        builder.append("@ " + point);
        if (iterationOfOccurrence.containsKey(point))
          builder.append(" in iter. " + iterationOfOccurrence.get(point));
        builder.append('\n');
        for (WarningMessage message : warningsHere) {
          builder.append("  " + message.detailedMessage());
          builder.append('\n');
        }
      }
    }
    return builder.toString();
  }
}