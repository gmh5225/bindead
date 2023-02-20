package satDomain;

import java.util.Arrays;

class ListSubstitution extends Substitution {
  final private int[] substs;

  ListSubstitution (int[] s) {
    substs = s;
  }

  @Override public int get (int from) {
    assert from >= 0 && from < substs.length;
    return substs[from];
  }

  @Override public void put (int from, int to) {
    assert from >= 0 && from < substs.length;
    substs[from] = to;
  }

  @Override public boolean substitutes (int from) {
    assert from >= 0 && from < substs.length;
    return substs[from] != from;
  }

  @Override public String toString () {
    return "Substitution" + Arrays.toString(substs);
  }
}