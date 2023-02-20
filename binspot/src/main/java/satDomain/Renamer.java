package satDomain;

import java.util.ArrayList;
import java.util.List;

public class Renamer {
  final List<Integer> renamings;
  private int current = 0;

  // int lastToKeep;
  public Renamer () {
    renamings = new ArrayList<Integer>();
  }

  public int add (int previous) {
    assert previous > 0;
    while (renamings.size() < previous + 1)
      renamings.add(0);
    assert renamings.get(previous) == 0;
    renamings.set(previous, ++current);
    return current;
  }

  public Integer get (int i) {
    return i < renamings.size() ? renamings.get(i) : 0;
  }

  public int getResultingLastVar () {
    return current;
  }

  @Override public String toString () {
    return "Renamer(" + current + ")" + renamings.toString();
  }
}
