package bindead.domains.segments.heap;

import javalx.exceptions.UnimplementedException;
import javalx.persistentcollections.AVLSet;
import rreil.lang.MemVar;

public class ConnectorData {
  final public MemVar src;
  final public MemVar tgt;

  public ConnectorData (MemVar s, MemVar t) {
    src = s;
    tgt = t;
  }

  public AVLSet<MemVar> getChildSupportSet () {
    return AVLSet.singleton(src).add(tgt);
  }

  @Override public boolean equals (Object o) {
    if (o == null)
      return false;
    if (!(o instanceof ConnectorData))
      return false;
    ConnectorData other = (ConnectorData) o;
    return src.equals(other.src) && tgt.equals(other.tgt);
  }

  @Override @Deprecated public int hashCode () {
    throw new UnimplementedException();
  }

  @Override public String toString () {
    return "<" + src + " ;" + tgt + ">";
  }
}