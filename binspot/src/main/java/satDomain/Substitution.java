package satDomain;

public abstract class Substitution {
  /**
   * get the substitution for value from
   * */
  public abstract int get (int from);

  final public int newValue (int from) {
    return from < 0 ? -get(-from) : get(from);
  }

  /**
   * add a new substitution
   * */
  abstract public void put (int from, int to);

  abstract public boolean substitutes (int from);
}