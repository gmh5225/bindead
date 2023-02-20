package satDomain;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

public class HashedSubstitution extends Substitution {
  final private HashMap<Integer, Integer> s;

  /**
   * the empty substitution (identity function on integers)
   */
  public HashedSubstitution () {
    s = new HashMap<Integer, Integer>();
  }

  Set<Entry<Integer, Integer>> entries () {
    return s.entrySet();
  }

  @Override public int get (int from) {
    final Integer to = s.get(from);
    return to == null ? from : to;
  }

  public boolean isIdentity () {
    return s.isEmpty();
  }

  @Override public void put (int from, int to) {
    assert from != 0;
    assert to != 0;
    assert !s.containsKey(from);
    if (from != to)
      s.put(from, to);
  }

  @Override public boolean substitutes (int from) {
    return s.containsKey(from);
  }

  @Override public String toString () {
    return s.toString();
  }
}