package satDomain;

import java.util.Set;

/**
 * will contain edge flags
 *
 * this is just a value object holding one flag.
 *
 * @author hsi
 *
 */
public class Flag extends DomainComponent<Flag> {
  public int flag;

  public Flag (int f) {
    flag = f;
  }



  @Override public String toString () {
    // assert false;
    final StringBuilder sb = new StringBuilder();
    show(sb, null);
    return sb.toString();
  }

  public String toString (Numeric numeric) {
    final StringBuilder sb = new StringBuilder();
    show(sb, numeric);
    return sb.toString();
  }

  @Override void allFlags (Set<Flag> flags) {
    flags.add(this);
  }

  @Override void makeCompatible (Flag other, Renamer rthis, Renamer rother) {
    flag = rthis.add(flag);
    other.flag = rother.add(other.flag);
  }

  @Override void makeCompressed (Renamer rthis) {
    flag = rthis.add(flag);
  }

  @Override void renameFlags (Substitution subst) {
    flag = subst.get(flag);
  }

  Flag split (Substitution subst) {
    assert subst.substitutes(flag);
    return new Flag(subst.get(flag));
  }

  void substWith (Flag keep, Substitution subst) {
    subst.put(flag, keep.flag);
  }

  private void show (StringBuilder sb, Numeric numeric) {
    assert numeric != null;
    if (numeric == null)
      sb.append("x#" + flag);
    else
      sb.append(numeric.showFlag(this));
  }

  @Override public int hashCode () {
    return flag;
  }
}
