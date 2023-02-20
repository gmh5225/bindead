package rreil.lang;

import rreil.lang.Rhs.Cmp;

/**
 * Tests on the RREIL language level.
 *
 * @author Bogdan Mihaila
 */
public class Test {
  private final Cmp opnd;

  public Test (Cmp opnd) {
    this.opnd = opnd;
  }

  public Cmp getComparison () {
    return opnd;
  }

  /**
   * Negate the test.
   */
  public Test not () {
    return new Test (opnd.not());
  }

  @Override public String toString () {
    return opnd.toString();
  }

}
