package rreil.lang.util;

/**
 * Utility to generate numbers to be used as IDs for a class of variables.
 */
public final class StampGen {
  private int state;

  public StampGen () {
    this.state = 0;
  }

  public int next () {
    state = state + 1;
    if (state == -1)
      throw new IllegalStateException("Stamp generator overflow occured.");
    return state;
  }
}
