package bindead.data;

import java.util.Map;
import java.util.TreeMap;

import rreil.lang.MemVar;

/**
 * A class representing a variable in the analyzer for which a numeric value is tracked.
 *
 * @author Axel Simon
 */
public class NumVar implements Comparable<NumVar> {
  private static StampGen stamps = new StampGen(0);
  private static Map<NumVar, String> regionNames = new TreeMap<>();
  private static Map<String, NumVar> singletonsReverse = new TreeMap<>();
  private static boolean printMemVarOnly = false;
  private static boolean printNumVarOnly = false;

  private final int stamp;

  /**
   * Used in {@link Linear} to instantiate special variable for constants.
   * Do not use otherwise!
   */
  NumVar (int id) {
    assert stamps.peek() > id; // instantiating normal NumVars cannot clash with this special one.
    stamp = id;
  }

  private NumVar () {
    stamp = stamps.next();
  }

  private NumVar (String name) {
    this();
    if (name != null && !name.isEmpty())
      regionNames.put(this, name);
  }

  /**
   * Set if only the memory region prefix for the variable should be printed. Most memory regions (e.g. registers)
   * contain only one numeric variable thus printing only the region is enough to disambiguate them.
   * By default this is disabled and prints both the memory region prefix and the numerical variable.
   *
   * @see #printNumVarOnly(boolean)
   */
  public static void printMemVarOnly (boolean enabled) {
    printMemVarOnly = enabled;
  }

  /**
   * Set if only the numerical variable should be printed without the memory region prefix. By default this is disabled
   * and prints both the memory region prefix and the numerical variable.
   *
   * @see #printMemVarOnly(boolean)
   */
  public static void printNumVarOnly (boolean enabled) {
    printNumVarOnly = enabled;
  }

  /**
   * Return a prefix that describes the type of this variable.
   */
  protected String typePrefix () {
    return "v";
  }

  public static class AddrVar extends NumVar {
    protected AddrVar () {
    }

    private AddrVar (String name) {
      super(name);
    }

    @Override protected String typePrefix () {
      return "a";
    }

    @Override final public boolean isAddress () {
      return true;
    }
  }

  public static class FlagVar extends NumVar {
    protected FlagVar () {
    }

    private FlagVar (String name) {
      super(name);
    }

    @Override protected String typePrefix () {
      return "f";
    }

    @Override public boolean isFlag () {
      return true;
    }

  }

  /**
   * Lookup a variable that was earlier introduced with the same name.
   * Creates a new variable if no variable with this name exists.<br>
   * Use with caution as the variable could be used by another domain
   * if the names clash. The singleton variables are thus global variables.
   *
   * @return the variable with the passed-in name or a new variable.
   */
  public static NumVar getSingleton (String name) {
    NumVar variable = singletonsReverse.get(name);
    if (variable != null) {
      return variable;
    } else {
      variable = fresh(name);
      singletonsReverse.put(name, variable);
      return variable;
    }
  }

  /**
   * Lookup an address variable that was earlier introduced with the same name.
   * Creates a new address variable if no variable with this name exists.
   * Use with caution as the variable could be used by another domain
   * if the names clash. The singleton variables are thus global variables.
   *
   * @return the variable with the passed-in name or a new variable.
   */
  public static AddrVar getSingletonAddress (String name) {
    NumVar variable = singletonsReverse.get(name);
    if (variable != null) {
      assert variable instanceof AddrVar;
      return (AddrVar) variable;
    } else {
      variable = freshAddress(name);
      singletonsReverse.put(name, variable);
      return (AddrVar) variable;
    }
  }

  /**
   * Request a new numeric variable.
   *
   * @return the new variable
   */
  public static NumVar fresh () {
    return new NumVar();
  }

  /**
   * Request a new numeric variable and give it a name that is used for printing it.
   * Note that the name is only meant to help with debug printing and not to identify
   * variables by their name as there can exist more variables with the same name.
   *
   * @return the new variable
   */
  public static NumVar fresh (String name) {
    return new NumVar(name);
  }

  /**
   * Request a new numeric variable that is used as an address.
   *
   * @return the new variable
   */
  public static AddrVar freshAddress () {
    return new AddrVar();
  }

  /**
   * Request a new numeric variable that is used as an address.
   *
   * @return the new variable
   */
  public static AddrVar freshAddress (String name) {
    return new AddrVar(name);
  }

  public static FlagVar freshFlag () {
    return new FlagVar();
  }

  /**
   * Request a new numeric variable that is used as a flag (zero/one variable).
   *
   * @return the new variable
   */
  public static FlagVar freshFlag (String name) {
    return new FlagVar(name);
  }

  /**
   * Return the name of this variable prefixed by the memory region it belongs to, if any.
   * Note that this method always returns the mentioned format whereas the {@link #toString()} can be tweaked
   * by using {@link #printMemVarOnly(boolean)} and {@link #printNumVarOnly(boolean)}.
   */
  public String getName () {
    String region = getRegionName();
    String numId = typePrefix() + getStamp();
    if (region != null)
      return region + "(" + numId + ")";
    else
      return numId;
  }

  /**
   * The name registered for the region this variable belongs to or {@code null} if it is not associated with any region.
   */
  public String getRegionName () {
    return regionNames.get(this);
  }

  /**
   * Register {@code region} as name-prefix for this variable.
   *
   * @param region the memory region variable
   * @param offset the offset in bits
   */
  public void setRegionName (MemVar region, int offset) {
    String regionName = region.getName();
    String offsetUnit = "bits"; // the offset is printed in bits
    if (offset != 0 && offset % 8 == 0) {
      offsetUnit = ""; // the offset is printed in bytes
      offset = offset / 8;
    }
    if (offset != 0) {
      regionName = "[" + regionName + (offset > 0 ? "+" : "") + offset + offsetUnit + "]";
    }
    regionNames.put(this, regionName);
  }

  public boolean isAddress () {
    return false;
  }

  public boolean isFlag () {
    return false;
  }

  /**
   * Request the internal stamp of this variable.
   */
  public int getStamp () {
    return stamp;
  }

  @Override public int compareTo (NumVar other) {
    return Integer.signum(stamp - other.stamp);
  }

  @Deprecated @Override public boolean equals (Object other) {
    if (other instanceof NumVar)
      return equalTo((NumVar) other);
    else
      return false;
  }

  public boolean equalTo (NumVar other) {
    return stamp == other.stamp;
  }

  @Override public int hashCode () {
    return stamp;
  }

  /**
   * Print the name and memory region of this variable. Can be tweaked by using {@link #printMemVarOnly(boolean)} and
   * {@link #printNumVarOnly(boolean)}.
   */
  @Override public String toString () {
    String region = getRegionName();
    String numId = typePrefix() + getStamp();
    if (printNumVarOnly)
      return numId;
    if (region != null) {
      if (printMemVarOnly)
        return region;
      else
        return region + "(" + numId + ")";
    }
    return numId;
  }

  /**
   * Reset all the id counters and mappings for variables.
   * Use with care!
   */
  public static void reset () {
    stamps = new StampGen();
    regionNames = new TreeMap<>();
    singletonsReverse = new TreeMap<>();
  }

  /**
   * Utility to generate numbers to be used as IDs for a class of variables.
   */
  private static final class StampGen {
    private int state;

    StampGen (int start) {
      state = start;
    }

    public StampGen () {
      this(0);
    }

    public int peek () {
      return state;
    }

    public int next () {
      state = state + 1;
      if (state == -1)
        throw new IllegalStateException("Stamp generator overflow occured.");
      return state;
    }
  }

}
