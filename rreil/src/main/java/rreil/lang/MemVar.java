package rreil.lang;

import java.util.Map;
import java.util.TreeMap;

import rreil.lang.util.StampGen;

/**
 * A class representing a memory region in the analyzer for which fields are tracked.
 *
 * @author Axel Simon
 */
public class MemVar implements Comparable<MemVar>, Reconstructable {
  private static StampGen stamps = new StampGen();
  private static Map<MemVar, String> names = new TreeMap<>();
  private static Map<String, MemVar> reverse = new TreeMap<>();
  public final int stamp;

  private MemVar () {
    stamp = stamps.next();
  }

  /**
   * Request a new memory region variable.
   *
   * @return the new variable
   */
  public static MemVar fresh () {
    return new MemVar();
  }

  /**
   * Request a new memory variable and give it a name that is used for printing it.
   *
   * @return the new variable
   */
  public static MemVar fresh (String name) {
    MemVar var = new MemVar();
    var.setName(name);
    return var;
  }

  /**
   * Request a new memory variable and give it a name prefix to designate it as a temporary register.
   * @return
   */
  public static MemVar freshTemporary () {
    MemVar var = new MemVar();
    var.setName("_t" + var.stamp);
    return var;
  }

  /**
   * Lookup a variable that was earlier introduced with the same name.
   * Creates a new variable if no variable with this name exists.
   *
   * @return the variable with the passed-in name or a new variable.
   */
  public static MemVar getVarOrFresh (String name) {
    MemVar var = reverse.get(name);
    if (var == null) {
      var = new MemVar();
      var.setName(name);
    }
    return var;
  }

  /**
   * Lookup a variable that was earlier introduced with the same name.
   *
   * @return the variable with the passed-in name or {@code null} if the name is not known.
   */
  public static MemVar getVarOrNull (String name) {
    return reverse.get(name);
  }

  /**
   * Give the memory region variable a new name.
   */
  public void setName (String name) {
    if (name == null || name.isEmpty())
      return;
    names.put(this, name);
    reverse.put(name, this);
  }

  /**
   * Return the name of this variable.
   */
  public String getName () {
    String name = names.get(this);
    if (name == null) {
      name = "m" + stamp;
      setName(name);
    }
    return name;
  }

  @Override public int hashCode () {
    return stamp;
  }

  // One can use == for comparison, since every MemVar has a unique stamp.
  @Override public boolean equals (Object other) {
    return stamp == ((MemVar) other).stamp;
  }

  @Override public int compareTo (MemVar other) {
    return Integer.signum(stamp - other.stamp);
  }

  @Override public String toString () {
    return getName();
  }

  @Override public String reconstructCode () {
    return "MemVar.getVarOrFresh(\"" + names.get(this) + "\")";
  }

  /**
   * Reset all the id counters and mappings for variables.
   * Use with care!
   */
  public static void reset () {
    stamps = new StampGen();
    names = new TreeMap<MemVar, String>();
    reverse = new TreeMap<String, MemVar>();
  }
}
