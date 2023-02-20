package bindead.domains.widening.oldthresholds;

import bindead.abstractsyntax.zeno.Zeno.Test;
import bindead.domainnetwork.interfaces.ProgramPoint;

/**
 * A threshold for widening. It consists of a predicate and the origin the predicate is from in the program.
 */
public class Threshold implements Comparable<Threshold> {
  private final Origin origin;
  private final Test predicate;

  public Threshold (ProgramPoint point, Test test) {
    this.origin = new Origin(point, test);
    this.predicate = test;
  }

  private Threshold (Origin origin, Test transformedTest) {
    this.origin = origin;
    this.predicate = transformedTest;
  }

  /**
   * Generate a new threshold that has the same origin but a different predicate.
   * Useful for any kind of operation that needs to be applied to the predicate.
   */
  public Threshold withTest (Test newTest) {
    return new Threshold(origin, newTest);
  }

  public Origin getOrigin () {
    return origin;
  }

  public Test getTest () {
    return predicate;
  }

  @Override public String toString () {
    return getTest() + " @" + getOrigin();
  }

  @Override public int compareTo (Threshold other) {
    int cmp = this.origin.compareTo(other.origin);
    return cmp != 0 ? cmp : this.predicate.compareTo(other.predicate);
  }

  @Override public int hashCode () {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((origin == null) ? 0 : origin.hashCode());
    result = prime * result + ((predicate == null) ? 0 : predicate.hashCode());
    return result;
  }

  @Override public boolean equals (Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Threshold))
      return false;
    Threshold other = (Threshold) obj;
    return compareTo(other) == 0;
  }


  /**
   * The origin of a threshold is the program location and the predicate. These two are necessary to distinguish thresholds.
   */
  static class Origin implements Comparable<Origin> {
    private final ProgramPoint point;
    private final Test test;

    public Origin (ProgramPoint point, Test test) {
      this.point = point;
      this.test = test;
    }

    public ProgramPoint getLocation () {
      return point;
    }

    public Test getTest () {
      return test;
    }

    @Override public int compareTo (Origin other) {
      int cmp = this.point.compareTo(other.point);
      return cmp != 0 ? cmp : this.test.compareTo(other.test);
    }

    @Override public String toString () {
      return "\"" + getTest() + ", " + getLocation() + "\"";
    }

    @Override public int hashCode () {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((point == null) ? 0 : point.hashCode());
      result = prime * result + ((test == null) ? 0 : test.hashCode());
      return result;
    }

    @Override public boolean equals (Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Origin))
        return false;
      Origin other = (Origin) obj;
      return compareTo(other) == 0;
    }

  }
}
